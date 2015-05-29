package net.sf.emustudio.intel8080.impl.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

public class Arithmetic {

    public static class BinaryOperation {
        public final int arg1;
        public final int arg2;
        public final int result;

        public BinaryOperation(int arg1, int arg2, int result) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.result = result;
        }
    }

    /**
     * Generates 8-bit overflow for binary add or sub.
     *
     * Signed byte is from -128 .. 127.
     *
     * group 1
     * -128 - (1..127) = (-129..-255) = overflow
     * -127 - (2..127) = (-129..-254) = overflow
     * -126 - (3..127) = (-129..-253) = overflow
     * ...
     * -2   - (127)    = (-129)       = overflow
     *
     * group 2
     * 127  + (1..127) = (128..254)   = overflow
     * 126  + (2..127) = (128..253)   = overflow
     * ...
     * 1    + 127      = 128          = overflow
     *
     * @param operator binary operator, must be either + or -
     * @return list of binary operations with applied operator
     */
    public static List<BinaryOperation> generate8bitOverflow(BinaryOperator<Integer> operator) {
        List<BinaryOperation> operations = new ArrayList<>();

        for (int i = 0; i < 127; i++) {
            int count = 127 - i;
            for (int j = 0; j < count; j++) {
                int arg1 = (byte)(-128 + i);
                int arg2 = (byte)(1 + j);
                int result = operator.apply(arg1, arg2) & 0xFF;
                operations.add(new BinaryOperation(arg1, arg2, result));

                arg1 = (byte)(127 - i);
                arg2 = (byte)(1 + j);
                result = operator.apply(arg1, arg2) & 0xFF;
                operations.add(new BinaryOperation(arg1, arg2, result));
            }
        }
        return operations;
    }



}
