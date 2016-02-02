/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.cpu.gui;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.cpu.InvalidInstructionException;
import java.util.Objects;
import sk.tuke.emustudio.rasp.memory.MemoryItem;
import sk.tuke.emustudio.rasp.memory.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.OperandType;
import sk.tuke.emustudio.rasp.memory.RASPInstruction;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

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
         return new DisassembledInstruction(memoryPosition, "unknown", "");
         }
         RASPInstruction instruction = (RASPInstruction) item;

         //retrieve its operand
         item = memory.read(memoryPosition + 1);
         if (!(item instanceof NumberMemoryItem)) {
         return new DisassembledInstruction(memoryPosition, "unknown", "");
         }
         NumberMemoryItem operand = (NumberMemoryItem) item;

         //prepare the mnemonic form
         String operandType = (instruction.getOperandType() == OperandType.CONSTANT) ? "=" : "";
         String mnemo = instruction.getCodeStr() + " " + operandType + " " + operand.toString();
         return new DisassembledInstruction(memoryPosition, mnemo, "");
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        //RASP instruction is followed by its operand, so next instruction is at the memoryPosition + 2 
        return memoryPosition + 2;
    }

}
