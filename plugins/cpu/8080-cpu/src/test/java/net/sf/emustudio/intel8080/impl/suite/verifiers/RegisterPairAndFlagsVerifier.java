package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterPairAndFlagsVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifier verifier;
    private final int registerPair;
    private final FlagsBuilder<T> flagsBuilder;

    public RegisterPairAndFlagsVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation, int registerPair,
                                        FlagsBuilder<T> flagsBuilder) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.registerPair = registerPair;
        this.flagsBuilder = Objects.requireNonNull(flagsBuilder);
    }

    @Override
    public void accept(RunnerContext<T> context) {
        int expectedResult = operation.apply(context);

        flagsBuilder.reset();
        flagsBuilder.eval(context.first, context.second, expectedResult);

        verifier.checkRegisterPair(registerPair, expectedResult);
        verifier.checkFlags(flagsBuilder.getExpectedFlags());
        verifier.checkNotFlags(flagsBuilder.getNotExpectedFlags());
    }
}