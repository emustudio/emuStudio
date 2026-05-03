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
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

/**
 * Logic-group instructions: AND/OR/XOR/CP (all addressing modes), DAA, CPL, SCF, CCF and the
 * accumulator rotates RLCA/RRCA/RLA/RRA. CB-prefixed rotates/shifts live in BitTest; NEG lives
 * in ArithmeticTest; CPI/CPD/CPIR/CPDR and RLD/RRD live in TransferTest.
 * <p>
 * Cycle reference (Z80 official):
 * - AND/OR/XOR/CP r:                  4T, R+=1
 * - AND/OR/XOR/CP n / (HL):           7T, R+=1
 * - AND/OR/XOR/CP IXh|IXl|IYh|IYl:    8T, R+=2
 * - AND/OR/XOR/CP (IX+d)/(IY+d):     19T, R+=2
 * - DAA, CPL, SCF, CCF, RxxA:         4T, R+=1
 */
public class LogicTest extends InstructionsTest {

    @Test
    public void testAND() {
        Function<RunnerContext<Byte>, Integer> op = c -> (c.first & 0xFF) & (c.second & 0xFF);
        runLogicByte(op, 0xA7, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5);
        runLogicByteHL(op, 0xA6);
        runLogicByteImm(op, 0xE6);
        runLogicByteII(op, 0xA4, 0xA5);
        runLogicByteIIplusD(op, 0xA6);
    }

    @Test
    public void testOR() {
        Function<RunnerContext<Byte>, Integer> op = c -> (c.first & 0xFF) | (c.second & 0xFF);
        runLogicByte(op, 0xB7, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5);
        runLogicByteHL(op, 0xB6);
        runLogicByteImm(op, 0xF6);
        runLogicByteII(op, 0xB4, 0xB5);
        runLogicByteIIplusD(op, 0xB6);
    }

    @Test
    public void testXOR() {
        Function<RunnerContext<Byte>, Integer> op = c -> (c.first & 0xFF) ^ (c.second & 0xFF);
        runLogicByte(op, 0xAF, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD);
        runLogicByteHL(op, 0xAE);
        runLogicByteImm(op, 0xEE);
        runLogicByteII(op, 0xAC, 0xAD);
        runLogicByteIIplusD(op, 0xAE);
    }

    @Test
    public void testCP() {
        // CP keeps A unchanged but sets flags from (A - operand). Verify A stays the same.
        Function<RunnerContext<Byte>, Integer> aPreserved = c -> c.first & 0xFF;
        runCpByte(aPreserved, 0xBF, 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD);
        runCpByteHL(aPreserved, 0xBE);
        runCpByteImm(aPreserved, 0xFE);
        runCpByteII(aPreserved, 0xBC, 0xBD);
        runCpByteIIplusD(aPreserved, 0xBE);
    }

