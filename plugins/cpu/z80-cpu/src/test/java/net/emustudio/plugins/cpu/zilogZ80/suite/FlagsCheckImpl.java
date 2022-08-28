/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.FlagsCheck;
import net.emustudio.cpu.testsuite.RunnerContext;

import java.util.Objects;
import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

public class FlagsCheckImpl<T extends Number> extends FlagsCheck<T, FlagsCheckImpl<T>> {
    private Function<RunnerContext<T>, Integer> first = context -> context.first.intValue();
    private Function<RunnerContext<T>, Integer> second = context -> context.second.intValue();

    public FlagsCheckImpl<T> setFirst(Function<RunnerContext<T>, Integer> first) {
        this.first = Objects.requireNonNull(first);
        return this;
    }

    public FlagsCheckImpl<T> setSecond(Function<RunnerContext<T>, Integer> second) {
        this.second = Objects.requireNonNull(second);
        return this;
    }

    public FlagsCheckImpl<T> setFirst8MSB() {
        this.first = context -> context.first.intValue() >>> 8;
        return this;
    }

    public FlagsCheckImpl<T> setFirst8LSB() {
        this.first = context -> context.first.intValue() & 0xFF;
        return this;
    }

    public FlagsCheckImpl<T> setSecond8MSB() {
        this.second = context -> context.second.intValue() >>> 8;
        return this;
    }

    public FlagsCheckImpl<T> setSecond8LSB() {
        this.second = context -> context.second.intValue() & 0xFF;
        return this;
    }

    public FlagsCheckImpl<T> exact(int flags) {
        evaluators.add((context, result) -> {
            expectedFlags = flags;
            expectedNotFlags = flags ^ 0xFF;
        });
        return this;
    }

