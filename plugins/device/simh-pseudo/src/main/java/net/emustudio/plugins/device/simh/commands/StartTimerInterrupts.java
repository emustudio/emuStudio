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
