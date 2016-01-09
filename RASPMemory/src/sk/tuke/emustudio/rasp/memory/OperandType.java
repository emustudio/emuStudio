package sk.tuke.emustudio.rasp.memory;

/**
 * Type of the operand of RASP instruction, constant or register.
 *
 * @author miso
 */
public enum OperandType {

    /**
     * constant as operand; e.g. ADD =2
     */
    CONSTANT,
    /**
     * register as operand; e.g. ADD 2
     */
    REGISTER
}
