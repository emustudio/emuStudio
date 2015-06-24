package net.sf.emustudio.intel8080.impl.suite.runners;

import java.util.function.Function;

public class RunnerContext<T> {
    public final T first;
    public final T second;
    public final int result;

    public final int flagsBefore;

    public RunnerContext(T first, T second, int result, int flagsBefore) {
        this.first = first;
        this.second = second;
        this.result = result;
        this.flagsBefore = flagsBefore;
    }

    public static <T> Function<RunnerContext<T>, RunnerContext<T>> transformFirst(
            Function<RunnerContext<T>, T> modifier) {
        return (context) -> new RunnerContext<>(
                modifier.apply(context), context.second, context.result, context.flagsBefore
        );
    }

    public static <T> Function<RunnerContext<T>, RunnerContext<T>> transformSecond(
            Function<RunnerContext<T>, T> modifier) {
        return (context) -> new RunnerContext<>(
                context.first, modifier.apply(context), context.result, context.flagsBefore
        );
    }

    public static <T> Function<RunnerContext<T>, RunnerContext<T>> transformResult(
            Function<RunnerContext<T>, Integer> modifier) {
        return (context) -> new RunnerContext<>(
                context.first, context.second, modifier.apply(context), context.flagsBefore
        );
    }

}
