/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.jcip.annotations.ThreadSafe;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Serializes interactive machine commands; CPU callbacks only acknowledge state changes. */
@ThreadSafe
public class EmulationController implements Closeable {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final CPU cpu;
    private final Memory memory;
    private final List<Device> devices;
    private final DebugSession debugger;
    private final CPU.CPUListener cpuListener = new TheCPUListener();

    private volatile CountDownLatch countDownLatch;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;
    // Guarded by this; a cancelled in-flight step must not affect its replacement.
    private ScheduledFuture<?> timedStep;
    private long timedStepGeneration;

    public EmulationController(CPU cpu, Memory memory, List<Device> devices, DebugSession debugger) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = memory;
        this.devices = Objects.requireNonNull(devices);
        this.debugger = debugger;

        executor.setRemoveOnCancelPolicy(true);
        cpu.addCPUListener(cpuListener);
    }

    private void submitMutation(Runnable operation) {
        executor.submit(() -> runMutation(operation));
    }

    private void runMutation(Runnable operation) {
        if (debugger == null) operation.run();
        else debugger.runMutation(operation);
    }

    private void submitExecution(Runnable operation) {
        executor.submit(() -> runExecution(operation));
    }

    private void runExecution(Runnable operation) {
        if (debugger == null) operation.run();
        else debugger.runExecution(operation);
    }

    private void awaitLatch() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        submitExecution(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.execute();
            awaitLatch();
        });
    }

    public synchronized void stop() {
        cancelTimedStep();
        submitMutation(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK && runState != CPU.RunState.STATE_RUNNING) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.stop();
            awaitLatch();
        });
    }

    public void step() {
        step(() -> {});
    }

    public void step(Runnable completed) {
        executor.submit(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) return;
            runExecution(() -> {
                countDownLatch = new CountDownLatch(1);
                cpu.step();
                awaitLatch();
            });
            completed.run();
        });
    }

    public synchronized boolean isTimedRunning() {
        return timedStep != null && !timedStep.isDone();
    }

    public synchronized void step(long sleep, TimeUnit timeUnit) {
        long delay = Math.max(1, timeUnit.toNanos(sleep));
        cancelTimedStep();
        long generation = timedStepGeneration;
        timedStep = executor.scheduleWithFixedDelay(() -> internalStep(generation), 0, delay, TimeUnit.NANOSECONDS);
    }

    private void internalStep(long generation) {
        synchronized (this) {
            if (generation != timedStepGeneration) return;
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                cancelTimedStep();
                return;
            }
        }
        runExecution(() -> {
            countDownLatch = new CountDownLatch(1);
            cpu.step();
            awaitLatch();
        });
        synchronized (this) {
            if (generation == timedStepGeneration && runState != CPU.RunState.STATE_STOPPED_BREAK)
                cancelTimedStep();
        }
    }

    private void cancelTimedStep() {
        timedStepGeneration++;
        if (timedStep != null) timedStep.cancel(false);
        timedStep = null;
    }

    public synchronized void pause() {
        cancelTimedStep();
        submitMutation(() -> {
            if (runState == CPU.RunState.STATE_RUNNING) {
                countDownLatch = new CountDownLatch(1);
                cpu.pause();
                awaitLatch();
            }
        });
    }

    public synchronized void reset() {
        cancelTimedStep();
        submitMutation(() -> {
            // pause() acknowledges execution exit, even if natural completion won the race.
            cpu.pause();
            if (memory != null) {
                memory.reset();
            }
            for (Device device : devices) {
                device.reset();
            }
            countDownLatch = new CountDownLatch(1);
            cpu.reset();
            awaitLatch();
        });
    }

    @Override
    public void close() {
        synchronized (this) {
            cancelTimedStep();
            executor.shutdown();
        }
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            cpu.removeCPUListener(cpuListener);
        }
    }

    private class TheCPUListener implements CPU.CPUListener {

        @Override
        public void runStateChanged(CPU.RunState runState) {
            EmulationController.this.runState = runState;
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void internalStateChanged() {

        }
    }
}
