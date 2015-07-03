package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterPair_PSW_Verifier implements Consumer<RunnerContext<Integer>> {
    private final Function<RunnerContext<Integer>, Integer> operation;
    private final CpuVerifier verifier;
    private final int registerPair;

    public RegisterPair_PSW_Verifier(CpuVerifier verifier, Function<RunnerContext<Integer>, Integer> operation, int registerPair) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.registerPair = registerPair;
    }

    @Override
    public void accept(RunnerContext<Integer> context) {
        verifier.checkRegisterPairPSW(registerPair, operation.apply(context));
    }
}