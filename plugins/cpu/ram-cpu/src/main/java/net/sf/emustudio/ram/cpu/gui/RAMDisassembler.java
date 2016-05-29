/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

import java.util.Objects;

public class RAMDisassembler implements Disassembler {
    private RAMMemoryContext memory;

    public RAMDisassembler(RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
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
