/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.emulation;

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

    private volatile CountDownLatch countDownLatch;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;
    private volatile boolean timedRunning = false;

    public EmulationController(CPU cpu, Memory memory, List<Device> devices) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = memory;
        this.devices = Objects.requireNonNull(devices);

        cpu.addCPUListener(new TheCPUListener());
    }

    private void awaitLatch() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        executor.submit(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.execute();
            awaitLatch();
        });
    }

    public void stop() {
        executor.submit(() -> {
            if (runState != CPU.RunState.STATE_STOPPED_BREAK && runState != CPU.RunState.STATE_RUNNING) {
                return; // invalid state
            }
            countDownLatch = new CountDownLatch(1);
            cpu.stop();
            awaitLatch();
        });
    }

    public void step() {
        executor.submit(() -> {
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
        executor.submit(() -> {
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
        executor.submit(() -> {
            if (runState == CPU.RunState.STATE_RUNNING) {
                countDownLatch = new CountDownLatch(1);
                cpu.pause();
                awaitLatch();
            }
        });
    }

    public void reset() {
        timedRunning = false;
        executor.submit(() -> {
            countDownLatch = new CountDownLatch(1);

            if (memory != null) {
                memory.reset();
            }
            cpu.reset();

            awaitLatch();

            for (Device device : devices) {
                device.reset();
            }
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
