/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
            int firstInt = first.apply(context) & 0xFF;
            int secondInt = ((~(second.apply(context) & 0xFF)) + 1) & 0xFF;

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

    public FlagsCheckImpl<T> overflow16bit() {
        evaluators.add((context, result) -> {
            int sign = context.first.intValue() & 0x8000;
            int trueSecond = result - context.first.intValue();

            if (sign != (trueSecond & 0x8000)) {
                expectedNotFlags |= FLAG_PV;
            } else if ((result & 0x8000) != sign) {
                expectedFlags |= FLAG_PV;
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
            int inversedSecond = ((~second.apply(context)) + 1) & 0xFF;
            int sum = (context.first.intValue() & 0xFF) + inversedSecond;

            if ((sum & 0x100) == 0x100) {
                expectedNotFlags |= FLAG_C;  // reversed flag C expectations on substraction
            } else {
                expectedFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> borrowWithCarry() {
        evaluators.add((context, result) -> {
            int inversedCarry = (context.flags & FLAG_C) == FLAG_C ? 0 : FLAG_C;
            int inversedSecond = ((~second.apply(context)) + 1) & 0xFF;
            int sum = (context.first.intValue() & 0xFF) + inversedSecond + inversedCarry;
            if ((sum & 0x100) == 0x100) {
                expectedNotFlags |= FLAG_C;
            } else {
                expectedFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandMSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 0x80) == 0x80) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandLSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 1) == 1) {
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

    public FlagsCheckImpl<T> halfCarry() {
        evaluators.add((context, result) -> {
            int carryIns = result ^ (first.apply(context) & 0xFF) ^ (second.apply(context) & 0xFF);
            int halfCarryOut = (carryIns >> 4) & 1;

            if (halfCarryOut == 1) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> halfBorrow() {
        evaluators.add((context, result) -> {
            int inversedSecond = ((~second.apply(context)) + 1) & 0xFF;

            int carryIns = result ^ (first.apply(context) & 0xFF) ^ inversedSecond;
            int halfCarryOut = (carryIns >>> 4) & 1;

            if (halfCarryOut == 1) {
                expectedFlags |= FLAG_H;
            } else {
                expectedNotFlags |= FLAG_H;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> halfCarry11() {
        evaluators.add((context, result) -> {
            int second = (result - context.first.intValue()) & 0xFFFF;

            int mask = second & context.first.intValue();
            int xormask = second ^ context.first.intValue();

            int C0 = mask & 1;
            int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
            int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
            int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;
            int C4 = ((mask >>> 4) ^ (C3 & (xormask >>> 4))) & 1;
            int C5 = ((mask >>> 5) ^ (C4 & (xormask >>> 5))) & 1;
            int C6 = ((mask >>> 6) ^ (C5 & (xormask >>> 6))) & 1;
            int C7 = ((mask >>> 7) ^ (C6 & (xormask >>> 7))) & 1;
            int C8 = ((mask >>> 8) ^ (C7 & (xormask >>> 8))) & 1;
            int C9 = ((mask >>> 9) ^ (C8 & (xormask >>> 9))) & 1;
            int C10 = ((mask >>> 10) ^ (C9 & (xormask >>> 10))) & 1;
            int C11 = ((mask >>> 11) ^ (C10 & (xormask >>> 11))) & 1;

            if (C11 != 0) {
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
