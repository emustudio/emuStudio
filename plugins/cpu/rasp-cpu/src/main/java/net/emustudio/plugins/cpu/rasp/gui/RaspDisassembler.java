/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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

package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.Objects;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;

public class RaspDisassembler implements Disassembler {
    private final RaspMemoryContext memory;

    public RaspDisassembler(RaspMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int address) {
        RaspMemoryCell in = memory.read(address);
        if (in == null) {
            return new DisassembledInstruction(address, "", "");
        }
        int opcode = in.getValue();
        String rawMnemo = memory.disassemble(opcode).orElse(String.valueOf(opcode));
        String rawOperand = "";

        if (opcode != HALT) {
            boolean isJump = (opcode == JMP) || (opcode == JZ) || (opcode == JGTZ);
            int operand = memory.read(address + 1).getValue();
            rawOperand = isJump ?
                    memory.getLabel(operand).map(RaspLabel::getLabel).orElse(String.valueOf(operand)) :
                    String.valueOf(operand);
        }

        String mnemo = String.format("%s %s", rawMnemo.toLowerCase(), rawOperand.toUpperCase());
        return new DisassembledInstruction(address, mnemo, Integer.toHexString(opcode));
    }

    @Override
    public int getNextInstructionPosition(int address) throws IndexOutOfBoundsException {
        RaspMemoryCell item = memory.read(address);
        if (!item.isInstruction() || item.getValue() == HALT) {
            return address + 1;
        }
        return address + 2;
    }
}
