package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.FlagsBuilder;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_N;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;

public class FlagsBuilderImpl<T extends Number> extends FlagsBuilder<T, FlagsBuilderImpl> {

    public FlagsBuilderImpl sign() {
        evaluators.add((first, second, result) -> {
            if ((byte)result < 0) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsBuilderImpl sign16bit() {
        evaluators.add((first, second, result) -> {
            if ((result & 0x8000) != 0) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsBuilderImpl zero() {
        evaluators.add((first, second, result) -> {
            if ((byte)result == 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsBuilderImpl zero16bit() {
        evaluators.add((first, second, result) -> {
            if ((result & 0xFFFF)== 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsBuilderImpl subtractionIsReset() {
        evaluators.add((first, second, result) -> {
            expectedNotFlags |= FLAG_N;
        });
        return this;
    }

    public FlagsBuilderImpl subtractionIsSet() {
        evaluators.add((first, second, result) -> {
            expectedFlags |= FLAG_N;
        });
        return this;
    }

    public FlagsBuilderImpl overflow() {
        evaluators.add((first, second, result) -> {
            int sign = first.intValue() & 0x80;
            int trueSecond = result - first.intValue();

            if (sign != (trueSecond & 0x80)) {
                expectedNotFlags |= FLAG_PV;
            } else if ((result & 0x80) != sign){
                expectedFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsBuilderImpl overflow16bit() {
        evaluators.add((first, second, result) -> {
            int sign = first.intValue() & 0x8000;
            int trueSecond = result - first.intValue();

            if (sign != (trueSecond & 0x8000)) {
                expectedNotFlags |= FLAG_PV;
            } else if ((result & 0x8000) != sign){
                expectedFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsBuilderImpl parity() {
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
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsBuilderImpl carry15() {
        evaluators.add((first, second, result) -> {
            if ((result & 0x10000) == 0x10000) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilderImpl carry() {
        evaluators.add((first, second, result) -> {
            if ((result & 0x100) == 0x100) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilderImpl carryIsFirstOperandMSB() {
        evaluators.add((first, second, result) -> {
            if ((first.intValue() & 0x80) == 0x80) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilderImpl carryIsFirstOperandLSB() {
        evaluators.add((first, second, result) -> {
            if ((((Number)first).intValue() & 1) == 1) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsBuilderImpl carryIsReset() {
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

    public FlagsBuilderImpl halfCarry() {
        evaluators.add((first, second, result) -> {
            int firstInt = first.intValue();
            byte diff = (byte)((result - firstInt) & 0xFF);

            if (isAuxCarry(first.intValue(), diff)) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsBuilderImpl halfCarry11() {
        evaluators.add((first, second, result) -> {
            second = (result - first.intValue()) & 0xFFFF;

            int mask = second.intValue() & first.intValue();
            int xormask = second.intValue() ^ first.intValue();

            int C0 = mask&1;
            int C1 = ((mask>>>1) ^ (C0&(xormask>>>1)))&1;
            int C2 = ((mask>>>2) ^ (C1&(xormask>>>2)))&1;
            int C3 = ((mask>>>3) ^ (C2&(xormask>>>3)))&1;
            int C4 = ((mask>>>4) ^ (C3&(xormask>>>4)))&1;
            int C5 = ((mask>>>5) ^ (C4&(xormask>>>5)))&1;
            int C6 = ((mask>>>6) ^ (C5&(xormask>>>6)))&1;
            int C7 = ((mask>>>7) ^ (C6&(xormask>>>7)))&1;
            int C8 = ((mask>>>8) ^ (C7&(xormask>>>8)))&1;
            int C9 = ((mask>>>9) ^ (C8&(xormask>>>9)))&1;
            int C10 = ((mask>>>10) ^ (C9&(xormask>>>10)))&1;
            int C11 = ((mask>>>11) ^ (C10&(xormask>>>11)))&1;

            if (C11 != 0) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsBuilderImpl halfCarryIsReset() {
        evaluators.add((first, second, result) -> expectedNotFlags |= FLAG_H);
        return this;
    }

    public FlagsBuilderImpl halfCarryDAA() {
        evaluators.add((first, second, result) -> {
            int firstInt = ((Number)first).intValue();
            int diff = (result - firstInt) & 0x0F;

            if ((diff == 6) && isAuxCarry(firstInt, 6)) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

}
