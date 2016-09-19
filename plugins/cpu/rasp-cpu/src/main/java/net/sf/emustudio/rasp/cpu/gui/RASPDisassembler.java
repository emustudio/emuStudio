/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.cpu.gui;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.exceptions.InvalidInstructionException;
import java.util.Objects;
import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.NumberMemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.RASPInstruction;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;

/**
 * The disassembler implementation for RASP machine language.
 *
 * @author miso
 */
public class RASPDisassembler implements Disassembler {

    private final RASPMemoryContext memory;

    /**
     * Constructor.
     *
     * @param memory associated memory to read instructions and operand from
     */
    public RASPDisassembler(RASPMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int memoryPosition) throws InvalidInstructionException {

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
            jumpInstruction=true;
        }

        //retrieve its operand
        item = memory.read(memoryPosition + 1);
        if (!(item instanceof NumberMemoryItem)) {
            return new DisassembledInstruction(memoryPosition, "unknown", "");
        }
        NumberMemoryItem operand = (NumberMemoryItem) item;

        //prepare the mnemonic form
        if(jumpInstruction){ //if we work with jump instr., mnemo should contain the label
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
