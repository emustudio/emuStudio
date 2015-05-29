package net.sf.emustudio.intel8080.impl.verifiers;

import net.sf.emustudio.intel8080.impl.generators.Arithmetic;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class FlagsVerifier {

    public static void verify8BitOverflow(BinaryOperator<Integer> operator,
                                          Consumer<List<Arithmetic.BinaryOperation>> programGenerator) {
        List<Arithmetic.BinaryOperation> operations = Arithmetic.generate8bitOverflow(operator);
        programGenerator.accept(operations);

        for (Arithmetic.BinaryOperation operation : operations) {

        }

    }
}
