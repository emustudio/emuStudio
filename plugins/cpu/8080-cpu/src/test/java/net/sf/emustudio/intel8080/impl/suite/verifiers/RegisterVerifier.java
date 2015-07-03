package net.sf.emustudio.intel8080.impl.suite.verifiers;

import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegisterVerifier<T extends Number> implements Consumer<RunnerContext<T>> {
    private final Function<RunnerContext<T>, Integer> operation;
    private final CpuVerifier verifier;
    private final int register;

    public RegisterVerifier(CpuVerifier verifier, Function<RunnerContext<T>, Integer> operation, int register) {
        this.operation = Objects.requireNonNull(operation);
        this.verifier = Objects.requireNonNull(verifier);
        this.register = register;
    }

    @Override
    public void accept(RunnerContext<T> context) {
        verifier.checkRegister(register, operation.apply(context));
    }
}
