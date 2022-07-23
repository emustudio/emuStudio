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

import java.util.concurrent.atomic.AtomicInteger;

public class GetCPUClockFrequency implements Command {
    public final static GetCPUClockFrequency INS = new GetCPUClockFrequency();

    private int getClockFrequencyPos = 0; // determines state for receiving the clock frequency
    private final AtomicInteger cpuFreq = new AtomicInteger();

    @Override
    public void reset() {
        getClockFrequencyPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getClockFrequencyPos == 0) {
            cpuFreq.set(control.getCpu().getCPUFrequency());
            result = (byte)(cpuFreq.get() & 0xff);
            getClockFrequencyPos = 1;
        } else {
            result = (byte)((cpuFreq.get() >> 8) & 0xff);
            getClockFrequencyPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        getClockFrequencyPos = 0;
    }
}