    @Test
    public void testDAA() {
        // Hand-picked vectors covering all (A, H, N, C) corners; expected A given Z80 DAA table.
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsFlags()
                .verifyR(1).verifyCycles(4)
                .keepCurrentInjectorsAfterRun();
        int[][] params = new int[][]{
                {0xAA, 0x7D, 0x10, 0x11}, {0xAA, 0x7E, 0x44, 0x07}, {0xAA, 0x7F, 0x44, 0x07},
                {0xAD, 0x7E, 0x47, 0x07}, {0xAD, 0x7F, 0x47, 0x07}, {0xAC, 0x6C, 0x12, 0x15},
                {0xA6, 0x7E, 0x40, 0x03}, {0xA6, 0x7F, 0x40, 0x03}, {0xA1, 0x6C, 0x01, 0x01},
                {0xA1, 0x6D, 0x01, 0x01}, {0xB8, 0x6F, 0x58, 0x0B}, {0xB8, 0x7C, 0x1E, 0x0D},
                {0xB8, 0x7D, 0x1E, 0x0D}, {0xB8, 0x7E, 0x52, 0x03}, {0x95, 0xE4, 0x95, 0x84},
                {0x95, 0xE5, 0xF5, 0xA5}, {0x95, 0xE6, 0x95, 0x86}, {0x95, 0xE7, 0x35, 0x27},
                {0x28, 0xE9, 0x88, 0x8D}, {0x28, 0xEA, 0x28, 0x2E}, {0x28, 0xEB, 0xC8, 0x8B},
                {0x28, 0xF8, 0x2E, 0x2C}, {0x53, 0xF8, 0x59, 0x0C}, {0x53, 0xF9, 0xB9, 0xA9},
                {0x53, 0xFA, 0x4D, 0x1E}, {0x53, 0xFB, 0xED, 0xBF}, {0xEF, 0xEF, 0x89, 0x8B},
                {0xEF, 0xFC, 0x55, 0x15}, {0xEF, 0xFD, 0x55, 0x15}, {0xEF, 0xFE, 0x89, 0x8B},
                {0x20, 0xFD, 0x86, 0x81}, {0x20, 0xFE, 0x1A, 0x1A}, {0x20, 0xFF, 0xBA, 0xBB},
                {0x1C, 0xEF, 0xB6, 0xA3}, {0x1C, 0xFC, 0x22, 0x34}, {0x1C, 0xFD, 0x82, 0x95},
                {0x16, 0xFF, 0xB0, 0xA3}, {0x11, 0xEC, 0x11, 0x04}, {0x11, 0xED, 0x71, 0x25},
                {0x11, 0xEE, 0x11, 0x06}, {0x55, 0xEC, 0x55, 0x04}, {0x55, 0xED, 0xB5, 0xA1},
                {0x55, 0xEE, 0x55, 0x06},
        };
        for (int[] p : params) {
            test.verifyRegister(REG_A, c -> p[2]).run(0x27).accept((byte) p[0], (byte) p[1]);
        }
    }

