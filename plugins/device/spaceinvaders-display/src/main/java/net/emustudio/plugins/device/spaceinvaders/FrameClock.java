/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class FrameClock implements AutoCloseable {
    private static final long HALF_FRAME_NANOS = 1_000_000_000L / 120;
    private final Context8080 cpu;
    private final Runnable frameReady;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "space-invaders-frame-clock");
        thread.setDaemon(true);
        return thread;
    });
    private boolean lowerScreen;

    FrameClock(Context8080 cpu, Runnable frameReady) {
        this.cpu = cpu;
        this.frameReady = frameReady;
    }

    void start() {
        executor.scheduleAtFixedRate(this::tick, HALF_FRAME_NANOS, HALF_FRAME_NANOS, TimeUnit.NANOSECONDS);
    }

    synchronized void tick() {
        lowerScreen = !lowerScreen;
        if (cpu.isInterruptSupported()) {
            cpu.signalInterrupt(new byte[]{(byte) (lowerScreen ? 0xCF : 0xD7)}); // RST 1 / RST 2
        }
        if (!lowerScreen) {
            frameReady.run();
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
