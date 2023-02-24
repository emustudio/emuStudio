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
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.util.Objects;

public class RAMDisassembler implements Disassembler {
    private final RAMMemoryContext memory;

    public RAMDisassembler(RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int memLocation) {
        String mnemo, oper = "";

        RAMInstruction in = memory.read(memLocation);
        if (in == null) {
            mnemo = "none";
            return new DisassembledInstruction(memLocation, mnemo, oper);
        }
        mnemo = in.getOpcode().toString() + " " + in.getDirection().value() +
                in.getOperand().map(RAMValue::getStringRepresentation).orElse("");
        return new DisassembledInstruction(memLocation, mnemo, oper);
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        return memoryPosition + 1;
    }
}
