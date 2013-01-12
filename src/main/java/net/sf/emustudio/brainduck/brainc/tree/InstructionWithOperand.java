/*
 * InstructionWithOperand.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.brainduck.brainc.tree;

import emulib.runtime.HEXFileManager;

public class InstructionWithOperand implements Instruction {
    public final static int PRINT = 5;
    public final static int LOAD = 6;
    public final static int INC = 9;
    public final static int DEC = 10;
    public final static int INCV = 11;
    public final static int DECV = 12;
    private int instr;
    private int param;
    
    public InstructionWithOperand(int instr, int param) {
        this.instr = instr;
        this.param = param;
    }

    @Override
    public int firstPass(int addressStart) throws Exception {
        return addressStart + 2;
    }

    @Override
    public void secondPass(HEXFileManager hex) {
        hex.putCode(String.format("%1$02X%2$02X", instr, param));
    }
}
