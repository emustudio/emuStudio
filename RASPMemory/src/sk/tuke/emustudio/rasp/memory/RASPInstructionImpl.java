/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory;

public class RASPInstructionImpl implements RASPInstruction {

    /**
     * machine code of the instruction
     */
    private final int instructionCode;

    /**
     * the type of the operand of the instruction; it determines how the
     * subsequent number will be interpreted
     */
    private final OperandType operandType;

    /**
     * Constructor.
     *
     * @param instructionCode the machine code of the instruction
     * @param operandType the type of the operand of the instruction
     */
    public RASPInstructionImpl(int instructionCode, OperandType operandType) {
        this.instructionCode = instructionCode;
        this.operandType = operandType;
    }

    @Override
    public int getCode() {
        return instructionCode;
    }

    @Override
    public OperandType getOperandType() {
        return operandType;
    }

    @Override
    public String getCodeStr() {
        switch (instructionCode) {
            case LOAD:
                return "LOAD";
            case STORE:
                return "STORE";
            case READ:
                return "READ";
            case WRITE:
                return "WRITE";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
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

}
