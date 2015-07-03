package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterPair_SP_Verifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifier verifier;
    private final int registerPair;

    public RegisterPair_SP_Verifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation, int registerPair) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.registerPair = registerPair;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        verifier.checkRegisterPair(registerPair, operation.apply(context));
    }
}