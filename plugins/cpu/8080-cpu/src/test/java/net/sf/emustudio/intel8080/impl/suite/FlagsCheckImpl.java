package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.cpu.testsuite.FlagsCheck;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_AC;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;

public class FlagsCheckImpl<T extends Number> extends FlagsCheck<T, FlagsCheckImpl<T>> {

    public FlagsCheckImpl sign() {
        evaluators.add((context, result) -> {
            if (result.byteValue() < 0) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsCheckImpl zero() {
        evaluators.add((context, result) -> {
            if (result.byteValue() == 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsCheckImpl parity() {
        evaluators.add((context, result) -> {
            int numberOf1 = 0;
            int intResult = result.intValue() & 0xFF;

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

    public FlagsCheckImpl carry15() {
        evaluators.add((context, result) -> {
            if ((result.intValue() & 0x10000) == 0x10000) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl carry() {
        evaluators.add((context, result) -> {
            if ((result.intValue() & 0x100) == 0x100) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl carryIsFirstOperandMSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 0x80) == 0x80) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl carryIsFirstOperandLSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 1) == 1) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl carryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= FLAG_C);
        return this;
    }

    public static boolean isAuxCarry(int first, int sumWith) {
        int mask = sumWith & first;
        int xormask = sumWith ^ first;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;

        return (C3 != 0);
    }

    public FlagsCheckImpl auxCarry() {
        evaluators.add((context, result) -> {
            int firstInt = context.first.intValue();
            byte diff = (byte)((result.intValue() - firstInt) & 0xFF);

            if (isAuxCarry(context.first.intValue(), diff)) {
                expectedFlags |= FLAG_AC;
            } else {
                expectedNotFlags |= FLAG_AC;
            }
        });
        return this;
    }

    public FlagsCheckImpl auxCarryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= FLAG_AC);
        return this;
    }

}
