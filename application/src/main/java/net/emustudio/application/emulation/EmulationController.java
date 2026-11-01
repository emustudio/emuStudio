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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@ThreadSafe
public class EmulationController implements Closeable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CPU cpu;
    private final Memory memory;
    private final List<Device> devices;
    private final DebugSession debugger;

    private volatile CountDownLatch countDownLatch;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;
    private volatile boolean timedRunning = false;

    public EmulationController(CPU cpu, Memory memory, List<Device> devices, DebugSession debugger) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = memory;
        this.devices = Objects.requireNonNull(devices);
        this.debugger = debugger;

        cpu.addCPUListener(new TheCPUListener());
    }

    private void submitMutation(Runnable operation) {
        executor.submit(() -> {
            if (debugger == null) operation.run();
            else debugger.runMutation(operation);
        });
    }

    private void awaitLatch() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        submitMutation(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.execute();
            awaitLatch();
        });
    }

    public void stop() {
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
        submitMutation(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.step();
            awaitLatch();
        });
    }

    public boolean isTimedRunning() {
        return timedRunning;
    }

    public void step(long sleep, TimeUnit timeUnit) {
        this.timedRunning = true;
        internalStep(sleep, timeUnit);
    }

    private void internalStep(final long sleep, final TimeUnit timeUnit) {
        submitMutation(() -> {
            if (runState == CPU.RunState.STATE_STOPPED_BREAK && timedRunning) {
                countDownLatch = new CountDownLatch(1);
                cpu.step();
                awaitLatch();

                LockSupport.parkNanos(timeUnit.toNanos(sleep));
                internalStep(sleep, timeUnit);
            } else {
                timedRunning = false;
            }
        });
    }

    public void pause() {
        timedRunning = false;
        submitMutation(() -> {
            if (runState == CPU.RunState.STATE_RUNNING) {
                countDownLatch = new CountDownLatch(1);
                cpu.pause();
                awaitLatch();
            }
        });
    }

    public void reset() {
        timedRunning = false;
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
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
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
