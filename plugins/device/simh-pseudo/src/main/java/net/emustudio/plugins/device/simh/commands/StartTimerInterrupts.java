/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class StartTimerInterrupts implements Command {
    public final static StartTimerInterrupts INS = new StartTimerInterrupts();
    private static final int TRY_AFTER_CYCLES = 50000;
    public final AtomicReference<TimerInterruptCallback> callback = new AtomicReference<>();

    @Override
    public void reset(Control control) {
        TimerInterruptCallback old = callback.getAndSet(null);
        if (old != null) {
            control.getCpu().removePassedCyclesListener(old);
        }
    }

    @Override
    public void start(Control control) {
        reset(control);

        Context8080 cpu = control.getCpu();
        TimerInterruptCallback cb = new TimerInterruptCallback(cpu);
        callback.set(cb);

        cpu.addPassedCyclesListener(cb);
        control.clearCommand();
    }

    private static class TimerInterruptCallback implements CPUContext.PassedCyclesListener {
        private final Context8080 cpu;
        private long startTime = System.nanoTime();
        private long cyclesPassed = 0;

        private TimerInterruptCallback(Context8080 cpu) {
            this.cpu = Objects.requireNonNull(cpu);
        }

        @Override
        public void passedCycles(long cycles) {
            cyclesPassed += cycles;

            if (cyclesPassed >= TRY_AFTER_CYCLES) {
                cyclesPassed -= TRY_AFTER_CYCLES;
                long endTime = System.nanoTime();
                long elapsed = endTime - startTime;

                if (elapsed >= (SetTimerDelta.INS.timerDelta * 1000000L)) {
                    startTime = endTime;
                    // will work only in interrupt mode 0
                    int addr = SetTimerInterruptAdr.INS.timerInterruptHandler;
                    byte b1 = (byte) (addr & 0xFF);
                    byte b2 = (byte) (addr >>> 8);
                    cpu.signalInterrupt(new byte[]{(byte) 0xCD, b1, b2});
                }
            }
        }
    }
}
