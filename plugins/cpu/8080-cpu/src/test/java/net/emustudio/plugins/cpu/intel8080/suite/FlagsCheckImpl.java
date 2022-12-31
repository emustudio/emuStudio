/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.intel8080.suite;

import net.emustudio.cpu.testsuite.FlagsCheck;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;

public class FlagsCheckImpl<T extends Number> extends FlagsCheck<T, FlagsCheckImpl<T>> {

    public static boolean isAuxCarry(int first, int sumWith) {
        int mask = sumWith & first;
        int xormask = sumWith ^ first;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;

        return (C3 != 0);
    }

    public FlagsCheckImpl<T> sign() {
        evaluators.add((context, result) -> {
            if (result.byteValue() < 0) {
                expectedFlags |= EmulatorEngine.FLAG_S;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_S;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> zero() {
        evaluators.add((context, result) -> {
            if (result.byteValue() == 0) {
                expectedFlags |= EmulatorEngine.FLAG_Z;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_Z;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> parity() {
        evaluators.add((context, result) -> {
            int numberOf1 = 0;
            int intResult = result & 0xFF;

            for (int i = 0; i < 8; i++) {
                if ((intResult & 1) == 1) {
                    numberOf1++;
                }
                intResult = intResult >>> 1;
            }

            if (numberOf1 % 2 == 0) {
                expectedFlags |= EmulatorEngine.FLAG_P;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_P;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carry15() {
        evaluators.add((context, result) -> {
            if ((result & 0x10000) == 0x10000) {
                expectedFlags |= EmulatorEngine.FLAG_C;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carry() {
        evaluators.add((context, result) -> {
            if ((result & 0x100) == 0x100) {
                expectedFlags |= EmulatorEngine.FLAG_C;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandMSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 0x80) == 0x80) {
                expectedFlags |= EmulatorEngine.FLAG_C;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsFirstOperandLSB() {
        evaluators.add((context, result) -> {
            if ((context.first.intValue() & 1) == 1) {
                expectedFlags |= EmulatorEngine.FLAG_C;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_C;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> carryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= EmulatorEngine.FLAG_C);
        return this;
    }

    public FlagsCheckImpl<T> auxCarry() {
        evaluators.add((context, result) -> {
            int firstInt = context.first.intValue();
            byte diff = (byte) ((result - firstInt) & 0xFF);

            if (isAuxCarry(context.first.intValue(), diff)) {
                expectedFlags |= EmulatorEngine.FLAG_AC;
            } else {
                expectedNotFlags |= EmulatorEngine.FLAG_AC;
            }
        });
        return this;
    }

    public FlagsCheckImpl<T> auxCarryIsReset() {
        evaluators.add((context, result) -> expectedNotFlags |= EmulatorEngine.FLAG_AC);
        return this;
    }
}
