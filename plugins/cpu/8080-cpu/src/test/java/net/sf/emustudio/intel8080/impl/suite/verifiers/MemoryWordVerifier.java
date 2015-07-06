package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class MemoryWordVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final Function<RunnerContext<T>, Integer> addressOperation;
    private final CpuVerifier verifier;

    public MemoryWordVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation,
                              Function<RunnerContext<T>, Integer> addressOperation) {
        this.operation = Objects.requireNonNull(operation);
        this.addressOperation = Objects.requireNonNull(addressOperation);
        this.verifier = Objects.requireNonNull(verifier);
    }

    @Override
    public void accept(RunnerContext<T> context) {
        int expectedResult = operation.apply(context);
        int address = addressOperation.apply(context);

        verifier.checkMemoryWord(address, expectedResult);
    }
}
