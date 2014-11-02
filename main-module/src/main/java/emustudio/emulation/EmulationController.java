package emustudio.emulation;

import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.Computer;
import net.jcip.annotations.ThreadSafe;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@ThreadSafe
public class EmulationController implements Closeable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Computer computer;

    private volatile CountDownLatch countDownLatch;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    private final CPU.CPUListener cpuListener = new TheCPUListener();

    public EmulationController(Computer computer) {
        this.computer = computer;
        this.computer.getCPU().addCPUListener(cpuListener);
    }

    private void awaitLatch() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                    return; // invalid state
                }
                countDownLatch = new CountDownLatch(1);
                computer.getCPU().execute();
                awaitLatch();
            }
        });
    }

    public void stop() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (runState != CPU.RunState.STATE_STOPPED_BREAK && runState != CPU.RunState.STATE_RUNNING) {
                    return; // invalid state
                }
                countDownLatch = new CountDownLatch(1);
                computer.getCPU().stop();
                awaitLatch();
            }
        });
    }

    public void step() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (runState != CPU.RunState.STATE_STOPPED_BREAK) {
                    return; // invalid state
                }
                countDownLatch = new CountDownLatch(1);
                computer.getCPU().step();
                awaitLatch();
            }
        });
    }

    public void step(final long sleep, final TimeUnit timeUnit) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (runState == CPU.RunState.STATE_STOPPED_BREAK) {
                    countDownLatch = new CountDownLatch(1);
                    computer.getCPU().step();
                    awaitLatch();

                    LockSupport.parkNanos(timeUnit.toNanos(sleep));
                    step(sleep, timeUnit);
                }
            }
        });
    }

    public void pause() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (runState == CPU.RunState.STATE_RUNNING) {
                    countDownLatch = new CountDownLatch(1);
                    computer.getCPU().pause();
                    awaitLatch();
                }
            }
        });
    }

    public void reset() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                countDownLatch = new CountDownLatch(1);

                CPU cpu = computer.getCPU();
                Memory memory = computer.getMemory();

                if (memory != null) {
                    cpu.reset(memory.getProgramStart()); // first address of an image??
                    memory.reset();
                } else {
                    cpu.reset();
                }
                awaitLatch();

                Device devices[] = computer.getDevices();
                if (devices != null) {
                    for (Device device : devices) {
                        device.reset();
                    }
                }
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
