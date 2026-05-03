/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.get8MSBplus8LSB;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

/**
 * CB-prefixed group: BIT, SET, RES and the rotation/shift family
 * (RLC, RRC, RL, RR, SLA, SRA, SLL, SRL). Behavior, T-state cycles and R-register delta are
 * verified for each addressing mode (r, (HL), (IX+d), (IY+d)). DD/FD CB undocumented "result
 * also stored in r" variants for SET/RES are exercised in dedicated tests.
 * <p>
 * Cycle reference:
 * - CB op r:               8T,  R+=2
 * - CB op (HL):           15T,  R+=2 (12T for BIT)
 * - DD/FD CB d op:        23T,  R+=2 (20T for BIT)
 */
public class BitTest extends InstructionsTest {

    private static final int[] R_REGS = {REG_B, REG_C, REG_D, REG_E, REG_H, REG_L, REG_A};
    private static final int[] R_OPCODES_LO3 = {0, 1, 2, 3, 4, 5, 7};

    // BIT b,r — opcodes CB 40 + b*8 + r. 8T R+=2. r=B,C,D,E,H,L,(HL),A so use r in {0..5,7}.
    @Test
    public void testBIT__b__r() {
        for (int b = 0; b < 8; b++) {
            for (int i = 0; i < R_REGS.length; i++) {
                int reg = R_REGS[i];
                int opcode = 0x40 | (b << 3) | R_OPCODES_LO3[i];
                ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .firstIsRegister(reg)
                        .verifyRegister(reg, c -> c.first & 0xFF) // BIT does not change the operand
                        .verifyR(2).verifyCycles(8);
                Generator.forSome8bitUnary(test.run(0xCB, opcode));
            }
        }
    }

    // BIT b,(HL) — opcodes CB 46 + b*8. 12T R+=2.
    @Test
    public void testBIT__b__mHL() {
        for (int b = 0; b < 8; b++) {
            int opcode = 0x46 | (b << 3);
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .setPair(REG_PAIR_HL, 2).firstIsMemoryByteAt(2)
                    .verifyByte(2, c -> c.first & 0xFF) // memory unchanged
                    .verifyR(2).verifyCycles(12);
            Generator.forSome8bitUnary(test.run(0xCB, opcode));
        }
    }

