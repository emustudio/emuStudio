package sk.tuke.emustudio.rasp.memory;

/**
 * Class representing RASP instruction together with its code and operand type.
 *
 * @author miso
 */
public interface RASPInstruction {

    /**
     * Operation code of RASP instruction.
     */
    public final static int READ = 1;
    /**
     * Operation code of RASP instruction.
     */
    public final static int WRITE = 2;
    /**
     * Operation code of RASP instruction.
     */
    public final static int LOAD = 3;
    /**
     * Operation code of RASP instruction.
     */
    public final static int STORE = 4;
    /**
     * Operation code of RASP instruction.
     */
    public final static int ADD = 5;
    /**
     * Operation code of RASP instruction.
     */
    public final static int SUB = 6;
    /**
     * Operation code of RASP instruction.
     */
    public final static int MUL = 7;
    /**
     * Operation code of RASP instruction.
     */
    public final static int DIV = 8;
    /**
     * Operation code of RASP instruction.
     */
    public final static int JMP = 9;
    /**
     * Operation code of RASP instruction.
     */
    public final static int JZ = 10;
    /**
     * Operation code of RASP instruction.
     */
    public final static int JGTZ = 11;
    /**
     * Operation code of RASP instruction.
     */
    public final static int HALT = 12;

    /**
     * Get operation code of the instruction.
     *
     * @return operation code of the instruction.
     */
    public int getCode();

    /**
     * Get operand type of the instruction; either constant or register.
     *
     * @return operand type of the instruction; either constant or register
     */
    public OperandType getOperandType();

    /**
     * Get string representation of the RASP instruction (mnemonic code).
     *
     * @return string representation of the instruction
     */
    public String getCodeStr();

}
