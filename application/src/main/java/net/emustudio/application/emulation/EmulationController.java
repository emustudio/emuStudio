/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.Memory;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Two host-owned threads: commands publish lifecycle state; the worker executes machine operations.
 */
public final class EmulationController implements Closeable {
    private final ScheduledThreadPoolExecutor commands = new ScheduledThreadPoolExecutor(1, task -> new Thread(task, "machine-commands"));
    private volatile Thread workerThread;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> new Thread(() -> {
        workerThread = Thread.currentThread();
        task.run();
    }, "machine-cpu"));
    private final CPU cpu;
    private final CPUContext cpuContext;
    private final Memory memory;
    private final List<Device> devices;
    private volatile DebuggerSession debugger;
    private final Compiler compiler;
    private final List<Consumer<CPU.RunState>> executionListeners = new CopyOnWriteArrayList<>();
    private volatile CPU.RunState state = CPU.RunState.STATE_STOPPED_BREAK;
    private volatile boolean timedRunning, closing, stepping;
    private volatile CompletableFuture<CPU.RunState> runCompletion = CompletableFuture.completedFuture(state);
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    private volatile Task active;
    private volatile Pending pending;
    // Remaining fields belong exclusively to the command thread.
    private ScheduledFuture<?> tick;
    private long timerGeneration;

    private enum Operation {
        CONTINUOUS, SINGLE_STEP, MUTATION, COMPILATION;

        boolean isMutation() {
            return this == MUTATION || this == COMPILATION;
        }
    }

    private static final class Task {
        final Operation operation;
        final CompletableFuture<CPU.RunState> result;
        private Thread runner;
        private boolean interrupted;

        Task(Operation operation, CompletableFuture<CPU.RunState> result) {
            this.operation = operation;
            this.result = result;
        }

        synchronized void requestStop() {
            if (operation.isMutation()) return;
            interrupted = true;
            if (runner != null) runner.interrupt();
        }

        synchronized void enter() {
            runner = Thread.currentThread();
            if (interrupted) runner.interrupt();
        }

        synchronized void exit() {
            runner = null;
        }
    }

    private static final class Pending {
        final CPU.RunState targetState;
        final boolean reset;
        final Integer location;
        final boolean executing;
        final CompletableFuture<CPU.RunState> result;

        Pending(CPU.RunState targetState, boolean reset, Integer location, CompletableFuture<CPU.RunState> result, boolean executing) {
            this.targetState = targetState;
            this.reset = reset;
            this.location = location;
            this.result = result;
            this.executing = executing;
        }
    }

    public EmulationController(CPU cpu, CPUContext cpuContext, Memory memory, List<Device> devices) {
        this(cpu, cpuContext, memory, devices, null);
    }

    public EmulationController(CPU cpu, CPUContext cpuContext, Memory memory, List<Device> devices, Compiler compiler) {
        this.cpu = cpu;
        this.cpuContext = cpu == null ? null : Objects.requireNonNull(cpuContext);
        this.memory = memory;
        this.devices = List.copyOf(devices);
        this.compiler = compiler;

        commands.setRemoveOnCancelPolicy(true);
        commands.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        commands.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    /** Attaches the machine's debugger once while execution is paused. */
    public void attachDebugger(DebuggerSession session) {
        Objects.requireNonNull(session);
        this.<Void>command(result -> {
            requirePaused();
            if (debugger != null) throw new IllegalStateException("Debugger already attached");
            if (session.cpu != cpu || session.execution() != this)
                throw new IllegalArgumentException("Debugger belongs to another machine");
            debugger = session;
            result.complete(null);
        }).join();
    }

    public CPU.RunState getRunState() {
        return state;
    }

    public void addExecutionListener(Consumer<CPU.RunState> listener) {
        executionListeners.add(Objects.requireNonNull(listener));
    }

    public void removeExecutionListener(Consumer<CPU.RunState> listener) {
        executionListeners.remove(listener);
    }

    public boolean isWorkerThread() {
        return Thread.currentThread() == workerThread;
    }

    /** Verifies that debugger resources can be released without racing machine work. */
    public void requireIdle() {
        if (active != null || pending != null || timedRunning || state == CPU.RunState.STATE_RUNNING)
            throw new IllegalStateException("Machine work must finish before closing debugger resources");
    }

    public int getInstructionLocation() {
        return cpuContext == null ? -1 : cpuContext.getInstructionLocation();
    }

    /**
     * Whether a step, machine edit, lifecycle command, or cooperative close is pending or executing.
     */
    public boolean isBusy() {
        Task task = active;
        return closing || pending != null || (task != null && task.operation != Operation.CONTINUOUS);
    }

    /**
     * Whether application-scheduled stepping is active, including the delay between instructions.
     */
    public boolean isTimedRunning() {
        return timedRunning;
    }

    /**
     * An individual instruction is executing; views can retain their paused controls.
     */
    public boolean isStepping() {
        return stepping;
    }

    public CPUContext cpuContext() {
        return cpuContext;
    }

    public CompletableFuture<CPU.RunState> runCompletion() {
        return runCompletion.copy();
    }

    private <T> CompletableFuture<T> command(Consumer<CompletableFuture<T>> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            commands.execute(() -> {
                try {
                    if (closing) throw new IllegalStateException("Machine is closing");
                    operation.accept(result);
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
        } catch (RejectedExecutionException failure) {
            result.completeExceptionally(failure);
        }
        return result.copy();
    }

    private void requirePaused() {
        if (isBusy()) throw new IllegalStateException("Machine command is pending");
        if (active != null || state == CPU.RunState.STATE_RUNNING) throw new IllegalStateException("Pause CPU first");
    }

    public CompletableFuture<CPU.RunState> start() {
        return command(result -> {
            requireCpu();
            requirePaused();
            if (state != CPU.RunState.STATE_STOPPED_BREAK) throw new IllegalStateException("Reset CPU first");
            cancelTimer();
            runCompletion = new CompletableFuture<>();
            launch(Operation.CONTINUOUS, runCompletion, cpu::execute);
            result.complete(CPU.RunState.STATE_RUNNING);
        });
    }

    public CompletableFuture<CPU.RunState> step() {
        return command(result -> {
            requirePaused();
            cancelTimer();
            launchStep(result, -1, 0);
        });
    }

    public CompletableFuture<CPU.RunState> step(long delay, TimeUnit unit) {
        long nanos = Math.max(1, unit.toNanos(delay));
        return command(result -> {
            requireTimedStepAvailable();
            cancelTimer();
            timedRunning = true;
            long generation = timerGeneration;
            if (active == null) scheduleStep(generation, nanos, 0);
            else active.result.whenComplete((ignored, failure) -> {
                if (isTimerCurrent(generation)) scheduleStep(generation, nanos, nanos);
            });
            publish(state);
            result.complete(state);
        });
    }

    private void requireTimedStepAvailable() {
        requireCpu();
        if (pending != null || (isBusy() && !timedRunning)) throw new IllegalStateException("Machine command is pending");
        if (active != null && active.operation == Operation.CONTINUOUS)
            throw new IllegalStateException("Pause CPU first");
        if (state != CPU.RunState.STATE_STOPPED_BREAK && !timedRunning)
            throw new IllegalStateException("Reset CPU first");
    }

    private boolean isTimerCurrent(long generation) {
        return generation == timerGeneration && timedRunning && !closing;
    }

    private void scheduleStep(long generation, long delay, long initialDelay) {
        tick = commands.schedule(() -> {
            if (!isTimerCurrent(generation)) return;
            if (state != CPU.RunState.STATE_STOPPED_BREAK || active != null) {
                cancelTimer();
                return;
            }
            launchStep(new CompletableFuture<>(), generation, delay);
        }, initialDelay, TimeUnit.NANOSECONDS);
    }

    private void launchStep(CompletableFuture<CPU.RunState> result, long generation, long delay) {
        requireCpu();
        if (state != CPU.RunState.STATE_STOPPED_BREAK) throw new IllegalStateException("Reset CPU first");
        launch(Operation.SINGLE_STEP, result, cpu::step);
        if (generation >= 0) result.whenComplete((value, failure) -> {
            if (isTimerCurrent(generation)) {
                if (failure == null && value == CPU.RunState.STATE_STOPPED_BREAK)
                    scheduleStep(generation, delay, delay);
                else cancelTimer();
            }
        });
    }

    private void cancelTimer() {
        boolean changed = timedRunning;
        timerGeneration++;
        timedRunning = false;
        if (tick != null) tick.cancel(false);
        tick = null;
        if (changed) publish(state);
    }

    private void requireCpu() {
        if (cpu == null) throw new IllegalStateException("No CPU configured");
    }

    public CompletableFuture<CPU.RunState> pause() {
        return stop(CPU.RunState.STATE_STOPPED_BREAK, false, null);
    }

    public CompletableFuture<CPU.RunState> stop() {
        return stop(CPU.RunState.STATE_STOPPED_NORMAL, false, null);
    }

    public CompletableFuture<CPU.RunState> reset() {
        return stop(CPU.RunState.STATE_STOPPED_BREAK, true, null);
    }

    public CompletableFuture<CPU.RunState> reset(int location) {
        return stop(CPU.RunState.STATE_STOPPED_BREAK, true, location);
    }

    private CompletableFuture<CPU.RunState> stop(CPU.RunState targetState, boolean reset, Integer location) {
        return command(result -> {
            if (pending != null || (active != null && active.operation.isMutation()))
                throw new IllegalStateException("Machine command is pending");
            cancelTimer();
            pending = new Pending(targetState, reset, location, result, active != null);
            publish(state);
            if (active != null) active.requestStop();
            else finishPending();
        });
    }

    private void finishPending() {
        Pending operation = pending;
        if (operation == null) {
            finishClose();
            return;
        }
        if (operation.reset) {
            launch(Operation.MUTATION, operation.result, () -> resetMachine(operation.location));
        } else {
            CPU.RunState nextState = state;
            if (!operation.executing && state == CPU.RunState.STATE_STOPPED_BREAK)
                nextState = operation.targetState;
            pending = null;
            publish(nextState);
            operation.result.complete(state);
            finishClose();
        }
    }

    private CPU.RunState resetMachine(Integer location) {
        if (compiler != null) compiler.reset();
        if (memory != null) memory.reset();
        for (Device device : devices) device.reset();
        resetCpu(location);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    public CompletableFuture<Void> mutate(Runnable mutation) {
        Objects.requireNonNull(mutation);
        return this.<CPU.RunState>command(result -> {
            requirePaused();
            CPU.RunState previous = state;
            launch(Operation.MUTATION, result, () -> {
                mutation.run();
                return previous;
            });
        }).thenApply(ignored -> null);
    }

    /**
     * Compiles on the worker without resetting memory, then resets only CPU execution position.
     */
    public CompletableFuture<CPU.RunState> compile(Runnable compilation) {
        return compile(compilation, null);
    }

    public CompletableFuture<CPU.RunState> compile(Runnable compilation, Integer location) {
        Objects.requireNonNull(compilation);
        return command(result -> {
            requirePaused();
            cancelTimer();
            launch(Operation.COMPILATION, result, () -> {
                compilation.run();
                resetCpu(location);
                return CPU.RunState.STATE_STOPPED_BREAK;
            });
        });
    }

    private void resetCpu(Integer location) {
        if (cpu == null) return;
        if (location == null) cpu.reset();
        else cpu.reset(location);
        if (debugger != null) debugger.cpuReset();
    }

    private void launch(Operation kind, CompletableFuture<CPU.RunState> result, Callable<CPU.RunState> operation) {
        CPU.RunState previousState = state;
        Task task = new Task(kind, result);
        active = task;
        stepping = kind == Operation.SINGLE_STEP;
        publish(kind.isMutation() ? state : CPU.RunState.STATE_RUNNING);
        worker.execute(() -> executeTask(task, operation, previousState));
    }

    private void executeTask(Task task, Callable<CPU.RunState> operation, CPU.RunState previousState) {
        task.enter();
        CPU.RunState returned;
        Throwable failure = null;
        try {
            returned = invokeOperation(task.operation, operation);
        } catch (Throwable problem) {
            failure = problem instanceof CompletionException && problem.getCause() != null ? problem.getCause() : problem;
            returned = task.operation.isMutation() ? previousState : failureState(failure);
        } finally {
            task.exit();
            Thread.interrupted();
        }
        CPU.RunState value = returned;
        Throwable error = failure;
        commands.execute(() -> finished(task, value, error));
    }

    private CPU.RunState invokeOperation(Operation kind, Callable<CPU.RunState> operation) {
        CPU.RunState[] returned = {CPU.RunState.STATE_STOPPED_BAD_INSTR};
        Runnable action = () -> {
            try {
                returned[0] = Objects.requireNonNull(operation.call());
            } catch (Exception problem) {
                throw new CompletionException(problem);
            }
        };
        if (debugger == null) action.run();
        else if (kind == Operation.COMPILATION) debugger.runCompilation(action);
        else if (kind.isMutation()) debugger.runMutation(action);
        else if (kind == Operation.SINGLE_STEP) debugger.runStep(action);
        else debugger.runExecution(action);
        return returned[0];
    }

    private CPU.RunState failureState(Throwable failure) {
        return failure instanceof IndexOutOfBoundsException || failure.getCause() instanceof IndexOutOfBoundsException ? CPU.RunState.STATE_STOPPED_ADDR_FALLOUT : CPU.RunState.STATE_STOPPED_BAD_INSTR;
    }

    private void finished(Task task, CPU.RunState returned, Throwable failure) {
        if (active != task) return;
        if (returned == CPU.RunState.STATE_RUNNING)
            returned = closing ? CPU.RunState.STATE_STOPPED_NORMAL
                    : pending != null ? pending.targetState : CPU.RunState.STATE_STOPPED_BREAK;
        active = null;
        stepping = false;
        if (pending != null && task.result == pending.result) {
            pending = null;
        }
        publish(returned, true);
        if (debugger != null) debugger.completeSourceCapture();
        if (failure == null) task.result.complete(state);
        else task.result.completeExceptionally(failure);
        if (pending != null) finishPending();
        else finishClose();
    }

    private void publish(CPU.RunState value) {
        publish(value, false);
    }

    private void publish(CPU.RunState value, boolean internalStateChanged) {
        CPU.RunState previous = state;
        state = value;
        if (!internalStateChanged && previous == value) return;
        if (debugger != null) debugger.executionChanged(previous, value);
        executionListeners.forEach(listener -> listener.accept(value));
    }

    public CompletableFuture<Void> closeAsync() {
        try {
            commands.execute(() -> {
                if (closing) return;
                closing = true;
                cancelTimer();
                publish(state);
                if (active != null) active.requestStop();
                else if (pending == null) finishClose();
            });
        } catch (RejectedExecutionException failure) {
            if (!closed.isDone()) closed.completeExceptionally(failure);
        }
        return closed.copy();
    }

    private void finishClose() {
        if (!closing || active != null || pending != null || worker.isShutdown()) return;
        worker.shutdown();
        commands.shutdown();
        closed.complete(null);
    }

    /**
     * Blocking lifecycle hook for non-UI callers. UI callers use closeAsync().
     */
    @Override
    public void close() {
        closeAsync().join();
    }
}
