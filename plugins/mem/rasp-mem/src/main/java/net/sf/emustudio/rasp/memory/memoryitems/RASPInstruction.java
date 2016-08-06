package net.sf.emustudio.rasp.memory.memoryitems;

/**
 * Interface representing RASP instruction.
 *
 * @author miso
 */
public interface RASPInstruction extends MemoryItem {
    
    /**
     * Operation code of RASP instruction.
     */
    public final static int READ = 1;
    /**
     * Operation code of RASP instruction.
     */
    public final static int WRITE_CONSTANT = 2;
    /**
     * Operation code of RASP instruction.
     */
    public final static int WRITE_REGISTER = 3;
    /**
     * Operation code of RASP instruction.
     */
    public final static int LOAD_CONSTANT = 4;
    /**
     * Operation code of RASP instruction.
     */
    public final static int LOAD_REGISTER = 5;
    /**
     * Operation code of RASP instruction.
     */
    public final static int STORE = 6;
    /**
     * Operation code of RASP instruction.
     */
    public final static int ADD_CONSTANT = 7;
    /**
     * Operation code of RASP instruction.
     */
    public final static int ADD_REGISTER = 8;
    /**
     * Operation code of RASP instruction.
     */
    public final static int SUB_CONSTANT = 9;
    /**
     * Operation code of RASP instruction.
     */
    public final static int SUB_REGISTER = 10;
    /**
     * Operation code of RASP instruction.
     */
    public final static int MUL_CONSTANT = 11;
    /**
     * Operation code of RASP instruction.
     */
    public final static int MUL_REGISTER = 12;
    /**
     * Operation code of RASP instruction.
     */
    public final static int DIV_CONSTANT = 13;
    /**
     * Operation code of RASP instruction.
     */
    public static final int DIV_REGISTER = 14;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JMP = 15;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JZ = 16;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JGTZ = 17;
    /**
     * Operation code of RASP instruction.
     */
    public static final int HALT = 18;

    /**
     * Get operation code of the instruction.
     *
     * @return operation code of the instruction.
     */
    public int getCode();

    /**
     * Get string representation of the RASP instruction (mnemonic code).
     *
     * @return string representation of the instruction
     */
    public String getCodeStr();

}
