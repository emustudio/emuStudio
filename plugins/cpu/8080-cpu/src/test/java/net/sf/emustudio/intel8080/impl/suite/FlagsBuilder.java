package net.sf.emustudio.intel8080.impl.suite;

import java.util.ArrayList;
import java.util.List;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_AC;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;

public class FlagsBuilder<T extends Number> {
    private final List<FlagsEval> evaluators = new ArrayList<>();

    int expectedFlags = 0;
    int expectedNotFlags = 0;

    @FunctionalInterface
    private interface FlagsEval<T> {
        void eval(T first, T second, int result);
    }

    public FlagsBuilder reset() {
        expectedFlags = 0;
        expectedNotFlags = 0;
        return this;
    }

    public FlagsBuilder sign() {
        evaluators.add((first, second, result) -> {
            if ((byte)result < 0) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsBuilder zero() {
        evaluators.add((first, second, result) -> {
            if ((byte)result == 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsBuilder parity() {
        evaluators.add((first, second, result) -> {
            int numberOf1 = 0;
            int intResult = result & 0xFF;

            for (int i = 0; i < 8; i++) {
                if ((intResult & 1) == 1) {
                    numberOf1++;
                }
                intResult = intResult >>> 1;
            }

            if (numberOf1 % 2 == 0) {
                expectedFlags |= FLAG_P;
            } else {
                expectedNotFlags |= FLAG_P;
            }
        });
        return this;
    }

    public FlagsBuilder carry15() {
        evaluators.add((first, second, result) -> {
            if ((result & 0x10000) == 0x10000) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilder carry() {
        evaluators.add((first, second, result) -> {
            if ((result & 0x100) == 0x100) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilder carryIsFirstOperandMSB() {
        evaluators.add((first, second, result) -> {
            if ((((Number)first).intValue() & 0x80) == 0x80) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilder carryIsFirstOperandLSB() {
        evaluators.add((first, second, result) -> {
            if ((((Number)first).intValue() & 1) == 1) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilder carryIsReset() {
        evaluators.add((first, second, result) -> expectedNotFlags |= FLAG_C);
        return this;
    }

    private boolean isAuxCarry(int first, int sumWith) {
        int mask = sumWith & first;
        int xormask = sumWith ^ first;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;

        return (C3 != 0);
    }

    public FlagsBuilder auxCarry() {
        evaluators.add((first, second, result) -> {
            if (isAuxCarry(((Number) first).intValue(), ((Number)second).intValue())) {
                expectedFlags |= FLAG_AC;
            } else {
                expectedNotFlags |= FLAG_AC;
            }
        });
        return this;
    }

    public FlagsBuilder auxCarryDAA() {
        evaluators.add((first, second, result) -> {
            int firstInt = ((Number)first).intValue();
            int diff = (result - firstInt) & 0x0F;

            if ((diff == 6) && isAuxCarry(firstInt, 6)) {
                expectedFlags |= FLAG_AC;
            } else {
                expectedNotFlags |= FLAG_AC;
            }
        });
        return this;
    }

    public FlagsBuilder or(int flags) {
        expectedFlags |= flags;
        return this;
    }

    public int getExpectedFlags() {
        return expectedFlags;
    }

    public int getNotExpectedFlags() {
        return expectedNotFlags;
    }

    public void eval(T first, T second, int result) {
        for (FlagsEval evaluator : evaluators) {
            evaluator.eval(first, second, result);
        }
    }

}
