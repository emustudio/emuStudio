package net.sf.emustudio.cpu.testsuite;

import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public abstract class FlagsBuilder<T extends Number, SpecificFlagsBuilder extends FlagsBuilder<T, ?>> {
    protected final List<FlagsEval> evaluators = new ArrayList<>();

    private boolean switchFirstAndSecond;
    protected int expectedFlags = 0;
    protected int expectedNotFlags = 0;

    @FunctionalInterface
    protected interface FlagsEval<T extends Number> {
        void eval(RunnerContext<T> context, int result);
    }

    public SpecificFlagsBuilder reset() {
        expectedFlags = 0;
        expectedNotFlags = 0;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder or(int flags) {
        expectedFlags |= flags;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder switchFirstAndSecond() {
        switchFirstAndSecond = !switchFirstAndSecond;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder expectFlagOnlyWhen(int flag, BiFunction<RunnerContext<T>, Number, Boolean> predicate) {
        evaluators.add(((context, result) -> {
            if (predicate.apply(context, result)) {
                expectedFlags |= flag;
            } else {
                expectedNotFlags |= flag;
            }
        }));
        return (SpecificFlagsBuilder)this;
    }

    public int getExpectedFlags() {
        return expectedFlags;
    }

    public int getNotExpectedFlags() {
        return expectedNotFlags;
    }

    public void eval(RunnerContext<T> context, int result) {
        for (FlagsEval evaluator : evaluators) {
            if (switchFirstAndSecond) {
                evaluator.eval(context.switchFirstAndSecond(), result);
            } else {
                evaluator.eval(context, result);
            }
        }
    }

}
