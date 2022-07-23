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

public class SetTimerDelta implements Command {
    public final static SetTimerDelta INS = new SetTimerDelta();

    private final static int DEFAULT_TIMER_DELTA = 100; // default value for timer delta in ms

    private int setTimerDeltaPos = 0; // determines state for receiving timerDelta
    public int timerDelta = DEFAULT_TIMER_DELTA;  // interrupt every 100 ms

    @Override
    public void reset() {
        setTimerDeltaPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerDeltaPos == 0) {
            timerDelta = data;
            setTimerDeltaPos = 1;
        } else {
            timerDelta |= (data << 8);
            setTimerDeltaPos = 0;
            control.clearCommand();
            if (timerDelta == 0) {
                timerDelta = DEFAULT_TIMER_DELTA;
                System.out.println("SIMH: Timer delta set to 0 ms ignored. Using " + DEFAULT_TIMER_DELTA + " ms instead.");
            }
        }
    }

    @Override
    public void start(Control control) {
        reset();
        control.clearReadCommand();
    }
}
