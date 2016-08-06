/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.emustudio.rasp.memory.memoryitems;

import net.sf.emustudio.rasp.memory.memoryitems.RASPInstruction;

public class RASPInstructionImpl implements RASPInstruction {

    /**
     * machine code of the instruction
     */
    private final int instructionCode;

    /**
     * Constructor.
     *
     * @param instructionCode the machine code of the instruction
     */
    public RASPInstructionImpl(int instructionCode) {
        this.instructionCode = instructionCode;
    }

    @Override
    public int getCode() {
        return instructionCode;
    }

    @Override
    public String getCodeStr() {
        switch (instructionCode) {
            case READ:
                return "READ";
            case WRITE_CONSTANT:
                return "WRITE =";
            case WRITE_REGISTER:
                return "WRITE";
            case LOAD_CONSTANT:
                return "LOAD =";
            case LOAD_REGISTER:
                return "LOAD";
            case STORE:
                return "STORE";
            case ADD_CONSTANT:
                return "ADD =";
            case ADD_REGISTER:
                return "ADD";
            case SUB_CONSTANT:
                return "SUB =";
            case SUB_REGISTER:
                return "SUB";
            case MUL_CONSTANT:
                return "MUL =";
            case MUL_REGISTER:
                return "MUL";
            case DIV_CONSTANT:
                return "DIV =";
            case DIV_REGISTER:
                return "DIV";
            case JMP:
                return "JMP";
            case JZ:
                return "JZ";
            case JGTZ:
                return "JGTZ";
            case HALT:
                return "HALT";
        }
        return "unknown";
    }

    @Override
    public String toString() {
        return getCodeStr();
    } 

}
