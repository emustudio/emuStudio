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

package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;

import java.util.Objects;

public class RASPDisassembler implements Disassembler {

    private final RASPMemoryContext memory;

    public RASPDisassembler(RASPMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int memoryPosition) {

        //retrieve the instruction
        MemoryItem item = memory.read(memoryPosition);
        if (!(item instanceof RASPInstruction)) {
            //what we retrieved is not a valid instruction, it can be either invalid, or it is a data memory item before the program start
            return new DisassembledInstruction(memoryPosition, "unknown", "");
        }
        RASPInstruction instruction = (RASPInstruction) item;

        int opCode = instruction.getCode();
        //true if instruction is a jump instruction, false otherwise
        boolean jumpInstruction = false;

        if (opCode == RASPInstruction.JMP || opCode == RASPInstruction.JZ || opCode == RASPInstruction.JGTZ) {
            jumpInstruction = true;
        }

        //retrieve its operand
        item = memory.read(memoryPosition + 1);
        if (!(item instanceof NumberMemoryItem)) {
            return new DisassembledInstruction(memoryPosition, "unknown", "");
        }
        NumberMemoryItem operand = (NumberMemoryItem) item;

        //prepare the mnemonic form
        if (jumpInstruction) { //if we work with jump instr., mnemo should contain the label
            String label = memory.addressToLabelString(operand.getValue());
            String mnemo = instruction.getCodeStr() + " " + label;
            return new DisassembledInstruction(memoryPosition, mnemo, "");
        }
        String mnemo = instruction.getCodeStr() + " " + operand.toString();
        return new DisassembledInstruction(memoryPosition, mnemo, "");
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        if (memoryPosition >= memory.getSize()) {
            return memoryPosition + 1;
        }
        MemoryItem item = memory.read(memoryPosition);
        if (item instanceof RASPInstruction) {
            RASPInstruction instruction = (RASPInstruction) item;
            if (instruction.getCode() == RASPInstruction.HALT) {
                return memoryPosition + 1;
            }
            return memoryPosition + 2;
        }
        return memoryPosition + 1;
    }

}
