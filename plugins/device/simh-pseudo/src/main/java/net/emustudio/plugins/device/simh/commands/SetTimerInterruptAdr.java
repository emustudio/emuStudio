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

public class SetTimerInterruptAdr implements Command {
    public static final SetTimerInterruptAdr INS = new SetTimerInterruptAdr();
    public int timerInterruptHandler = 0x0fc00; // default address of interrupt handling routine
    private int setTimerInterruptAdrPos = 0; // determines state for receiving timerInterruptHandler

    @Override
    public void reset(Control control) {
        setTimerInterruptAdrPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerInterruptAdrPos == 0) {
            timerInterruptHandler = data;
            setTimerInterruptAdrPos = 1;
        } else {
            timerInterruptHandler |= (data << 8);
            setTimerInterruptAdrPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        reset(control);
        control.clearReadCommand();
    }
}
