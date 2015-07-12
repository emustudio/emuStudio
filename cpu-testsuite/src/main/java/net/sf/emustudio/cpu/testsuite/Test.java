package net.sf.emustudio.cpu.testsuite;

import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Test<T extends Number> implements BiConsumer<T, T> {
    private final List<Consumer<RunnerContext<T>>> verifiers = new ArrayList<>();
    private final BiFunction<T, T, RunnerContext<T>> runner;

    public Test(BiFunction<T, T, RunnerContext<T>> runner, List<Consumer<RunnerContext<T>>> verifiers) {
        this.runner = Objects.requireNonNull(runner);
        this.verifiers.addAll(new ArrayList<>(Objects.requireNonNull(verifiers)));
    }

    private void verify(RunnerContext<T> context) {
        for (Consumer<RunnerContext<T>> verifier : verifiers) {
            verifier.accept(context);
        }
    }

    @Override
    public void accept(T first, T second) {
        verify(runner.apply(first, second));
    }

}
