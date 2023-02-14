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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.Utils;
import org.junit.Test;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

public class TransferTest extends InstructionsTest {

    @Test
    public void testLD_R_R() {
        runLD_R_R_test(REG_A, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F);
        runLD_R_R_test(REG_B, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47);
        runLD_R_R_test(REG_C, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F);
        runLD_R_R_test(REG_D, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57);
        runLD_R_R_test(REG_E, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F);
        runLD_R_R_test(REG_H, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67);
        runLD_R_R_test(REG_L, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F);
    }

    @Test
    public void testLD_R_N() {
        runLD_R_N_test(REG_A, 0x3E);
        runLD_R_N_test(REG_B, 0x06);
        runLD_R_N_test(REG_C, 0x0E);
        runLD_R_N_test(REG_D, 0x16);
        runLD_R_N_test(REG_E, 0x1E);
        runLD_R_N_test(REG_H, 0x26);
        runLD_R_N_test(REG_L, 0x2E);
    }

    @Test
    public void testLD_REF_HL_N() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryByteAt(0x303)
                .setPair(REG_PAIR_HL, 0x303)
                .verifyByte(0x303, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0x36)
        );
    }

    @Test
    public void testLD_REF_HL_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryByteAt(0x303)
                .setPair(REG_PAIR_HL, 0x303)
                .verifyByte(0x303, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x70),
                test.secondIsRegister(REG_C).run(0x71),
                test.secondIsRegister(REG_D).run(0x72),
                test.secondIsRegister(REG_E).run(0x73),
                test.secondIsRegister(REG_A).run(0x77)
        );
    }

    @Test
    public void testLD_REF_HL_HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(1,
                test.verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF).run(0x74),
                test.verifyByte(context -> context.first, context -> context.first & 0xFF).run(0x75)
        );
    }

    @Test
    public void testLD_R_REF_II_N() {
        runLD_R_REF_II_N(REG_A, 0x7E);
        runLD_R_REF_II_N(REG_B, 0x46);
        runLD_R_REF_II_N(REG_C, 0x4E);
        runLD_R_REF_II_N(REG_D, 0x56);
        runLD_R_REF_II_N(REG_E, 0x5E);
        runLD_R_REF_II_N(REG_H, 0x66);
        runLD_R_REF_II_N(REG_L, 0x6E);
    }

    @Test
    public void testLD_REF_IX_N_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .verifyByte(context -> Utils.get8MSBplus8LSB(context.first), context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8LSBisRegister(REG_A).runWithFirst8bitOperand(0xDD, 0x77),
                test.first8LSBisRegister(REG_B).runWithFirst8bitOperand(0xDD, 0x70),
                test.first8LSBisRegister(REG_C).runWithFirst8bitOperand(0xDD, 0x71),
                test.first8LSBisRegister(REG_D).runWithFirst8bitOperand(0xDD, 0x72),
                test.first8LSBisRegister(REG_E).runWithFirst8bitOperand(0xDD, 0x73),
                test.first8LSBisRegister(REG_H).runWithFirst8bitOperand(0xDD, 0x74),
                test.first8LSBisRegister(REG_L).runWithFirst8bitOperand(0xDD, 0x75)
        );
    }

    @Test
    public void testLD_REF_IY_N_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .verifyByte(context -> Utils.get8MSBplus8LSB(context.first), context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8LSBisRegister(REG_A).runWithFirst8bitOperand(0xFD, 0x77),
                test.first8LSBisRegister(REG_B).runWithFirst8bitOperand(0xFD, 0x70),
                test.first8LSBisRegister(REG_C).runWithFirst8bitOperand(0xFD, 0x71),
                test.first8LSBisRegister(REG_D).runWithFirst8bitOperand(0xFD, 0x72),
                test.first8LSBisRegister(REG_E).runWithFirst8bitOperand(0xFD, 0x73),
                test.first8LSBisRegister(REG_H).runWithFirst8bitOperand(0xFD, 0x74),
                test.first8LSBisRegister(REG_L).runWithFirst8bitOperand(0xFD, 0x75)
        );
    }

    @Test
    public void testLD_REF_II_N_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(context -> Utils.get8MSBplus8LSB(context.first), context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandTwoTimes(0xDD, 0x36),
                test.first8MSBisIY().runWithFirst8bitOperandTwoTimes(0xFD, 0x36)
        );
    }

    @Test
    public void testLD_A_REF_RP() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .verifyRegister(REG_A, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(1,
                test.firstIsPair(REG_PAIR_BC).run(0x0A),
                test.firstIsPair(REG_PAIR_DE).run(0x1A)
        );
    }

    @Test
    public void testLD_A_REF_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .verifyRegister(REG_A, context -> context.second & 0xFF);

        Generator.forSome16bitBinary(3,
                test.runWithFirstOperand(0x3A)
        );
    }

    @Test
    public void testLD_REF_RP_A() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyByte(context -> context.first, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(1,
                test.firstIsPair(REG_PAIR_BC).run(0x02),
                test.firstIsPair(REG_PAIR_DE).run(0x12)
        );
    }

    @Test
    public void testLD_REF_NN_A() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyByte(context -> context.first, context -> context.first & 0xFF);

        Generator.forSome16bitBinary(
                test.runWithFirstOperand(0x32)
        );
    }

    @Test
    public void testLD_A_I() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsRegisterI()
                .setFlags(FLAG_H | FLAG_N)
                .verifyRegister(REG_A, context -> context.second & 0xFF);

        Generator.forSome8bitBinary(
                test.run(0xED, 0x57)
        );
    }

    @Test
    public void testLD_A_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsRegisterR()
                .setFlags(FLAG_H | FLAG_N)
                .verifyRegister(REG_A, context -> (context.second & 0x80) | ((context.second & 0x7F) + 2) & 0x7F);

        Generator.forSome8bitBinary(
                test.run(0xED, 0x5F)
        );
    }

    @Test
    public void testLD_I_A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegisterI()
                .secondIsRegister(REG_A)
                .verifyRegisterI(context -> context.second & 0xFF);

        Generator.forSome8bitBinary(
                test.run(0xED, 0x47)
        );
    }

    @Test
    public void testLD_R_A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegisterR()
                .secondIsRegister(REG_A)
                .verifyRegisterR(context -> context.second & 0xFF);

        Generator.forSome8bitBinary(
                test.run(0xED, 0x4F)
        );
    }

    @Test
    public void testLD_RP_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, context -> context.first).runWithFirstOperand(0x01),
                test.verifyPair(REG_PAIR_DE, context -> context.first).runWithFirstOperand(0x11),
                test.verifyPair(REG_PAIR_HL, context -> context.first).runWithFirstOperand(0x21),
                test.verifyPair(REG_SP, context -> context.first).runWithFirstOperand(0x31)
        );
    }

    @Test
    public void testLD_II_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(
                test.verifyIX(context -> context.first).runWithFirstOperand(0xDD, 0x21),
                test.verifyIY(context -> context.first).runWithFirstOperand(0xFD, 0x21)
        );
    }

    @Test
    public void testLD_HL_REF_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyPair(REG_PAIR_HL, context -> context.second);

        Generator.forSome16bitBinary(3,
                test.runWithFirstOperand(0x2A)
        );
    }

    @Test
    public void testLD_RP_REF_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(4,
                test.verifyPair(REG_PAIR_BC, context -> context.second).runWithFirstOperand(0xED, 0x4B),
                test.verifyPair(REG_PAIR_DE, context -> context.second).runWithFirstOperand(0xED, 0x5B),
                test.verifyPair(REG_PAIR_HL, context -> context.second).runWithFirstOperand(0xED, 0x6B),
                test.verifyPair(REG_SP, context -> context.second).runWithFirstOperand(0xED, 0x7B)
        );
    }

    @Test
    public void testLD_II_REF_NN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(4,
                test.verifyIX(context -> context.second).runWithFirstOperand(0xDD, 0x2A),
                test.verifyIY(context -> context.second).runWithFirstOperand(0xFD, 0x2A)
        );
    }

    @Test
    public void testLD_REF_NN_HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_PAIR_HL)
                .verifyWord(context -> context.first, context -> context.first);

        Generator.forSome16bitBinary(3,
                test.runWithFirstOperand(0x22)
        );
    }

    @Test
    public void testLD_REF_NN_RP() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyWord(context -> context.first, context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(4,
                test.firstIsPair(REG_PAIR_BC).runWithFirstOperand(0xED, 0x43),
                test.firstIsPair(REG_PAIR_DE).runWithFirstOperand(0xED, 0x53),
                test.firstIsPair(REG_PAIR_HL).runWithFirstOperand(0xED, 0x63),
                test.firstIsPair(REG_SP).runWithFirstOperand(0xED, 0x73)
        );
    }

    @Test
    public void testLD_REF_NN_II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyWord(context -> context.first, context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(4,
                test.firstIsIX().runWithFirstOperand(0xDD, 0x22),
                test.firstIsIY().runWithFirstOperand(0xFD, 0x22)
        );
    }

    @Test
    public void testLD_SP_HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitUnary(
                test.run(0xF9)
        );
    }

    @Test
    public void testLD_SP_II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_SP, context -> context.first)
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(
                test.firstIsIX().run(0xDD, 0xF9),
                test.firstIsIY().run(0xFD, 0xF9)
        );
    }

    @Test
    public void testEX_DE_HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_DE, context -> context.second)
                .verifyPair(REG_PAIR_HL, context -> context.first);

        Generator.forSome16bitBinary(
                test.run(0xEB)
        );
    }

    @Test
    public void testEX_AF_AF2() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAF()
                .secondIsAF2()
                .verifyAF(context -> context.second)
                .verifyAF2(context -> context.first);

        Generator.forSome16bitBinary(
                test.run(0x08)
        );
    }

    @Test
    public void testEXX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_BC)
                .firstIsPair(REG_PAIR_DE)
                .firstIsPair(REG_PAIR_HL)
                .secondIsPair2(REG_PAIR_BC)
                .secondIsPair2(REG_PAIR_DE)
                .secondIsPair2(REG_PAIR_HL)
                .verifyPair(REG_PAIR_BC, context -> context.second)
                .verifyPair(REG_PAIR_DE, context -> context.second)
                .verifyPair(REG_PAIR_HL, context -> context.second)
                .verifyPair2(REG_PAIR_BC, context -> context.first)
                .verifyPair2(REG_PAIR_DE, context -> context.first)
                .verifyPair2(REG_PAIR_HL, context -> context.first);

        Generator.forSome16bitBinary(
                test.run(0xD9)
        );
    }

    @Test
    public void testEX_REF_SP_HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, context -> context.second)
                .verifyWord(context -> context.first, context -> context.first);

        Generator.forSome16bitBinary(1,
                test.run(0xE3)
        );
    }

    @Test
    public void testEX_REF_SP_II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyWord(context -> context.first, context -> context.first)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(2,
                test.firstIsIX().verifyIX(context -> context.second).run(0xDD, 0xE3),
                test.firstIsIY().verifyIY(context -> context.second).run(0xFD, 0xE3)
        );

    }

    @Test
    public void testLDI() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .secondIsAddressAndFirstIsMemoryByte()
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .firstIsPair(REG_PAIR_BC)
                .verifyByte(context -> context.first, context -> context.first & 0xFF)
                .verifyPair(REG_PAIR_DE, context -> (context.first + 1) & 0xFFFF)
                .verifyPair(REG_PAIR_HL, context -> (context.second + 1) & 0xFFFF)
                .verifyPair(REG_PAIR_BC, context -> (context.first - 1) & 0xFFFF);

        Generator.forSome16bitBinary(2, 2,
                test.run(0xED, 0xA0)
        );
    }

    @Test
    public void testLDIR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .secondIsAddressAndFirstIsMemoryByte()
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .firstIsPair(REG_PAIR_BC)
                .verifyByte(context -> context.first, context -> context.first & 0xFF)
                .verifyPair(REG_PAIR_DE, context -> (context.first + 1) & 0xFFFF)
                .verifyPair(REG_PAIR_HL, context -> (context.second + 1) & 0xFFFF)
                .verifyPair(REG_PAIR_BC, context -> (context.first - 1) & 0xFFFF)
                .verifyR(context -> 2)
                .verifyPC(context -> {
                    if (((context.first - 1) & 0xFFFF) != 0) {
                        return context.PC;
                    }
                    return (context.PC + 2) & 0xFFFF;
                });

        Generator.forSome16bitBinary(2, 2,
                test.run(0xED, 0xB0)
        );
    }

    @Test
    public void testLDD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .secondIsAddressAndFirstIsMemoryByte()
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .firstIsPair(REG_PAIR_BC)
                .verifyByte(context -> context.first, context -> context.first & 0xFF)
                .verifyPair(REG_PAIR_DE, context -> (context.first - 1) & 0xFFFF)
                .verifyPair(REG_PAIR_HL, context -> (context.second - 1) & 0xFFFF)
                .verifyPair(REG_PAIR_BC, context -> (context.first - 1) & 0xFFFF);

        Generator.forSome16bitBinary(2, 2,
                test.run(0xED, 0xA8)
        );
    }

    @Test
    public void testLDDR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .secondIsAddressAndFirstIsMemoryByte()
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .firstIsPair(REG_PAIR_BC)
                .verifyByte(context -> context.first, context -> context.first & 0xFF)
                .verifyPair(REG_PAIR_DE, context -> (context.first - 1) & 0xFFFF)
                .verifyPair(REG_PAIR_HL, context -> (context.second - 1) & 0xFFFF)
                .verifyPair(REG_PAIR_BC, context -> (context.first - 1) & 0xFFFF)
                .verifyPC(context -> {
                    if (((context.first - 1) & 0xFFFF) != 0) {
                        return context.PC;
                    }
                    return (context.PC + 2) & 0xFFFF;
                });

        Generator.forSome16bitBinary(2, 2,
                test.run(0xED, 0xB8)
        );
    }

    @Test
    public void testLD_IXH_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIX()
                .verifyIX(context -> (context.second & 0xFF) | ((context.first << 8) & 0xFF00));

        Generator.forSome16bitBinary(
                test.runWithFirst8bitOperand(0xDD, 0x26)
        );
    }

    @Test
    public void testLD_IYH_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIY()
                .verifyIY(context -> (context.second & 0xFF) | ((context.first << 8) & 0xFF00));

        Generator.forSome16bitBinary(
                test.runWithFirst8bitOperand(0xFD, 0x26)
        );
    }

    @Test
    public void testLD_IXL_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIX()
                .verifyIX(context -> (context.second & 0xFF00) | (context.first & 0xFF));

        Generator.forSome16bitBinary(
                test.runWithFirst8bitOperand(0xDD, 0x2E)
        );
    }

    @Test
    public void testLD_IYL_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIY()
                .verifyIY(context -> (context.second & 0xFF00) | (context.first & 0xFF));

        Generator.forSome16bitBinary(
                test.runWithFirst8bitOperand(0xFD, 0x2E)
        );
    }

    @Test
    public void testLD_R_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x44}, {REG_C, 0x4C}, {REG_D, 0x54}, {REG_E, 0x5C}, {REG_A, 0x7C}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIX()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyRegister(regOpcode[0], context -> (context.second >>> 8) & 0xFF);

            Generator.forSome16bitBinary(
                    test.run(0xDD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_R_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x45}, {REG_C, 0x4D}, {REG_D, 0x55}, {REG_E, 0x5D}, {REG_A, 0x7D}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIX()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyRegister(regOpcode[0], context -> context.second & 0xFF);

            Generator.forSome16bitBinary(
                    test.run(0xDD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_R_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x44}, {REG_C, 0x4C}, {REG_D, 0x54}, {REG_E, 0x5C}, {REG_A, 0x7C}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIY()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyRegister(regOpcode[0], context -> (context.second >>> 8) & 0xFF);

            Generator.forSome16bitBinary(
                    test.run(0xFD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_R_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x45}, {REG_C, 0x4D}, {REG_D, 0x55}, {REG_E, 0x5D}, {REG_A, 0x7D}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIY()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyRegister(regOpcode[0], context -> context.second & 0xFF);

            Generator.forSome16bitBinary(
                    test.run(0xFD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_IXH_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x60}, {REG_C, 0x61}, {REG_D, 0x62}, {REG_E, 0x63}, {REG_A, 0x67}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIX()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyIX(context -> (context.second & 0xFF) | ((context.first << 8) & 0xFF00));

            Generator.forSome16bitBinary(
                    test.run(0xDD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_IYH_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x60}, {REG_C, 0x61}, {REG_D, 0x62}, {REG_E, 0x63}, {REG_A, 0x67}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIY()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyIY(context -> (context.second & 0xFF) | ((context.first << 8) & 0xFF00));

            Generator.forSome16bitBinary(
                    test.run(0xFD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_IXH_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyIX(context -> ((context.first << 8) & 0xFF00) | (context.first & 0xFF));

        Generator.forSome16bitBinary(
                test.run(0xDD, 0x65)
        );
    }

    @Test
    public void testLD_IYH_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyIY(context -> ((context.first << 8) & 0xFF00) | (context.first & 0xFF));

        Generator.forSome16bitBinary(
                test.run(0xFD, 0x65)
        );
    }

    @Test
    public void testLD_IXL_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x68}, {REG_C, 0x69}, {REG_D, 0x6A}, {REG_E, 0x6B}, {REG_A, 0x6F}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIX()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyIX(context -> (context.second & 0xFF00) | (context.first & 0xFF));

            Generator.forSome16bitBinary(
                    test.run(0xDD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_IYL_R() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        int[][] regOpcodes = {{REG_B, 0x68}, {REG_C, 0x69}, {REG_D, 0x6A}, {REG_E, 0x6B}, {REG_A, 0x6F}};

        for (int[] regOpcode : regOpcodes) {
            test.secondIsIY()
                    .first8LSBisRegister(regOpcode[0])
                    .verifyIY(context -> (context.second & 0xFF00) | (context.first & 0xFF));

            Generator.forSome16bitBinary(
                    test.run(0xFD, regOpcode[1])
            );
        }
    }

    @Test
    public void testLD_IXL_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyIX(context -> (context.first >>> 8) | (context.first & 0xFF00));

        Generator.forSome16bitBinary(
                test.run(0xDD, 0x6C)
        );
    }

    @Test
    public void testLD_IYL_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyIY(context -> (context.first >>> 8) | (context.first & 0xFF00));

        Generator.forSome16bitBinary(
                test.run(0xFD, 0x6C)
        );
    }

    private void runLD_R_R_test(int register, int... opcodes) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(register)
                .verifyRegister(register, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(opcodes[0]),
                test.secondIsRegister(REG_C).run(opcodes[1]),
                test.secondIsRegister(REG_D).run(opcodes[2]),
                test.secondIsRegister(REG_E).run(opcodes[3]),
                test.secondIsRegister(REG_H).run(opcodes[4]),
                test.secondIsRegister(REG_L).run(opcodes[5]),
                test.secondIsMemoryByteAt(0x303).setPair(REG_PAIR_HL, 0x303).run(opcodes[6]),
                test.secondIsRegister(REG_A).run(opcodes[7])
        );

    }

    private void runLD_R_N_test(int register, int opcode) {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(register)
                .verifyRegister(register, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(opcode)
        );
    }

    private void runLD_R_REF_II_N(int register, int opcode) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(register)
                .verifyRegister(register, context -> context.second & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, opcode),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, opcode)
        );
    }
}
