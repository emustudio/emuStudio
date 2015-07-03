package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class FlagsVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final FlagsBuilder flagsBuilder;
    private final CpuVerifier verifier;

    public FlagsVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation, FlagsBuilder flagsBuilder) {
        this.operation = Objects.requireNonNull(operation);
        this.flagsBuilder = Objects.requireNonNull(flagsBuilder);
        this.verifier = Objects.requireNonNull(verifier);
    }

    @Override
    public void accept(RunnerContext<T> context) {
        flagsBuilder.reset();
        flagsBuilder.eval(context.first, context.second, operation.apply(context));

        verifier.checkFlags(flagsBuilder.getExpectedFlags());
        verifier.checkNotFlags(flagsBuilder.getNotExpectedFlags());
    }
}
