package net.sf.emustudio.zilogZ80.impl.suite.verifiers;

import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.CpuVerifierImpl;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class IX_Verifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifierImpl verifier;

    public IX_Verifier(CpuVerifierImpl verifier, Function<RunnerContext<T>, Integer> operation) {
        this.verifier = Objects.requireNonNull(verifier);
        this.operation = Objects.requireNonNull(operation);
    }

    @Override
    public void accept(RunnerContext<T> context) {
        verifier.checkIX(operation.apply(context));
    }
}
