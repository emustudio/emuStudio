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

public class StartTimer implements Command {
    public final static StartTimer INS = new StartTimer();

    private final static int TIMER_STACK_LIMIT = 10; // stack depth of timer stack
    // stop watch and timer related
    public int markTimeSP = 0; // stack pointer for timer stack
    public long[] markTime = new long[TIMER_STACK_LIMIT];  // timer stack

    @Override
    public void reset() {
        markTimeSP = 0;
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
        if (markTimeSP < TIMER_STACK_LIMIT) {
            markTime[markTimeSP++] = System.currentTimeMillis();
        } else {
            System.out.println("SIMH: Timer stack overflow");
        }
    }
}
