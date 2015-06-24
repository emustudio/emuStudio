package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterAndFlagsVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final FlagsBuilder flagsBuilder;
    private final CpuVerifier verifier;
    private final int register;
    private Function<RunnerContext<T>, Integer> resultOp;

    public RegisterAndFlagsVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation,
                                       FlagsBuilder flagsBuilder, int register) {
        this.operation = Objects.requireNonNull(operation);
        this.flagsBuilder = Objects.requireNonNull(flagsBuilder);
        this.verifier = Objects.requireNonNull(verifier);
        this.register = register;
    }

    public void setFlagResultOp(Function<RunnerContext<T>, Integer> resultOp) {
        this.resultOp = resultOp;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        int expectedResult = operation.apply(context);
        int flagsResult = expectedResult;

        if (resultOp != null) {
            flagsResult = resultOp.apply(context);
        }

        flagsBuilder.reset();
        flagsBuilder.eval(context.first, context.second, flagsResult);

        verifier.checkRegister(register, expectedResult);
        verifier.checkFlags(flagsBuilder.getExpectedFlags());
        verifier.checkNotFlags(flagsBuilder.getNotExpectedFlags());
    }
}
