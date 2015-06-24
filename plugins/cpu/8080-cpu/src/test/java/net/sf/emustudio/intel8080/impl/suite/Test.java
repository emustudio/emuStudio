package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Test<T> {
    private final Consumer<RunnerContext<T>> verifier;

    public Test(Consumer<RunnerContext<T>> verifier) {
        this.verifier = Objects.requireNonNull(verifier);
    }

    private void verify(RunnerContext<T> context) {
        verifier.accept(context);
    }

    private Unary pCreate(Function<T, RunnerContext<T>> runner, Consumer<RunnerContext<T>> verifier) {
        return new Unary(runner, verifier);
    }

    public static <T> Test<T>.Unary create(Function<T, RunnerContext<T>> runner,
                                           Consumer<RunnerContext<T>> verifier) {
        return new Test<>(verifier).pCreate(runner, verifier);
    }

    private Binary pCreate(BiFunction<T, T, RunnerContext<T>> runner, Consumer<RunnerContext<T>> verifier) {
        return new Binary(runner, verifier);
    }

    public static <T> Test<T>.Binary create(BiFunction<T, T, RunnerContext<T>> runner,
                                            Consumer<RunnerContext<T>> verifier) {
        return new Test<>(verifier).pCreate(runner, verifier);
    }

    public class Unary extends Test<T> implements Consumer<T> {
        private final Function<T, RunnerContext<T>> runner;

        private Unary(Function<T, RunnerContext<T>> runner, Consumer<RunnerContext<T>> verifier) {
            super(verifier);
            this.runner = Objects.requireNonNull(runner);
        }

        @Override
        public void accept(T first) {
            verify(runner.apply(first));
        }
    }

    public class Binary extends Test<T> implements BinaryConsumer<T> {
        private final BiFunction<T, T, RunnerContext<T>> runner;

        private Binary(BiFunction<T, T, RunnerContext<T>> runner, Consumer<RunnerContext<T>> verifier) {
            super(verifier);
            this.runner = Objects.requireNonNull(runner);
        }

        @Override
        public void accept(T first, T second) {
            verify(runner.apply(first, second));
        }
    }


}
