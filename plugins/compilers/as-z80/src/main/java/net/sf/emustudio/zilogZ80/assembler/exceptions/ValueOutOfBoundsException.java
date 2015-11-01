package net.sf.emustudio.zilogZ80.assembler.exceptions;

public class ValueOutOfBoundsException extends CompilerException {

    public ValueOutOfBoundsException(int column, int line, int minValue, int maxValue, int currentValue) {
        super(column, line, "Value (" + currentValue + ") is out of bounds (minimum is "
                + minValue + ", maximum is " + maxValue + ")");
    }
}
