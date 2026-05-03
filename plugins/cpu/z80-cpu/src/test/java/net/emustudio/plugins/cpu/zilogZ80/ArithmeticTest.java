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
 * Arithmetic instructions: ADD/ADC/SUB/SBC (8-bit and 16-bit), INC/DEC, NEG.
 * One test per mnemonic; all addressing-mode variants live inside, each variant pairs its
 * specific opcode with the matching T-state and R-register increment count.
 * <p>
 * Z80 reference timings used here:
 * - ADD/ADC/SUB/SBC A,r:           4T,  R+=1
 * - ADD/ADC/SUB/SBC A,n / A,(HL):  7T,  R+=1
 * - ADD/ADC/SUB/SBC A,IXh|IXl|IYh|IYl: 8T, R+=2
 * - ADD/ADC/SUB/SBC A,(IX+d)/(IY+d):  19T, R+=2
 * - INC/DEC r:           4T,  R+=1
 * - INC/DEC (HL):       11T,  R+=1
 * - INC/DEC IXh|IXl|IYh|IYl:           8T, R+=2
 * - INC/DEC (IX+d)/(IY+d):           23T, R+=2
 * - INC/DEC rr:          6T,  R+=1
 * - INC/DEC IX/IY:      10T,  R+=2
 * - ADD HL,rr:          11T,  R+=1
 * - ADD IX/IY,rr:       15T,  R+=2
 * - ADC/SBC HL,rr:      15T,  R+=2
 * - NEG (ED 44/...):     8T,  R+=2
 */
public class ArithmeticTest extends InstructionsTest {

    @Test
    public void testADD__A() {
        Function<RunnerContext<Byte>, Integer> add = c -> (c.first & 0xFF) + (c.second & 0xFF);
        runAluByte(add, 0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85);  // ADD A,r (and A,A)
        runAluByteHL(add, 0x86);
        runAluByteImm(add, 0xC6);
        runAluByteII(add, 0x84, 0x85, true);   // IXh,IXl,IYh,IYl
        runAluByteIIplusD(add, 0x86);          // (IX+d)/(IY+d)
    }

    @Test
    public void testADC__A() {
        Function<RunnerContext<Byte>, Integer> adc = c -> (c.first & 0xFF) + (c.second & 0xFF) + (c.flags & FLAG_C);
        runAluByte(adc, 0x8F, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D);
        runAluByteHL(adc, 0x8E);
        runAluByteImm(adc, 0xCE);
        runAluByteII(adc, 0x8C, 0x8D, true);
        runAluByteIIplusD(adc, 0x8E);
    }

    @Test
    public void testSUB() {
        Function<RunnerContext<Byte>, Integer> sub = c -> ((c.first & 0xFF) - (c.second & 0xFF)) & 0xFF;
        runAluByte(sub, 0x97, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95);
        runAluByteHL(sub, 0x96);
        runAluByteImm(sub, 0xD6);
        runAluByteII(sub, 0x94, 0x95, true);
        runAluByteIIplusD(sub, 0x96);
    }

    @Test
    public void testSBC__A() {
        Function<RunnerContext<Byte>, Integer> sbc = c -> ((c.first & 0xFF) - (c.second & 0xFF) - (c.flags & FLAG_C)) & 0xFF;
        runAluByte(sbc, 0x9F, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D);
        runAluByteHL(sbc, 0x9E);
        runAluByteImm(sbc, 0xDE);
        runAluByteII(sbc, 0x9C, 0x9D, true);
        runAluByteIIplusD(sbc, 0x9E);
    }

    @Test
    public void testINC__r() {
        Function<RunnerContext<Byte>, Integer> inc = c -> (c.first + 1) & 0xFF;
        runIncDecReg(inc, 0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x3C);
        runIncDecMHL(inc, 0x34, 11);
        runIncDecII(inc, 0x24, 0x2C);
        runIncDecMIIplusD(inc, 0x34);
    }

    @Test
    public void testDEC__r() {
        Function<RunnerContext<Byte>, Integer> dec = c -> (c.first - 1) & 0xFF;
        runIncDecReg(dec, 0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x3D);
        runIncDecMHL(dec, 0x35, 11);
        runIncDecII(dec, 0x25, 0x2D);
        runIncDecMIIplusD(dec, 0x35);
    }

    @Test
    public void testINC__rr() {
        Function<RunnerContext<Integer>, Integer> inc = c -> c.first + 1;
        runIncDecRP(inc, 0x03, 0x13, 0x23, 0x33);
        runIncDecII16(inc, 0x23);
    }

    @Test
    public void testDEC__rr() {
        Function<RunnerContext<Integer>, Integer> dec = c -> c.first - 1;
        runIncDecRP(dec, 0x0B, 0x1B, 0x2B, 0x3B);
        runIncDecII16(dec, 0x2B);
    }

