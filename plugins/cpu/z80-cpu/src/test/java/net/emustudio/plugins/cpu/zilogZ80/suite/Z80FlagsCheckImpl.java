/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.FlagsCheck;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

/**
 * Declarative Z80 flag rules used by per-instruction tests.
 * <p>
 * Each rule appends an evaluator that consumes the runner context plus the operation result and
 * sets the matching expected/not-expected flag bits. Bits not described by any rule are left
 * unchecked. Both documented (S, Z, H, P/V, N, C) and undocumented (Y, X) bits are supported,
 * including the Z80-specific cases:
 * <ul>
 *   <li>Y/X derived from the result for ALU/logic/rotate/shift/CB/SET/RES/BIT (and mirroring
 *       cases like CP where they come from the operand).</li>
 *   <li>Y/X copied from MEMPTR (high byte) for {@code BIT n,(HL)} and friends.</li>
 *   <li>Post-CCF/SCF behavior where Y/X = (previous F) | A.</li>
 * </ul>
 * Use {@link #expectFlagOnlyWhen(int, java.util.function.BiFunction)} for ad-hoc rules.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Z80FlagsCheckImpl extends FlagsCheck<Byte, Z80FlagsCheckImpl> {

    public Z80FlagsCheckImpl sign() {
        return expectFlagOnlyWhen(FLAG_S, (ctx, result) -> (result.intValue() & 0x80) != 0);
    }

    public Z80FlagsCheckImpl signWord() {
        return expectFlagOnlyWhen(FLAG_S, (ctx, result) -> (result.intValue() & 0x8000) != 0);
    }

    public Z80FlagsCheckImpl zero() {
        return expectFlagOnlyWhen(FLAG_Z, (ctx, result) -> (result.intValue() & 0xFF) == 0);
    }

    public Z80FlagsCheckImpl zeroWord() {
        return expectFlagOnlyWhen(FLAG_Z, (ctx, result) -> (result.intValue() & 0xFFFF) == 0);
    }

    public Z80FlagsCheckImpl parity() {
        return expectFlagOnlyWhen(FLAG_PV, (ctx, result) -> {
            int byteValue = result.intValue() & 0xFF;
            int bits = Integer.bitCount(byteValue);
            return (bits & 1) == 0;
        });
    }

    public Z80FlagsCheckImpl carry15() {
        return expectFlagOnlyWhen(FLAG_C, (ctx, result) -> (result.intValue() & 0x10000) != 0);
    }

    public Z80FlagsCheckImpl carry8() {
        return expectFlagOnlyWhen(FLAG_C, (ctx, result) -> (result.intValue() & 0x100) != 0);
    }

    public Z80FlagsCheckImpl borrow8() {
        return expectFlagOnlyWhen(FLAG_C, (ctx, result) -> (result.intValue() & 0x100) != 0);
    }

    public Z80FlagsCheckImpl halfCarry8() {
        return expectFlagOnlyWhen(FLAG_H, (ctx, result) -> {
            int first = ctx.first.intValue() & 0xFF;
            int second = ctx.second.intValue() & 0xFF;
            return (((first & 0x0F) + (second & 0x0F)) & 0x10) != 0;
        });
    }

    public Z80FlagsCheckImpl halfBorrow8() {
        return expectFlagOnlyWhen(FLAG_H, (ctx, result) -> {
            int first = ctx.first.intValue() & 0xFF;
            int second = ctx.second.intValue() & 0xFF;
            return ((first & 0x0F) - (second & 0x0F)) < 0;
        });
    }

    public Z80FlagsCheckImpl overflowAdd8() {
        return expectFlagOnlyWhen(FLAG_PV, (ctx, result) -> {
            int first = ctx.first.intValue();
            int second = ctx.second.intValue();
            int sum = result.intValue();
            return (((first ^ ~second) & (first ^ sum)) & 0x80) != 0;
        });
    }

    public Z80FlagsCheckImpl overflowSub8() {
        return expectFlagOnlyWhen(FLAG_PV, (ctx, result) -> {
            int first = ctx.first.intValue();
            int second = ctx.second.intValue();
            int diff = result.intValue();
            return (((first ^ second) & (first ^ diff)) & 0x80) != 0;
        });
    }

    public Z80FlagsCheckImpl undocumentedYX() {
        evaluators.add((ctx, result) -> {
            int byteValue = result.intValue() & 0xFF;
            if ((byteValue & FLAG_Y) != 0) {
                expectedFlags |= FLAG_Y;
            } else {
                expectedNotFlags |= FLAG_Y;
            }
            if ((byteValue & FLAG_X) != 0) {
                expectedFlags |= FLAG_X;
            } else {
                expectedNotFlags |= FLAG_X;
            }
        });
        return this;
    }

    public Z80FlagsCheckImpl subtract() {
        return or(FLAG_N);
    }

    public Z80FlagsCheckImpl notSubtract() {
        evaluators.add((ctx, result) -> expectedNotFlags |= FLAG_N);
        return this;
    }

    public Z80FlagsCheckImpl carryUnchanged() {
        evaluators.add((ctx, result) -> {
            if ((ctx.flags & FLAG_C) != 0) {
                expectedFlags |= FLAG_C;
            } else {
                expectedNotFlags |= FLAG_C;
            }
        });
        return this;
    }

    public Z80FlagsCheckImpl halfCarryClear() {
        evaluators.add((ctx, result) -> expectedNotFlags |= FLAG_H);
        return this;
    }

    public Z80FlagsCheckImpl halfCarrySet() {
        return or(FLAG_H);
    }
}