    @Test
    public void testCPL() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, c -> ~c.first)
                .verifyR(1).verifyCycles(4);
        Generator.forSome8bitUnary(test.run(0x2F));
    }

    @Test
    public void testSCF() {
        cpuRunnerImpl.setProgram(0x37);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.step();
        cpuVerifierImpl.checkFlags(FLAG_C);
    }

    @Test
    public void testCCF() {
        cpuRunnerImpl.setProgram(0x3F);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setFlags(FLAG_C);
        cpuRunnerImpl.step();
        cpuVerifierImpl.checkNotFlags(FLAG_C);
    }

    @Test
    public void testRLCA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, c -> ((c.first << 1) & 0xFF) | (c.first >>> 7) & 1)
                .verifyR(1).verifyCycles(4);
        Generator.forSome8bitUnary(test.run(0x07));
    }

    @Test
    public void testRRCA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, c -> (((c.first & 0xFF) >>> 1) | (c.first << 7)) & 0xFF)
                .verifyR(1).verifyCycles(4);
        Generator.forSome8bitUnary(test.run(0x0F));
    }

    @Test
    public void testRLA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, c -> ((c.first << 1) & 0xFE) | (c.flags & 1))
                .verifyR(1).verifyCycles(4);
        Generator.forSome8bitUnary(test.run(0x17));
    }

    @Test
    public void testRRA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, c -> ((c.first >> 1) & 0x7F) | ((c.flags & 1) << 7))
                .verifyR(1).verifyCycles(4);
        Generator.forSome8bitUnary(test.run(0x1F));
    }

    // ---------- Helpers (logic ops; mirror the ALU helpers in ArithmeticTest) ----------

    private void runLogicByte(Function<RunnerContext<Byte>, Integer> op,
                              int opcA, int opcB, int opcC, int opcD, int opcE, int opcH, int opcL) {
        ByteTestBuilder test = logicByteBuilder(op, 4, 1);
        Generator.forSome8bitBinaryWhichEqual(test.run(opcA));
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(opcB),
                test.secondIsRegister(REG_C).run(opcC),
                test.secondIsRegister(REG_D).run(opcD),
                test.secondIsRegister(REG_E).run(opcE),
                test.secondIsRegister(REG_H).run(opcH),
                test.secondIsRegister(REG_L).run(opcL)
        );
    }

    private void runLogicByteHL(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        ByteTestBuilder test = logicByteBuilder(op, 7, 1)
                .setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1);
        Generator.forSome8bitBinary(test.run(opcode));
    }

    private void runLogicByteImm(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        ByteTestBuilder test = logicByteBuilder(op, 7, 1);
        Generator.forSome8bitBinary(test.runWithSecondOperand(opcode));
    }

    private void runLogicByteII(Function<RunnerContext<Byte>, Integer> op, int opcHigh, int opcLow) {
        runLogicByteIIVariant(op, 0xDD, opcHigh, true);
        runLogicByteIIVariant(op, 0xDD, opcLow, false);
        runLogicByteIIVariant(op, 0xFD, opcHigh, true);
        runLogicByteIIVariant(op, 0xFD, opcLow, false);
    }

    private void runLogicByteIIVariant(Function<RunnerContext<Byte>, Integer> op,
                                       int prefix, int opcode, boolean high) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .secondIsIX_or_IY(prefix == 0xFD)
                .verifyR(2).verifyCycles(8);
        if (high) {
            test = test.verifyRegister(REG_A,
                    c -> op.apply(byteCtx2((byte) (c.first & 0xFF), (byte) (c.second >>> 8), c.flags)) & 0xFF);
        } else {
            test = test.verifyRegister(REG_A,
                    c -> op.apply(byteCtx2((byte) (c.first & 0xFF), (byte) (c.second & 0xFF), c.flags)) & 0xFF);
        }
        Generator.forSome16bitBinary(test.run(prefix, opcode));
    }

    private void runLogicByteIIplusD(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, c -> op.apply(byteCtx2(
                        (byte) (c.first & 0xFF), (byte) (c.second & 0xFF), c.flags)) & 0xFF)
                .verifyR(2).verifyCycles(19)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, opcode),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, opcode)
        );
    }

    private ByteTestBuilder logicByteBuilder(Function<RunnerContext<Byte>, Integer> op, int cycles, int rDelta) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, op)
                .verifyR(rDelta).verifyCycles(cycles)
                .keepCurrentInjectorsAfterRun();
    }

    // ---------- CP helpers (verify A unchanged; flags computed but not asserted here) ----------

    private void runCpByte(Function<RunnerContext<Byte>, Integer> aPreserved,
                           int opcA, int opcB, int opcC, int opcD, int opcE, int opcH, int opcL) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, aPreserved)
                .verifyR(1).verifyCycles(4)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome8bitBinaryWhichEqual(test.run(opcA));
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(opcB),
                test.secondIsRegister(REG_C).run(opcC),
                test.secondIsRegister(REG_D).run(opcD),
                test.secondIsRegister(REG_E).run(opcE),
                test.secondIsRegister(REG_H).run(opcH),
                test.secondIsRegister(REG_L).run(opcL)
        );
    }

    private void runCpByteHL(Function<RunnerContext<Byte>, Integer> aPreserved, int opcode) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1)
                .verifyRegister(REG_A, aPreserved)
                .verifyR(1).verifyCycles(7);
        Generator.forSome8bitBinary(test.run(opcode));
    }

    private void runCpByteImm(Function<RunnerContext<Byte>, Integer> aPreserved, int opcode) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, aPreserved)
                .verifyR(1).verifyCycles(7);
        Generator.forSome8bitBinary(test.runWithSecondOperand(opcode));
    }

    private void runCpByteII(Function<RunnerContext<Byte>, Integer> aPreserved, int opcHigh, int opcLow) {
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int opcode : new int[]{opcHigh, opcLow}) {
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8LSBisRegister(REG_A)
                        .secondIsIX_or_IY(prefix == 0xFD)
                        .verifyRegister(REG_A, c -> c.first & 0xFF)
                        .verifyR(2).verifyCycles(8);
                Generator.forSome16bitBinary(test.run(prefix, opcode));
            }
        }
    }

    private void runCpByteIIplusD(Function<RunnerContext<Byte>, Integer> aPreserved, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, c -> c.first & 0xFF)
                .verifyR(2).verifyCycles(19)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, opcode),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, opcode)
        );
    }

    private static RunnerContext<Byte> byteCtx2(Byte a, Byte b, int flags) {
        return new RunnerContext<>(a, b, flags);
    }
}

