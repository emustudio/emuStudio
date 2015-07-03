package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class PCVerifier implements Consumer<RunnerContext<Integer>> {
    private final Function<RunnerContext<Integer>, Integer> operation;
    private final CpuVerifier verifier;

    public PCVerifier(CpuVerifier verifier, Function<RunnerContext<Integer>, Integer> operation) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
    }

    @Override
    public void accept(RunnerContext<Integer> context) {
        verifier.checkPC(operation.apply(context));
    }
}
