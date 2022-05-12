/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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

package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.rasp.api.RASPLabel;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.util.Objects;

public class RASPDisassembler implements Disassembler {
    private final RASPMemoryContext memory;

    public RASPDisassembler(RASPMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int address) {
        RASPMemoryCell instruction = memory.read(address);
        int opcode = instruction.getValue();
        String mnemo = memory.disassemble(opcode).orElse("unknown");

        boolean isJump = (opcode == 15) || (opcode == 16) || (opcode == 17);
        if (opcode != 18) {
            int operand = memory.read(address + 1).getValue();
            String operandStr = isJump ?
                memory.getLabel(operand).map(RASPLabel::getLabel).orElse(String.valueOf(operand)) :
                String.valueOf(operand);
            mnemo += " " + operandStr;
        }

        return new DisassembledInstruction(address, mnemo, String.valueOf(opcode));
    }

    @Override
    public int getNextInstructionPosition(int address) throws IndexOutOfBoundsException {
        if (address >= memory.getSize()) {
            return address + 1;
        }
        RASPMemoryCell item = memory.read(address);
        if (!item.isInstruction() || item.getValue() == 18) {
            return address + 1;
        }
        return address + 2;
    }

}