    // BIT b,(IX+d) / (IY+d) — opcodes DD/FD CB d 46+b*8. 20T R+=2.
    @Test
    public void testBIT__b__mII_plus_d() {
        for (int b = 0; b < 8; b++) {
            int opcode = 0x46 | (b << 3);
            for (int prefix : new int[]{0xDD, 0xFD}) {
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                        .verifyR(2).verifyCycles(20)
                        .keepCurrentInjectorsAfterRun();
                if (prefix == 0xDD) test = test.first8MSBisIX();
                else test = test.first8MSBisIY();
                Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                        test.runWithFirst8bitOperandWithOpcodeAfter(opcode, prefix, 0xCB));
            }
        }
    }

    // SET b,r — opcodes CB C0 + b*8 + r. 8T R+=2.
    @Test
    public void testSET__b__r() {
        runBitwiseReg(0xC0, true);
    }

    // SET b,(HL) — opcodes CB C6 + b*8. 15T R+=2.
    @Test
    public void testSET__b__mHL() {
        runBitwiseMHL(0xC6, true);
    }

    // SET b,(IX+d)/(IY+d) — opcodes DD/FD CB d C6+b*8. 23T R+=2.
    @Test
    public void testSET__b__mII_plus_d() {
        runBitwiseMIIplusD(0xC6, true);
    }

    // Undoc DD/FD CB d (C0 + b*8 + r): also writes the result back to r (r != 6).
    @Test
    public void testSET__b__mII_plus_d__r() {
        runBitwiseMIIplusD_undoc(0xC0, true);
    }

    // RES b,r — opcodes CB 80 + b*8 + r. 8T R+=2.
    @Test
    public void testRES__b__r() {
        runBitwiseReg(0x80, false);
    }

    // RES b,(HL) — opcodes CB 86 + b*8. 15T R+=2.
    @Test
    public void testRES__b__mHL() {
        runBitwiseMHL(0x86, false);
    }

    // RES b,(IX+d)/(IY+d) — opcodes DD/FD CB d 86+b*8. 23T R+=2.
    @Test
    public void testRES__b__mII_plus_d() {
        runBitwiseMIIplusD(0x86, false);
    }

    // Undoc DD/FD CB d (80 + b*8 + r): also writes the result back to r (r != 6).
    @Test
    public void testRES__b__mII_plus_d__r() {
        runBitwiseMIIplusD_undoc(0x80, false);
    }

    // Rotates / shifts: RLC=00, RRC=08, RL=10, RR=18, SLA=20, SRA=28, SLL=30, SRL=38
    @Test
    public void testRLC() {
        runRot(0x00, c -> ((c.first << 1) & 0xFF) | ((c.first >>> 7) & 1));
    }

    @Test
    public void testRRC() {
        runRot(0x08, c -> (((c.first & 0xFF) >>> 1) | (c.first << 7)) & 0xFF);
    }

    @Test
    public void testRL() {
        runRot(0x10, c -> ((c.first << 1) & 0xFF) | (c.flags & FLAG_C));
    }

    @Test
    public void testRR() {
        runRot(0x18, c -> ((c.first >> 1) & 0x7F) | ((c.flags & FLAG_C) << 7));
    }

    @Test
    public void testSLA() {
        runRot(0x20, c -> (c.first << 1) & 0xFE);
    }

    @Test
    public void testSRA() {
        runRot(0x28, c -> ((c.first >> 1) & 0xFF) | (c.first & 0x80));
    }

    @Test
    public void testSLL() {
        runRot(0x30, c -> ((c.first << 1) & 0xFF) | 1);
    }

    @Test
    public void testSRL() {
        runRot(0x38, c -> (c.first >>> 1) & 0x7F);
    }

    // ---------- Helpers ----------

    /**
     * SET / RES for register operands. baseLow3=0xC0/0x80 base; setBit=true means SET, false=RES.
     */
    private void runBitwiseReg(int base, boolean setBit) {
        for (int b = 0; b < 8; b++) {
            for (int i = 0; i < R_REGS.length; i++) {
                int reg = R_REGS[i];
                int opcode = base | (b << 3) | R_OPCODES_LO3[i];
                int mask = 1 << b;
                Function<RunnerContext<Byte>, Integer> op = setBit
                        ? c -> (c.first | mask) & 0xFF
                        : c -> (c.first & ~mask) & 0xFF;
                ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .firstIsRegister(reg)
                        .verifyRegister(reg, op)
                        .verifyR(2).verifyCycles(8);
                Generator.forSome8bitUnary(test.run(0xCB, opcode));
            }
        }
    }

    /**
     * SET / RES for (HL). 15T R+=2.
     */
    private void runBitwiseMHL(int base, boolean setBit) {
        for (int b = 0; b < 8; b++) {
            int opcode = base | (b << 3);
            int mask = 1 << b;
            Function<RunnerContext<Byte>, Integer> op = setBit
                    ? c -> (c.first | mask) & 0xFF
                    : c -> (c.first & ~mask) & 0xFF;
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .setPair(REG_PAIR_HL, 2).firstIsMemoryByteAt(2)
                    .verifyByte(2, op)
                    .verifyR(2).verifyCycles(15);
            Generator.forSome8bitUnary(test.run(0xCB, opcode));
        }
    }

    /**
     * SET / RES for (IX+d)/(IY+d). 23T R+=2.
     */
    private void runBitwiseMIIplusD(int base, boolean setBit) {
        for (int b = 0; b < 8; b++) {
            int opcode = base | (b << 3) | 0x06; // r=6 selects (II+d) only
            int mask = 1 << b;
            Function<RunnerContext<Integer>, Integer> op = setBit
                    ? c -> (c.second | mask) & 0xFF
                    : c -> (c.second & ~mask) & 0xFF;
            for (int prefix : new int[]{0xDD, 0xFD}) {
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                        .verifyByte(c -> get8MSBplus8LSB(c.first), op)
                        .verifyR(2).verifyCycles(23)
                        .keepCurrentInjectorsAfterRun();
                if (prefix == 0xDD) test = test.first8MSBisIX();
                else test = test.first8MSBisIY();
                Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                        test.runWithFirst8bitOperandWithOpcodeAfter(opcode, prefix, 0xCB));
            }
        }
    }

    /**
     * Undocumented DD/FD CB d SET/RES r (low 3 bits != 6) — also writes result into r.
     */
    private void runBitwiseMIIplusD_undoc(int base, boolean setBit) {
        // Use bit=0 only (one b enough to verify the side effect across registers).
        int b = 0;
        int mask = 1 << b;
        Function<RunnerContext<Integer>, Integer> op = setBit
                ? c -> (c.second | mask) & 0xFF
                : c -> (c.second & ~mask) & 0xFF;
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int i = 0; i < R_REGS.length; i++) {
                int reg = R_REGS[i];
                int opcode = base | (b << 3) | R_OPCODES_LO3[i];
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                        .verifyByte(c -> get8MSBplus8LSB(c.first), op)
                        .verifyRegister(reg, c -> op.apply(c) & 0xFF)
                        .verifyR(2).verifyCycles(23)
                        .keepCurrentInjectorsAfterRun();
                if (prefix == 0xDD) test = test.first8MSBisIX();
                else test = test.first8MSBisIY();
                Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                        test.runWithFirst8bitOperandWithOpcodeAfter(opcode, prefix, 0xCB));
            }
        }
    }

    /**
     * CB rotates/shifts for r / (HL) / (IX+d) / (IY+d). subOpc points to base of the op (0..0x38 step 8).
     */
    private void runRot(int subOpc, Function<RunnerContext<Byte>, Integer> op) {
        // r variants — 8T R+=2
        for (int i = 0; i < R_REGS.length; i++) {
            int reg = R_REGS[i];
            int opcode = subOpc | R_OPCODES_LO3[i];
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsRegister(reg)
                    .setFlags(FLAG_H | FLAG_N)
                    .verifyRegister(reg, op)
                    .verifyR(2).verifyCycles(8);
            Generator.forSome8bitUnary(test.run(0xCB, opcode));
        }
        // (HL) — 15T R+=2
        ByteTestBuilder mHL = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setPair(REG_PAIR_HL, 2).firstIsMemoryByteAt(2)
                .setFlags(FLAG_H | FLAG_N)
                .verifyByte(2, op)
                .verifyR(2).verifyCycles(15);
        Generator.forSome8bitUnary(mHL.run(0xCB, subOpc | 0x06));
        // (IX+d) / (IY+d) — 23T R+=2; uses second operand as memory byte
        Function<RunnerContext<Integer>, Integer> opInt =
                c -> op.apply(new RunnerContext<>((byte) (c.second & 0xFF), (byte) 0, c.flags)) & 0xFF;
        for (int prefix : new int[]{0xDD, 0xFD}) {
            IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                    .verifyByte(c -> get8MSBplus8LSB(c.first), opInt)
                    .setFlags(FLAG_H | FLAG_N)
                    .verifyR(2).verifyCycles(23)
                    .keepCurrentInjectorsAfterRun();
            if (prefix == 0xDD) test = test.first8MSBisIX();
            else test = test.first8MSBisIY();
            Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                    test.runWithFirst8bitOperandWithOpcodeAfter(subOpc | 0x06, prefix, 0xCB));
        }
    }
}

