package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.CpuVerifierImpl;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifierImpl verifier;
    private final int register;

    public RegisterVerifier(CpuVerifierImpl verifier, Function<RunnerContext<T>, Integer> operation, int register) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.register = register;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        verifier.checkRegister(register, operation.apply(context));
    }
}
