/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.emulation;

import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import net.jcip.annotations.ThreadSafe;

import java.io.Closeable;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@ThreadSafe
public class EmulationController implements Closeable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CPU cpu;
    private final Optional<Memory> memory;
    private final Supplier<ListIterator<Device>> devices;

    private volatile CountDownLatch countDownLatch;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public EmulationController(CPU cpu, Optional<Memory> memory, Supplier<ListIterator<Device>> devices) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);
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

    public void step(final long sleep, final TimeUnit timeUnit) {
        executor.submit(() -> {
            if (runState == CPU.RunState.STATE_STOPPED_BREAK) {
                countDownLatch = new CountDownLatch(1);
                cpu.step();
                awaitLatch();

                LockSupport.parkNanos(timeUnit.toNanos(sleep));
                step(sleep, timeUnit);
            }
        });
    }

    public void pause() {
        executor.submit(() -> {
            if (runState == CPU.RunState.STATE_RUNNING) {
                countDownLatch = new CountDownLatch(1);
                cpu.pause();
                awaitLatch();
            }
        });
    }

    public void reset() {
        executor.submit(() -> {
            countDownLatch = new CountDownLatch(1);

            if (memory.isPresent()) {
                cpu.reset(memory.get().getProgramStart()); // first address of an image??
                memory.get().reset();
            } else {
                cpu.reset();
            }
            awaitLatch();

            ListIterator<Device> deviceIterator = devices.get();
            while (deviceIterator.hasNext()) {
                deviceIterator.next().reset();
            }
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
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
