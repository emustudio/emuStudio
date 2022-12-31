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

public class ReadStopWatch implements Command {
    public final static ReadStopWatch INS = new ReadStopWatch();
    public long stopWatchNow = 0; // stores starting time of stop watch
    private int getStopWatchDeltaPos = 0; // determines the state for receiving stopWatchDelta
    private long stopWatchDelta = 0; // stores elapsed time of stop watch

    @Override
    public void reset(Control control) {
        getStopWatchDeltaPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getStopWatchDeltaPos == 0) {
            result = (byte) (stopWatchDelta & 0xff);
            getStopWatchDeltaPos = 1;
        } else {
            result = (byte) ((stopWatchDelta >> 8) & 0xff);
            getStopWatchDeltaPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void start(Control control) {
        getStopWatchDeltaPos = 0;
        stopWatchDelta = System.currentTimeMillis() - stopWatchNow;
        control.clearWriteCommand();
    }
}
