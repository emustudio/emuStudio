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

public class GetCommon implements Command {
    public final static GetCommon INS = new GetCommon();

    private int getCommonPos = 0; // determines state for sending the 'common' register

    @Override
    public void reset(Control control) {
        getCommonPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getCommonPos == 0) {
            result = (byte) (control.getMemory().getCommonBoundary() & 0xff);
            getCommonPos = 1;
        } else {
            result = (byte) ((control.getMemory().getCommonBoundary() >> 8) & 0xff);
            getCommonPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void start(Control control) {
        getCommonPos = 0;
        control.clearWriteCommand();
    }
}