    @Test
    public void testADD__HL__rr() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, c -> c.first + c.second)
                .verifyR(1).verifyCycles(11)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(test.run(0x29));
        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0x09),
                test.secondIsPair(REG_PAIR_DE).run(0x19),
                test.secondIsPair(REG_SP).run(0x39)
        );
    }

    @Test
    public void testADD__IX__rr() {
        runAddII(true);
    }

    @Test
    public void testADD__IY__rr() {
        runAddII(false);
    }

    @Test
    public void testADC__HL__rr() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, c -> c.first + c.second + (c.flags & FLAG_C))
                .verifyR(2).verifyCycles(15)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryWhichEqual(test.run(0xED, 0x6A));
        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xED, 0x4A),
                test.secondIsPair(REG_PAIR_DE).run(0xED, 0x5A),
                test.secondIsPair(REG_SP).run(0xED, 0x7A)
        );
    }

    @Test
    public void testSBC__HL__rr() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, c -> c.first - c.second - (c.flags & FLAG_C))
                .verifyR(2).verifyCycles(15)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryWhichEqual(test.run(0xED, 0x62));
        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xED, 0x42),
                test.secondIsPair(REG_PAIR_DE).run(0xED, 0x52),
                test.secondIsPair(REG_SP).run(0xED, 0x72)
        );
    }

    @Test
    public void testNEG() {
        // ED 44 plus 7 undocumented aliases (ED 4C/54/5C/64/6C/74/7C). 8T, R+=2.
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, c -> (-c.first) & 0xFF)
                .verifyR(2).verifyCycles(8)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome8bitUnary(
                test.run(0xED, 0x44),
                test.run(0xED, 0x4C),
                test.run(0xED, 0x54),
                test.run(0xED, 0x5C),
                test.run(0xED, 0x64),
                test.run(0xED, 0x6C),
                test.run(0xED, 0x74),
                test.run(0xED, 0x7C)
        );
    }

    // ---------- Helpers ----------

    /**
     * ALU A,r (incl A,A) — 4T R+=1. opcodes in order: A,B,C,D,E,H,L.
     */
    private void runAluByte(Function<RunnerContext<Byte>, Integer> op,
                            int opcA, int opcB, int opcC, int opcD, int opcE, int opcH, int opcL) {
        ByteTestBuilder test = aluByteBuilder(op, 4, 1);
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

    /**
     * ALU A,(HL) — 7T R+=1.
     */
    private void runAluByteHL(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        ByteTestBuilder test = aluByteBuilder(op, 7, 1)
                .setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1);
        Generator.forSome8bitBinary(test.run(opcode));
    }

    /**
     * ALU A,n — 7T R+=1.
     */
    private void runAluByteImm(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        ByteTestBuilder test = aluByteBuilder(op, 7, 1);
        Generator.forSome8bitBinary(test.runWithSecondOperand(opcode));
    }

    /**
     * ALU A,IXh|IXl|IYh|IYl — 8T R+=2. opcHigh / opcLow point to IXh / IXl base; toggles via prefix.
     */
    private void runAluByteII(Function<RunnerContext<Byte>, Integer> op,
                              int opcHigh, int opcLow, boolean unused) {
        // Two builders: first=A operand, second=IXh/IXl etc.
        runAluByteIIPair(op, 0xDD, opcHigh, opcLow);
        runAluByteIIPair(op, 0xFD, opcHigh, opcLow);
    }

    private void runAluByteIIPair(Function<RunnerContext<Byte>, Integer> op,
                                  int prefix, int opcHigh, int opcLow) {
        // op accesses second; we project IX/IY high or low via setRegister-like indirection
        IntegerTestBuilder testHigh = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .secondIsIX_or_IY(prefix == 0xFD)
                .verifyRegister(REG_A, c -> {
                    Byte a = (byte) (c.first & 0xFF);
                    Byte high = (byte) (c.second >>> 8);
                    return op.apply(new RunnerContext<>(a, high, c.flags)) & 0xFF;
                })
                .verifyR(2).verifyCycles(8);
        Generator.forSome16bitBinary(testHigh.run(prefix, opcHigh));

        IntegerTestBuilder testLow = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .secondIsIX_or_IY(prefix == 0xFD)
                .verifyRegister(REG_A, c -> {
                    Byte a = (byte) (c.first & 0xFF);
                    Byte low = (byte) (c.second & 0xFF);
                    return op.apply(new RunnerContext<>(a, low, c.flags)) & 0xFF;
                })
                .verifyR(2).verifyCycles(8);
        Generator.forSome16bitBinary(testLow.run(prefix, opcLow));
    }

    /**
     * ALU A,(IX+d) / A,(IY+d) — 19T R+=2.
     */
    private void runAluByteIIplusD(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, c -> op.apply(new RunnerContext<>(
                        (byte) (c.first & 0xFF), (byte) (c.second & 0xFF), c.flags)) & 0xFF)
                .verifyR(2).verifyCycles(19)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, opcode),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, opcode)
        );
    }

    private ByteTestBuilder aluByteBuilder(Function<RunnerContext<Byte>, Integer> op, int cycles, int rDelta) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, op)
                .verifyR(rDelta).verifyCycles(cycles)
                .keepCurrentInjectorsAfterRun();
    }

    /**
     * INC/DEC r — 4T R+=1. opcodes: B,C,D,E,H,L,A.
     */
    private void runIncDecReg(Function<RunnerContext<Byte>, Integer> op,
                              int opB, int opC, int opD, int opE, int opH, int opL, int opA) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyR(1).verifyCycles(4)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();
        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_B).verifyRegister(REG_B, op).run(opB),
                test.firstIsRegister(REG_C).verifyRegister(REG_C).run(opC),
                test.firstIsRegister(REG_D).verifyRegister(REG_D).run(opD),
                test.firstIsRegister(REG_E).verifyRegister(REG_E).run(opE),
                test.firstIsRegister(REG_H).verifyRegister(REG_H).run(opH),
                test.firstIsRegister(REG_L).verifyRegister(REG_L).run(opL),
                test.firstIsRegister(REG_A).verifyRegister(REG_A).run(opA)
        );
    }

    /**
     * INC/DEC (HL) — 11T R+=1.
     */
    private void runIncDecMHL(Function<RunnerContext<Byte>, Integer> op, int opcode, int cycles) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryByteAt(1).setPair(REG_PAIR_HL, 1)
                .verifyByte(1, op)
                .verifyR(1).verifyCycles(cycles);
        Generator.forSome8bitUnary(test.run(opcode));
    }

    /**
     * INC/DEC IXh|IXl|IYh|IYl — 8T R+=2. opcHigh used for IXh/IYh, opcLow for IXl/IYl.
     */
    private void runIncDecII(Function<RunnerContext<Byte>, Integer> op, int opcHigh, int opcLow) {
        runIncDecIIVariant(op, 0xDD, opcHigh, true);
        runIncDecIIVariant(op, 0xDD, opcLow, false);
        runIncDecIIVariant(op, 0xFD, opcHigh, true);
        runIncDecIIVariant(op, 0xFD, opcLow, false);
    }

    private void runIncDecIIVariant(Function<RunnerContext<Byte>, Integer> op,
                                    int prefix, int opcode, boolean high) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX_or_IY(prefix == 0xFD)
                .verifyR(2).verifyCycles(8);
        if (high) {
            test = test.verifyIX_or_IY(prefix == 0xFD,
                    c -> (c.first & 0xFF) | ((op.apply(byteCtx((byte) (c.first >>> 8))) & 0xFF) << 8));
        } else {
            test = test.verifyIX_or_IY(prefix == 0xFD,
                    c -> (c.first & 0xFF00) | (op.apply(byteCtx((byte) (c.first & 0xFF))) & 0xFF));
        }
        Generator.forSome16bitUnary(test.run(prefix, opcode));
    }

    private static RunnerContext<Byte> byteCtx(Byte value) {
        return new RunnerContext<>(value, (byte) 0, 0);
    }

    /**
     * INC/DEC (IX+d)/(IY+d) — 23T R+=2.
     */
    private void runIncDecMIIplusD(Function<RunnerContext<Byte>, Integer> op, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(c -> get8MSBplus8LSB(c.first),
                        c -> op.apply(byteCtx((byte) (c.second & 0xFF))) & 0xFF)
                .verifyR(2).verifyCycles(23)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, opcode),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, opcode)
        );
    }

    /**
     * INC/DEC rr — 6T R+=1. opcodes for BC, DE, HL, SP.
     */
    private void runIncDecRP(Function<RunnerContext<Integer>, Integer> op,
                             int opBC, int opDE, int opHL, int opSP) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyR(1).verifyCycles(6)
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitUnary(
                test.firstIsPair(REG_PAIR_BC).verifyPair(REG_PAIR_BC, op).run(opBC),
                test.firstIsPair(REG_PAIR_DE).verifyPair(REG_PAIR_DE, op).run(opDE),
                test.firstIsPair(REG_PAIR_HL).verifyPair(REG_PAIR_HL, op).run(opHL),
                test.firstIsPair(REG_SP).verifyPair(REG_SP, op).run(opSP)
        );
    }

    /**
     * INC/DEC IX/IY — 10T R+=2. opcode is 0x23 for INC, 0x2B for DEC.
     */
    private void runIncDecII16(Function<RunnerContext<Integer>, Integer> op, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyR(2).verifyCycles(10)
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitUnary(
                test.firstIsIX().verifyIX(op).run(0xDD, opcode),
                test.firstIsIY().verifyIY(op).run(0xFD, opcode)
        );
    }

    /**
     * ADD IX,rr / ADD IY,rr — 15T R+=2.
     */
    private void runAddII(boolean ix) {
        int prefix = ix ? 0xDD : 0xFD;
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .verifyR(2).verifyCycles(15)
                .keepCurrentInjectorsAfterRun();
        if (ix) {
            test = test.firstIsIX().verifyIX(c -> c.first + c.second);
        } else {
            test = test.firstIsIY().verifyIY(c -> c.first + c.second);
        }
        Generator.forSome16bitBinaryWhichEqual(test.run(prefix, 0x29));
        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(prefix, 0x09),
                test.secondIsPair(REG_PAIR_DE).run(prefix, 0x19),
                test.secondIsPair(REG_SP).run(prefix, 0x39)
        );
    }
}

