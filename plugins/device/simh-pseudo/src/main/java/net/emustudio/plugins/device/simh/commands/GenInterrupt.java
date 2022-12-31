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

public class GenInterrupt implements Command {
    public final static GenInterrupt INS = new GenInterrupt();

    private int genInterruptPos = 0; // determines state for receiving interrupt vector and data
    private int genInterruptVec = 0; // stores interrupt vector

    @Override
    public void write(byte data, Control control) {
        if (genInterruptPos == 0) {
            genInterruptVec = data; // interrupt vector is not used.
            genInterruptPos = 1;
            System.out.println("genInterruptVec=" + genInterruptVec + " genInterruptPos=" + genInterruptPos);
        } else {
            control.getCpu().signalInterrupt(new byte[]{data});
            genInterruptPos = 0;
            control.clearCommand();
            System.out.printf(
                    "genInterruptVec=%d vectorInterrupt=%X dataBus=%02X genInterruptPos=%d\n",
                    genInterruptVec, 1 << genInterruptVec, data, genInterruptPos);
        }
    }

    @Override
    public void start(Control control) {
        control.clearReadCommand();
    }
}
