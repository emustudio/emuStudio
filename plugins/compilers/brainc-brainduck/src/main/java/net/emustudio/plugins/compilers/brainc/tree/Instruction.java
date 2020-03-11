/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.brainc.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;

public class Instruction {
    public final static int HALT = 0;
    public final static int INC = 1;
    public final static int DEC = 2;
    public final static int INCV = 3;
    public final static int DECV = 4;
    public final static int PRINT = 5;
    public final static int LOAD = 6;
    public final static int LOOP = 7;
    public final static int ENDL = 8;
    private final int instructionCode;

    public Instruction(int instr) {
        this.instructionCode = instr;
    }

    public int firstPass(int addressStart) {
        return addressStart + 1;
    }

    public void secondPass(IntelHEX hex) {
        hex.putCode(String.format("%1$02X", instructionCode));
    }
}
