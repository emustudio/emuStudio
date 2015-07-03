package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Test<T> {
    private final List<Consumer<RunnerContext<T>>> verifiers = new ArrayList<>();

    public Test(List<Consumer<RunnerContext<T>>> verifiers) {
        this.verifiers.addAll(new ArrayList<>(Objects.requireNonNull(verifiers)));
    }

    private void verify(RunnerContext<T> context) {
        for (Consumer<RunnerContext<T>> verifier : verifiers) {
            verifier.accept(context);
        }
    }

    private Unary pCreate(Function<T, RunnerContext<T>> runner) {
        return new Unary(runner, verifiers);
    }

    public static <T> Test<T>.Unary create(Function<T, RunnerContext<T>> runner,
                                           List<Consumer<RunnerContext<T>>> verifiers) {
        return new Test<>(verifiers).pCreate(runner);
    }

    private Binary pCreate(BiFunction<T, T, RunnerContext<T>> runner) {
        return new Binary(runner, verifiers);
    }

    public static <T> Test<T>.Binary create(BiFunction<T, T, RunnerContext<T>> runner,
                                            List<Consumer<RunnerContext<T>>> verifiers) {
        return new Test<>(verifiers).pCreate(runner);
    }

    public class Unary extends Test<T> implements Consumer<T> {
        private final Function<T, RunnerContext<T>> runner;

        private Unary(Function<T, RunnerContext<T>> runner, List<Consumer<RunnerContext<T>>> verifiers) {
            super(verifiers);
            this.runner = Objects.requireNonNull(runner);
        }

        @Override
        public void accept(T first) {
            verify(runner.apply(first));
        }
    }

    public class Binary extends Test<T> implements BinaryConsumer<T> {
        private final BiFunction<T, T, RunnerContext<T>> runner;

        private Binary(BiFunction<T, T, RunnerContext<T>> runner, List<Consumer<RunnerContext<T>>> verifiers) {
            super(verifiers);
            this.runner = Objects.requireNonNull(runner);
        }

        @Override
        public void accept(T first, T second) {
            verify(runner.apply(first, second));
        }
    }


}
