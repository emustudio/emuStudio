package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class MemoryByteVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifier verifier;
    private final Function<RunnerContext<T>, Integer> address;

    public MemoryByteVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation,
                              Function<RunnerContext<T>, Integer> addressOperator) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.address = addressOperator;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        verifier.checkMemoryByte(address.apply(context), operation.apply(context));
    }
}
