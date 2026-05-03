/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.Z80FlagsCheckImpl;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

/**
 * I/O instructions: simple port reads/writes and the block I/O group.
 * Verifies behavior, T-state cycles, R-register delta, port traffic; flags for ED-prefixed ops.
 * Cycles for repeating block ops depend on B-after-decrement (16 if zero, 21 if non-zero).
 */
public class IOTest extends InstructionsTest {

    /**
     * IN A,(n) — DB nn — 11T, R+=1, no flag change.
     */
    @Test
    public void testIN__A__mn() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .verifyR(1)
                .verifyCycles(11);

        Generator.forSome8bitBinary(test.runWithSecondOperand(0xDB));
    }

    /**
     * IN r,(C) — ED 40+8r — 12T, R+=2, sets S/Z/PV/Y/X from byte; H,N=0; C unchanged.
     */
    @Test
    public void testIN__r__mC() {
        Function<RunnerContext<Byte>, Integer> read = context -> context.first & 0xFF;
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_C)
                .verifyR(2)
                .verifyCycles(12)
                .verifyFlags(inCFlags(), read)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(
                test.verifyRegister(REG_A, read).runWithSecondOperand(0xED, 0x78),
                test.verifyRegister(REG_B, read).runWithSecondOperand(0xED, 0x40),
                test.verifyRegister(REG_C, read).runWithSecondOperand(0xED, 0x48),
                test.verifyRegister(REG_D, read).runWithSecondOperand(0xED, 0x50),
                test.verifyRegister(REG_E, read).runWithSecondOperand(0xED, 0x58),
                test.verifyRegister(REG_H, read).runWithSecondOperand(0xED, 0x60),
                test.verifyRegister(REG_L, read).runWithSecondOperand(0xED, 0x68)
        );
    }

    /**
     * IN (C) — ED 70 — undocumented; sets flags only, no register written.
     */
    @Test
    public void testIN__mC() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_C)
                .verifyR(2)
                .verifyCycles(12)
                .verifyFlags(inCFlags(), context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(test.runWithSecondOperand(0xED, 0x70));
    }

    /**
     * OUT (n),A — D3 nn — 11T, R+=1, no flag change.
     */
    @Test
    public void testOUT__mn__A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF)
                .verifyR(1)
                .verifyCycles(11);

        Generator.forSome8bitBinary(test.runWithSecondOperand(0xD3));
    }

    /**
     * OUT (C),r — ED 41+8r — 12T, R+=2, no flag change.
     */
    @Test
    public void testOUT__mC__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsRegister(REG_C)
                .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF)
                .verifyR(2)
                .verifyCycles(12)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.firstIsRegister(REG_C).run(0xED, 0x49)
        );
        Generator.forSome8bitBinary(
                test.firstIsRegister(REG_B).run(0xED, 0x41),
                test.firstIsRegister(REG_D).run(0xED, 0x51),
                test.firstIsRegister(REG_E).run(0xED, 0x59),
                test.firstIsRegister(REG_H).run(0xED, 0x61),
                test.firstIsRegister(REG_L).run(0xED, 0x69),
                test.firstIsRegister(REG_A).run(0xED, 0x79)
        );
    }

    /**
     * OUT (C),0 — ED 71 — undocumented (NMOS writes 0). 12T, R+=2.
     */
    @Test
    public void testOUT__mC__0() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsRegister(REG_C)
                .verifyDeviceWhenSecondIsPort(context -> 0)
                .verifyR(2)
                .verifyCycles(12)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.firstIsRegister(REG_C).run(0xED, 0x71)
        );
    }

    /**
     * INI — ED A2 — 16T, R+=2; (HL)=in(BC), HL+=1, B-=1.
     */
    @Test
    public void testINI() {
        Generator.forSome16bitBinary(2,
                blockInputBuilder(true).verifyR(2).verifyCycles(16).run(0xED, 0xA2));
    }

    /**
     * INIR — ED B2 — repeats INI until B==0. 21T per repeat, 16T on terminate.
     */
    @Test
    public void testINIR() {
        Generator.forSome16bitBinary(2,
                blockInputBuilder(true)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles(true))
                        .verifyPC(blockRepeatPC(true))
                        .run(0xED, 0xB2));
    }

    /**
     * IND — ED AA — 16T, R+=2; (HL)=in(BC), HL-=1, B-=1.
     */
    @Test
    public void testIND() {
        Generator.forSome16bitBinary(2,
                blockInputBuilder(false).verifyR(2).verifyCycles(16).run(0xED, 0xAA));
    }

    /**
     * INDR — ED BA — repeats IND until B==0.
     */
    @Test
    public void testINDR() {
        Generator.forSome16bitBinary(2,
                blockInputBuilder(false)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles(false))
                        .verifyPC(blockRepeatPC(false))
                        .run(0xED, 0xBA));
    }

    /**
     * OUTI — ED A3 — 16T, R+=2; B-=1, out(BC, (HL)), HL+=1.
     */
    @Test
    public void testOUTI() {
        Generator.forSome16bitBinary(2,
                blockOutputBuilder(true).verifyR(2).verifyCycles(16).run(0xED, 0xA3));
    }

    /**
     * OTIR — ED B3 — repeats OUTI until B==0.
     */
    @Test
    public void testOTIR() {
        Generator.forSome16bitBinary(2,
                blockOutputBuilder(true)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles(true))
                        .verifyPC(blockRepeatPC(true))
                        .run(0xED, 0xB3));
    }

    /**
     * OUTD — ED AB — 16T, R+=2; B-=1, out(BC, (HL)), HL-=1.
     */
    @Test
    public void testOUTD() {
        Generator.forSome16bitBinary(2,
                blockOutputBuilder(false).verifyR(2).verifyCycles(16).run(0xED, 0xAB));
    }

    /**
     * OTDR — ED BB — repeats OUTD until B==0.
     */
    @Test
    public void testOTDR() {
        Generator.forSome16bitBinary(2,
                blockOutputBuilder(false)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles(false))
                        .verifyPC(blockRepeatPC(false))
                        .run(0xED, 0xBB));
    }

    private IntegerTestBuilder blockInputBuilder(boolean increment) {
        // Input from port=C (after high byte set as device value), write to (HL), then HL ±= 1, B-=1
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisDeviceAndFirst8LSBIsPort()
                .first8LSBisRegister(REG_C)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8MSBisRegister(REG_B)
                .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF);
        return increment
                ? test.verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                : test.verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF);
    }

    private IntegerTestBuilder blockOutputBuilder(boolean increment) {
        // Read (HL), write to port=C, then HL ±= 1, B-=1
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_C)
                .first8MSBisRegister(REG_B)
                .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF);
        return increment
                ? test.verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                : test.verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF);
    }

    private static Function<RunnerContext<Integer>, Integer> blockRepeatCycles(boolean unused) {
        return context -> {
            int bAfter = (((context.first >>> 8) & 0xFF) - 1) & 0xFF;
            return (bAfter == 0) ? 16 : 21;
        };
    }

    private static Function<RunnerContext<Integer>, Integer> blockRepeatPC(boolean unused) {
        return context -> {
            int bAfter = (((context.first >>> 8) & 0xFF) - 1) & 0xFF;
            return (bAfter == 0) ? context.PC + 2 : context.PC;
        };
    }

    /**
     * Flags after IN r,(C) / IN (C): S,Z,P/V,Y,X from byte; H=0; N=0; C unchanged.
     */
    private static Z80FlagsCheckImpl inCFlags() {
        return new Z80FlagsCheckImpl().sign().zero().parity().undocumentedYX().halfCarryClear().notSubtract().carryUnchanged();
    }
}