    public FlagsCheckImpl<T> sign() {
        evaluators.add((context, result) -> {
            if ((result & 0x80) == 0x80) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> sign16bit() {
        evaluators.add((context, result) -> {
            if ((result & 0x8000) == 0x8000) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> zero() {
        evaluators.add((context, result) -> {
            if (result.byteValue() == 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> zero16bit() {
        evaluators.add((context, result) -> {
            if ((result & 0xFFFF) == 0) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> subtractionIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= FLAG_N);
        return this;
    }

    public FlagsCheckImpl<T> subtractionIsSet() {
        evaluators.add((context, result) -> expectedFlags |= FLAG_N);
        return this;
    }

    public FlagsCheckImpl<T> overflow() {
        evaluators.add((context, result) -> {
            int firstInt = first.apply(context) & 0xFF;
            int secondInt = second.apply(context) & 0xFF;

            int carryIns = ((firstInt ^ secondInt) ^ 0x80) & 0x80;
            if (carryIns != 0) { // if addend signs are the same
                // overflow if the sum sign differs from the sign of either of addends
                carryIns = ((result ^ firstInt) & 0x80);
            }
            if (carryIns != 0) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> overflowSub() {
        evaluators.add((context, result) -> {
            int fst = first.apply(context) & 0xFF;
            int snd = second.apply(context) & 0xFF;

            int sum = (fst - snd) & 0x1FF;
            int flagC = (sum & 0x100) == 0x100 ? FLAG_C : 0;
            int carryIns = (result & 0xFF) ^ fst ^ snd;
            carryIns = (carryIns >>> 7) ^ flagC;
            int flagP = (carryIns == 0) ? 0 : FLAG_PV;

            if (flagP == FLAG_PV) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> overflowSubCarry() {
        evaluators.add((context, result) -> {
            int firstInt = first.apply(context) & 0xFF;
            int inversedSecond = (~second.apply(context) & 0xFF);

            int carryIns = ((firstInt ^ inversedSecond) ^ 0x80) & 0x80;
            if (carryIns != 0) { // if addend signs are the same
                // overflow if the sum sign differs from the sign of either of addends
                carryIns = ((result ^ firstInt) & 0x80);
            }
            if (carryIns != 0) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> overflow16bit() {
        evaluators.add((context, result) -> {
            int fst = context.first.intValue();
            int snd = context.second.intValue();
            int ov = (((snd ^ fst ^ 0x8000) & (snd ^ result) & 0x8000) >>> 13);
            if (ov != 0) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> borrow16bit() {
        evaluators.add((context, result) -> {
            int fst = context.first.intValue();
            int snd = context.second.intValue();
            int ov = (((snd ^ fst) & (snd ^ result) & 0x8000) >>> 13);
            if (ov != 0) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> parity() {
        evaluators.add((context, result) -> {
            if (isParity(result & 0xFF)) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> parityIsPreserved() {
        evaluators.add((context, result) -> {
            if ((context.flags & FLAG_PV) == FLAG_PV) {
                expectedFlags |= FLAG_PV;
            } else {
                expectedNotFlags |= FLAG_PV;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carry15() {
        evaluators.add((context, result) -> {
            if ((result & 0x10000) == 0x10000) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carry() {
        evaluators.add((context, result) -> {
            if ((result & 0x100) == 0x100) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> borrow() {
        evaluators.add((context, result) -> {
            int fst = first.apply(context) & 0xFF;
            int snd = second.apply(context) & 0xFF;

            int r = (fst - snd) & 0x1FF;
            int flagC = (r & 0x100) == 0x100 ? FLAG_C : 0;

            if (flagC != 0) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> borrowWithCarry() {
        evaluators.add((context, result) -> {
            int fst = first.apply(context) & 0xFF;
            int snd = second.apply(context) & 0xFF;

            int r = (fst - snd - (context.flags & FLAG_C)) & 0x1FF;
            int flagC = (r & 0x100) == 0x100 ? FLAG_C : 0;
            if (flagC != 0) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandMSB() {
        evaluators.add((context, result) -> {
            if ((first.apply(context) & 0x80) == 0x80) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandLSB() {
        evaluators.add((context, result) -> {
            if ((first.apply(context) & 1) == 1) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= FLAG_C);
        return this;
    }

    public FlagsCheckImpl<T> carryIsPreserved() {
        evaluators.add((context, result) -> {
            if ((context.flags & FLAG_C) == FLAG_C) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> halfCarry() {
        evaluators.add((context, result) -> {
            int carryIns = result ^ (first.apply(context) & 0xFF) ^ (second.apply(context) & 0xFF);
            if ((carryIns & FLAG_H) == FLAG_H) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> halfBorrow() {
        return halfCarry(); // it's really the same for sub
    }

    public FlagsCheckImpl<T> halfCarry11() {
        evaluators.add((context, result) -> {
            int fst = first.apply(context);
            int snd = second.apply(context);
            int hc = ((result ^ fst ^ snd) >>> 8) & FLAG_H;

            if (hc != 0) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> halfCarryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= FLAG_H);
        return this;
    }

    public FlagsCheckImpl<T> halfCarryIsSet() {
        evaluators.add((context, result) -> expectedFlags |= FLAG_H);
        return this;
    }

    public FlagsCheckImpl<T> zeroIsSet() {
        evaluators.add((context, result) -> expectedFlags |= FLAG_Z);
        return this;
    }

    public FlagsCheckImpl<T> zeroIsPreserved() {
        evaluators.add((context, result) -> {
            if ((context.flags & FLAG_Z) == FLAG_Z) {
                expectedFlags |= FLAG_Z;
            } else {
                expectedNotFlags |= FLAG_Z;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> signIsPreserved() {
        evaluators.add((context, result) -> {
            if ((context.flags & FLAG_S) == FLAG_S) {
                expectedFlags |= FLAG_S;
            } else {
                expectedNotFlags |= FLAG_S;
            }
        });
        return this;
    }

    public static boolean isParity(int value) {
        int numberOfOnes = 0;

        for (int i = 0; i < 8; i++) {
            if ((value & 1) == 1) {
                numberOfOnes++;
            }
            value = value >>> 1;
        }
        return numberOfOnes % 2 == 0;
    }
}
