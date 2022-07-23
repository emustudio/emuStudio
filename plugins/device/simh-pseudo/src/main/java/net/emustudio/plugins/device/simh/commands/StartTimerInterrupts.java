/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class StartTimerInterrupts implements Command {
    public final static StartTimerInterrupts INS = new StartTimerInterrupts();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public final AtomicReference<ScheduledFuture<?>> timerTask = new AtomicReference<>();

    @Override
    public void reset() {
        ScheduledFuture<?> oldTask = timerTask.get();
        if (oldTask != null) {
            oldTask.cancel(true);
        }
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        reset();

        timerTask.set(executor.scheduleAtFixedRate(() -> {
            // will work only in interrupt mode 0
            int addr = SetTimerInterruptAdr.INS.timerInterruptHandler;
            byte b1 = (byte) (addr & 0xFF);
            byte b2 = (byte) (addr >>> 8);
            control.getCpu().signalInterrupt(control.getDevice(), new byte[]{(byte) 0xCD, b1, b2});
        }, SetTimerDelta.INS.timerDelta, SetTimerDelta.INS.timerDelta, TimeUnit.MILLISECONDS));
    }
}
