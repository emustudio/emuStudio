package net.sf.emustudio.cpu.testsuite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public abstract class FlagsBuilder<T extends Number, SpecificFlagsBuilder extends FlagsBuilder> {
    protected final List<FlagsEval> evaluators = new ArrayList<>();

    private boolean switchFirstAndSecond;
    protected int expectedFlags = 0;
    protected int expectedNotFlags = 0;

    @FunctionalInterface
    protected interface FlagsEval<T extends Number> {
        void eval(T first, T second, int result);
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

    public SpecificFlagsBuilder expectFlagOnlyWhen(int flag, BiFunction<Number, Number, Boolean> predicate) {
        evaluators.add(((first, second, result) -> {
            if (predicate.apply(first, second)) {
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

    public void eval(T first, T second, int result) {
        for (FlagsEval evaluator : evaluators) {
            if (switchFirstAndSecond) {
                evaluator.eval(second, first, result);
            } else {
                evaluator.eval(first, second, result);
            }
        }
    }

}
