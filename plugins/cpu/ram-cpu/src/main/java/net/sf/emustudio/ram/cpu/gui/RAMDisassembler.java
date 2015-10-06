/*
 * RAMDisassembler.java
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
package net.sf.emustudio.ram.cpu.gui;

import emulib.plugins.cpu.AbstractDisassembler;
import emulib.plugins.cpu.DisassembledInstruction;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

public class RAMDisassembler extends AbstractDisassembler {
    private RAMMemoryContext memory;

    public RAMDisassembler(RAMMemoryContext memory) {
        super (new RAMDecoder(memory));
        this.memory = memory;
    }

    @Override
    public DisassembledInstruction disassemble(int memLocation) {
        String mnemo, oper = "";

        RAMInstruction in = memory.read(memLocation);
        if (in == null) {
            mnemo = "unknown instruction";
            return new DisassembledInstruction(memLocation, mnemo, oper);
        }
        mnemo = in.getCodeStr() + " " + in.getOperandStr();
        return new DisassembledInstruction(memLocation, mnemo, oper);
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        return memoryPosition + 1;
    }
}
