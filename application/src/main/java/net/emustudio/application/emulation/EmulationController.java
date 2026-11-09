/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Two host-owned threads: commands publish lifecycle state; the worker executes machine operations. */
public final class EmulationController implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulationController.class);
    private final ScheduledThreadPoolExecutor commands = new ScheduledThreadPoolExecutor(1,
            task -> new Thread(task, "machine-commands"));
    public boolean isWorkerThread() { return Thread.currentThread() == workerThread; }
    private volatile Thread workerThread;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> new Thread(() -> {
        workerThread = Thread.currentThread();
        task.run();
    }, "machine-cpu"));
    private CPU.RunState publishedState;
    private final CPU cpu;
    private final CPUContext cpuContext;
    private final Memory memory;
    private final List<Device> devices;
    private final DebugSession debugger;
    private final Compiler compiler;
    private volatile CPU.RunState state = CPU.RunState.STATE_STOPPED_BREAK;
    private volatile boolean busy, timedRunning, closing;
    private volatile long mutationGeneration;
    private volatile CompletableFuture<CPU.RunState> runCompletion = CompletableFuture.completedFuture(state);
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    // Remaining fields belong exclusively to the command thread.
    private Task active;
    private Pending pending;
    private ScheduledFuture<?> tick;
    private long timerGeneration;
    private boolean shutDown;

    private enum Stop { PAUSE, STOP, RESET }

    private static final class Task {
        final boolean continuous, mutation;
        final CompletableFuture<CPU.RunState> result;
        private Thread runner;
        private boolean interrupted;
        Task(boolean continuous, boolean mutation, CompletableFuture<CPU.RunState> result) {
            this.continuous = continuous; this.mutation = mutation; this.result = result;
        }
        synchronized void requestStop() {
            if (mutation) return;
            interrupted = true;
            if (runner != null) runner.interrupt();
        }
        synchronized void enter() {
            runner = Thread.currentThread();
            if (interrupted) runner.interrupt();
        }
        synchronized void exit() { runner = null; }
    }

    private static final class Pending {
        final Stop kind;
        final Integer location;
        final boolean executing;
        final CompletableFuture<CPU.RunState> result;
        Pending(Stop kind, Integer location, CompletableFuture<CPU.RunState> result, boolean executing) {
            this.kind = kind; this.location = location; this.result = result; this.executing = executing;
        }
    }

    public EmulationController(CPU cpu, CPUContext cpuContext, Memory memory, List<Device> devices, DebugSession debugger) {
        this(cpu, cpuContext, memory, devices, debugger, null);
    }

    public EmulationController(CPU cpu, CPUContext cpuContext, Memory memory, List<Device> devices, DebugSession debugger, Compiler compiler) {
        this.cpu = cpu;
        this.cpuContext = cpu == null ? null : Objects.requireNonNull(cpuContext);
        this.memory = memory;
        this.devices = List.copyOf(devices);
        this.debugger = debugger;
        this.compiler = compiler;

        commands.setRemoveOnCancelPolicy(true);
        commands.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        commands.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        publish(state);
    }

    public CPU.RunState getRunState() { return state; }
    public int getInstructionLocation() { return cpuContext == null ? -1 : cpuContext.getInstructionLocation(); }
    /** Whether an application command or cooperative close is pending or executing. */
    public boolean isBusy() { return busy || closing; }
    /** Whether application-scheduled stepping is active, including the delay between instructions. */
    public boolean isTimedRunning() { return timedRunning; }
    public CPUContext cpuContext() { return cpuContext; }
    public CompletableFuture<CPU.RunState> runCompletion() { return runCompletion.copy(); }

    private <T> CompletableFuture<T> command(Consumer<CompletableFuture<T>> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            commands.execute(() -> {
                try {
                    if (closing) throw new IllegalStateException("Machine is closing");
                    operation.accept(result);
                } catch (Throwable failure) { result.completeExceptionally(failure); }
            });
        } catch (RejectedExecutionException failure) { result.completeExceptionally(failure); }
        return result.copy();
    }

    private void requirePaused() {
        if (pending != null || busy) throw new IllegalStateException("Machine command is pending");
        if (active != null || state == CPU.RunState.STATE_RUNNING) throw new IllegalStateException("Pause CPU first");
    }

    public CompletableFuture<CPU.RunState> start() {
        return command(result -> {
            requireCpu();
            requirePaused();
            if (state != CPU.RunState.STATE_STOPPED_BREAK) throw new IllegalStateException("Reset CPU first");
            cancelTimer();
            runCompletion = new CompletableFuture<>();
            launch(true, runCompletion, cpu::execute, false);
            result.complete(CPU.RunState.STATE_RUNNING);
        });
    }

    public CompletableFuture<CPU.RunState> step() {
        return command(result -> { requirePaused(); cancelTimer(); launchStep(result, -1, 0); });
    }

    public CompletableFuture<CPU.RunState> step(long delay, TimeUnit unit) {
        long nanos = Math.max(1, unit.toNanos(delay));
        return command(result -> {
            requireCpu();
            if (pending != null || (busy && !timedRunning)) throw new IllegalStateException("Machine command is pending");
            if (active != null && active.continuous) throw new IllegalStateException("Pause CPU first");
            if (state != CPU.RunState.STATE_STOPPED_BREAK && !timedRunning) throw new IllegalStateException("Reset CPU first");
            cancelTimer();
            timedRunning = true;
            long generation = timerGeneration;
            if (active == null) scheduleStep(generation, nanos, 0);
            else active.result.whenComplete((ignored, failure) -> {
                if (generation == timerGeneration && timedRunning && !closing) scheduleStep(generation, nanos, nanos);
            });
            publish(state);
            result.complete(state);
        });
    }

    private void scheduleStep(long generation, long delay, long initialDelay) {
        tick = commands.schedule(() -> {
            if (generation != timerGeneration || !timedRunning || closing) return;
            if (state != CPU.RunState.STATE_STOPPED_BREAK || active != null) { cancelTimer(); return; }
            launchStep(new CompletableFuture<>(), generation, delay);
        }, initialDelay, TimeUnit.NANOSECONDS);
    }

    private void launchStep(CompletableFuture<CPU.RunState> result, long generation, long delay) {
        requireCpu();
        if (state != CPU.RunState.STATE_STOPPED_BREAK) throw new IllegalStateException("Reset CPU first");
        if (debugger != null) debugger.prepareManualStep();
        launch(false, result, cpu::step, false);
        if (generation >= 0) result.whenComplete((value, failure) -> {
            if (generation == timerGeneration && timedRunning && !closing) {
                if (failure == null && value == CPU.RunState.STATE_STOPPED_BREAK) scheduleStep(generation, delay, delay);
                else cancelTimer();
            }
        });
    }

    private void cancelTimer() {
        boolean changed = timedRunning;
        timerGeneration++; timedRunning = false;
        if (tick != null) tick.cancel(false);
        tick = null;
        if (changed) publish(state);
    }

    private void requireCpu() {
        if (cpu == null) throw new IllegalStateException("No CPU configured");
    }

    public CompletableFuture<CPU.RunState> pause() { return stop(Stop.PAUSE, null); }
    public CompletableFuture<CPU.RunState> stop() { return stop(Stop.STOP, null); }
    public CompletableFuture<CPU.RunState> reset() { return stop(Stop.RESET, null); }
    public CompletableFuture<CPU.RunState> reset(int location) { return stop(Stop.RESET, location); }

    private CompletableFuture<CPU.RunState> stop(Stop kind, Integer location) {
        return command(result -> {
            if (pending != null || (busy && (active == null || active.mutation)))
                throw new IllegalStateException("Machine command is pending");
            cancelTimer(); pending = new Pending(kind, location, result, active != null); busy = true; mutationGeneration++;
            publish(state);
            if (active != null) active.requestStop(); else finishPending();
        });
    }

    private void finishPending() {
        Pending operation = pending;
        if (operation == null) { finishClose(); return; }
        if (operation.kind == Stop.RESET) {
            launch(false, operation.result, () -> {
                if (compiler != null) compiler.reset();
                if (memory != null) memory.reset();
                for (Device device : devices) device.reset();
                if (cpu != null) {
                    if (operation.location == null) cpu.reset(); else cpu.reset(operation.location);
                }
                return CPU.RunState.STATE_STOPPED_BREAK;
            }, true);
        } else {
            if (operation.kind == Stop.STOP && !operation.executing && state == CPU.RunState.STATE_STOPPED_BREAK)
                state = CPU.RunState.STATE_STOPPED_NORMAL;
            pending = null; busy = false; publish(state); operation.result.complete(state); finishClose();
        }
    }

    public CompletableFuture<Void> mutate(Runnable mutation) {
        Objects.requireNonNull(mutation);
        long expectedGeneration = mutationGeneration;
        return this.<CPU.RunState>command(result -> {
            requirePaused();
            if (expectedGeneration != mutationGeneration) throw new IllegalStateException("Machine edit is stale");
            CPU.RunState previous = state;
            launch(false, result, () -> { mutation.run(); return previous; }, true);
        }).thenApply(ignored -> null);
    }

    /** Compiles on the worker without resetting memory, then resets only CPU execution position. */
    public CompletableFuture<CPU.RunState> compile(Runnable compilation) {
        return compile(compilation, null);
    }

    public CompletableFuture<CPU.RunState> compile(Runnable compilation, Integer location) {
        Objects.requireNonNull(compilation);
        return command(result -> {
            requirePaused();
            cancelTimer();
            launch(false, result, () -> {
                compilation.run();
                if (cpu != null) {
                    if (location == null) cpu.reset(); else cpu.reset(location);
                }
                return CPU.RunState.STATE_STOPPED_BREAK;
            }, true, true);
        });
    }

    private void launch(boolean continuous, CompletableFuture<CPU.RunState> result,
                        Callable<CPU.RunState> operation, boolean mutation) {
        launch(continuous, result, operation, mutation, false);
    }

    private void launch(boolean continuous, CompletableFuture<CPU.RunState> result,
                        Callable<CPU.RunState> operation, boolean mutation, boolean compilation) {
        CPU.RunState previousState = state;
        Task task = new Task(continuous, mutation, result);
        active = task; busy = !continuous || pending != null || closing; mutationGeneration++;
        publish(mutation ? state : CPU.RunState.STATE_RUNNING);
        worker.execute(() -> {
            task.enter();
            CPU.RunState[] returned = {CPU.RunState.STATE_STOPPED_BAD_INSTR};
            Throwable failure = null;
            try {
                Runnable action = () -> {
                    try { returned[0] = Objects.requireNonNull(operation.call()); }
                    catch (Exception problem) { throw new CompletionException(problem); }
                };
                if (debugger == null) action.run();
                else if (compilation) debugger.runCompilation(action);
                else if (mutation) debugger.runMutation(action);
                else debugger.runExecution(action);
            } catch (Throwable problem) {
                failure = problem instanceof CompletionException && problem.getCause() != null ? problem.getCause() : problem;
                returned[0] = mutation ? previousState
                        : failure instanceof IndexOutOfBoundsException || failure.getCause() instanceof IndexOutOfBoundsException
                                ? CPU.RunState.STATE_STOPPED_ADDR_FALLOUT : CPU.RunState.STATE_STOPPED_BAD_INSTR;
            } finally {
                task.exit();
                Thread.interrupted();
            }
            CPU.RunState value = returned[0];
            Throwable error = failure;
            commands.execute(() -> finished(task, value, error));
        });
    }

    private void finished(Task task, CPU.RunState returned, Throwable failure) {
        if (active != task) return;
        active = null;
        if (!task.continuous && !task.mutation && debugger != null) debugger.completeManualStep();
        if (returned == CPU.RunState.STATE_RUNNING) returned = (pending != null && pending.kind == Stop.STOP) || closing
                ? CPU.RunState.STATE_STOPPED_NORMAL : CPU.RunState.STATE_STOPPED_BREAK;
        state = returned; busy = pending != null || closing; mutationGeneration++;
        if (pending != null && task.result == pending.result) { pending = null; busy = closing; }
        publish(state, true);
        if (debugger != null) debugger.completeSourceCapture();
        if (failure == null) task.result.complete(state); else task.result.completeExceptionally(failure);
        if (pending != null) finishPending();
        else finishClose();
    }

    private void publish(CPU.RunState value) {
        publish(value, false);
    }

    private void publish(CPU.RunState value, boolean internalStateChanged) {
        state = value;
        if (!internalStateChanged && publishedState == value) return;
        publishedState = value;
        if (debugger != null) debugger.executionChanged(value);
    }

    public CompletableFuture<Void> closeAsync() {
        try {
            commands.execute(() -> {
                if (closing) return;
                closing = true; busy = true; cancelTimer(); publish(state);
                if (active != null) active.requestStop(); else if (pending == null) finishClose();
            });
        } catch (RejectedExecutionException failure) { if (!closed.isDone()) closed.completeExceptionally(failure); }
        return closed.copy();
    }

    private void finishClose() {
        if (!closing || active != null || pending != null || shutDown) return;
        shutDown = true;
        worker.shutdown(); commands.shutdown(); closed.complete(null);
    }

    /** Blocking lifecycle hook for non-UI callers. UI callers use closeAsync(). */
    @Override public void close() { closeAsync().join(); }
}
