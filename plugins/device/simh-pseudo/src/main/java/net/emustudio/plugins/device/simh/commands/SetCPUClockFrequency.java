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

public class SetCPUClockFrequency implements Command {
    public final static SetCPUClockFrequency INS = new SetCPUClockFrequency();

    private int newClockFrequency;
    private int setClockFrequencyPos = 0; // determines state for sending the clock frequency

    @Override
    public void reset(Control control) {
        setClockFrequencyPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setClockFrequencyPos == 0) {
            newClockFrequency = data & 0xFF;
            setClockFrequencyPos = 1;
        } else {
            control.getCpu().setCPUFrequency(((data << 8) & 0xFF00) | newClockFrequency);
            setClockFrequencyPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        setClockFrequencyPos = 0;
        control.clearReadCommand();
    }
}
