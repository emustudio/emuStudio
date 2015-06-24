package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.function.Consumer;
import java.util.function.Function;

public class MemoryAndFlagsVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final FlagsBuilder flagsBuilder;
    private final CpuVerifier verifier;
    private final int address;

    public MemoryAndFlagsVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation,
                                    FlagsBuilder flagsBuilder, int address) {
        this.operation = operation;
        this.flagsBuilder = flagsBuilder;
        this.verifier = verifier;
        this.address = address;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        int expectedResult = operation.apply(context);

        flagsBuilder.reset();
        flagsBuilder.eval(context.first, context.second, expectedResult);

        verifier.checkMemoryByte(address, expectedResult);
        verifier.checkFlags(flagsBuilder.getExpectedFlags());
        verifier.checkNotFlags(flagsBuilder.getNotExpectedFlags());
    }
}
