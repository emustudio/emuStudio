package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.cpu.testsuite.CpuVerifier;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.CpuVerifierImpl;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class PCVerifier implements Consumer<RunnerContext<Integer>> {
    private final Function<RunnerContext<Integer>, Integer> operation;
    private final CpuVerifierImpl verifier;

    public PCVerifier(CpuVerifierImpl verifier, Function<RunnerContext<Integer>, Integer> operation) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
    }

    @Override
    public void accept(RunnerContext<Integer> context) {
        verifier.checkPC(operation.apply(context));
    }
}
