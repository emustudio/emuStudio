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
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.get8MSBplus8LSB;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

public class LogicTest extends InstructionsTest {

    @Test
    public void testAND_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & context.second)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xA7)
        );

        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0xA0),
                test.secondIsRegister(REG_C).run(0xA1),
                test.secondIsRegister(REG_D).run(0xA2),
                test.secondIsRegister(REG_E).run(0xA3),
                test.secondIsRegister(REG_H).run(0xA4),
                test.secondIsRegister(REG_L).run(0xA5),
                test.secondIsMemoryByteAt(0x0320).setPair(REG_PAIR_HL, 0x0320).run(0xA6)
        );
    }

    @Test
    public void testAND_N() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & context.second)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xE6)
        );
    }

    @Test
    public void testOR_R() {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first | context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xB7)
        );

        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0xB0),
                test.secondIsRegister(REG_C).run(0xB1),
                test.secondIsRegister(REG_D).run(0xB2),
                test.secondIsRegister(REG_E).run(0xB3),
                test.secondIsRegister(REG_H).run(0xB4),
                test.secondIsRegister(REG_L).run(0xB5),
                test.secondIsMemoryByteAt(0x0320).setPair(REG_PAIR_HL, 0x0320).run(0xB6)
        );
    }

    @Test
    public void testOR_N() {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first | context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xF6)
        );
    }

    @Test
    public void testXOR_R() {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first ^ context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xAF)
        );

        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0xA8),
                test.secondIsRegister(REG_C).run(0xA9),
                test.secondIsRegister(REG_D).run(0xAA),
                test.secondIsRegister(REG_E).run(0xAB),
                test.secondIsRegister(REG_H).run(0xAC),
                test.secondIsRegister(REG_L).run(0xAD),
                test.secondIsMemoryByteAt(0x0320).setPair(REG_PAIR_HL, 0x0320).run(0xAE)
        );
    }

    @Test
    public void testXOR_N() {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first ^ context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xEE)
        );
    }

    @Test
    public void testCP_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xBF)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0xB8),
                test.secondIsRegister(REG_C).run(0xB9),
                test.secondIsRegister(REG_D).run(0xBA),
                test.secondIsRegister(REG_E).run(0xBB),
                test.secondIsRegister(REG_H).run(0xBC),
                test.secondIsRegister(REG_L).run(0xBD),
                test.setPair(REG_PAIR_HL, 0x0320).secondIsMemoryByteAt(0x0320).run(0xBE)
        );
    }

    @Test
    public void testCP_N() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xFE)
        );
    }

    @Test
    public void testDAA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsFlags()
                .keepCurrentInjectorsAfterRun();

        int[][] params = new int[][]{
                new int[]{0xAA, 0x7D, 0x10, 0x11},
                new int[]{0xAA, 0x7E, 0x44, 0x7},
                new int[]{0xAA, 0x7F, 0x44, 0x7},
                new int[]{0xAD, 0x7E, 0x47, 0x7},
                new int[]{0xAD, 0x7F, 0x47, 0x7},
                new int[]{0xAC, 0x6C, 0x12, 0x15},
                new int[]{0xA6, 0x7E, 0x40, 0x3},
                new int[]{0xA6, 0x7F, 0x40, 0x3},
                new int[]{0xA1, 0x6C, 0x1, 0x1},
                new int[]{0xA1, 0x6D, 0x1, 0x1},
                new int[]{0xB8, 0x6F, 0x58, 0xB},
                new int[]{0xB8, 0x7C, 0x1E, 0xD},
                new int[]{0xB8, 0x7D, 0x1E, 0xD},
                new int[]{0xB8, 0x7E, 0x52, 0x3},
                new int[]{0x95, 0xE4, 0x95, 0x84},
                new int[]{0x95, 0xE5, 0xF5, 0xA5},
                new int[]{0x95, 0xE6, 0x95, 0x86},
                new int[]{0x95, 0xE7, 0x35, 0x27},
                new int[]{0x28, 0xE9, 0x88, 0x8D},
                new int[]{0x28, 0xEA, 0x28, 0x2E},
                new int[]{0x28, 0xEB, 0xC8, 0x8B},
                new int[]{0x28, 0xF8, 0x2E, 0x2C},
                new int[]{0x53, 0xF8, 0x59, 0xC},
                new int[]{0x53, 0xF9, 0xB9, 0xA9},
                new int[]{0x53, 0xFA, 0x4D, 0x1E},
                new int[]{0x53, 0xFB, 0xED, 0xBF},
                new int[]{0xEF, 0xEF, 0x89, 0x8B},
                new int[]{0xEF, 0xFC, 0x55, 0x15},
                new int[]{0xEF, 0xFD, 0x55, 0x15},
                new int[]{0xEF, 0xFE, 0x89, 0x8B},
                new int[]{0x20, 0xFD, 0x86, 0x81},
                new int[]{0x20, 0xFE, 0x1A, 0x1A},
                new int[]{0x20, 0xFF, 0xBA, 0xBB},
                new int[]{0x1C, 0xEF, 0xB6, 0xA3},
                new int[]{0x1C, 0xFC, 0x22, 0x34},
                new int[]{0x1C, 0xFD, 0x82, 0x95},
                new int[]{0x16, 0xFF, 0xB0, 0xA3},
                new int[]{0x11, 0xEC, 0x11, 0x4},
                new int[]{0x11, 0xED, 0x71, 0x25},
                new int[]{0x11, 0xEE, 0x11, 0x6},
                new int[]{0x55, 0xEC, 0x55, 0x4},
                new int[]{0x55, 0xED, 0xB5, 0xA1},
                new int[]{0x55, 0xEE, 0x55, 0x6},
        };

        for (int[] p : params) {
            test.verifyRegister(REG_A, c -> p[2])
                    .run(0x27)
                    .accept((byte) p[0], (byte) p[1]);
        }
    }

    @Test
    public void testCPL() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> ~context.first);

        Generator.forSome8bitUnary(
                test.run(0x2F)
        );
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
                .verifyRegister(REG_A, context -> ((context.first << 1) & 0xFF) | (context.first >>> 7) & 1);

        Generator.forSome8bitUnary(
                test.run(0x07)
        );
    }

    @Test
    public void testRRCA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, context -> (((context.first & 0xFF) >>> 1) | (context.first << 7)) & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x0F)
        );
    }

    @Test
    public void testRLA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, context -> ((context.first << 1) & 0xFE) | (context.flags & 1));

        Generator.forSome8bitUnary(
                test.run(0x17)
        );
    }

    @Test
    public void testRRA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .setFlags(FLAG_N | FLAG_H)
                .verifyRegister(REG_A, context -> ((context.first >> 1) & 0x7F) | ((context.flags & 1) << 7));

        Generator.forSome8bitUnary(
                test.run(0x1F)
        );
    }

    @Test
    public void testCPI() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) + 1));

        Generator.forSome16bitBinary(3,
                test.run(0xED, 0xA1)
        );
    }

    @Test
    public void testCPIR() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) + 1))
                .verifyPC(context -> {
                    boolean regAzero = (context.registers.get(REG_A) == (context.second & 0xFF));
                    boolean BCzero = ((context.getRegister(REG_B) << 8 | context.getRegister(REG_C)) - 1) == 0;
                    if (regAzero || BCzero) {
                        return context.PC + 2;
                    }
                    return context.PC;
                });

        Generator.forSome16bitBinary(3,
                test.run(0xED, 0xB1)
        );
    }

    @Test
    public void testCPD() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) - 1));

        Generator.forSome16bitBinary(3,
                test.run(0xED, 0xA9)
        );
    }

    @Test
    public void testCPDR() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) - 1))
                .verifyPC(context -> {
                    boolean regAzero = (context.registers.get(REG_A) == (context.second & 0xFF));
                    boolean BCzero = ((context.getRegister(REG_B) << 8 | context.getRegister(REG_C)) - 1) == 0;
                    if (regAzero || BCzero) {
                        return context.PC + 2;
                    }
                    return context.PC;
                });

        Generator.forSome16bitBinary(3,
                test.run(0xED, 0xB9)
        );
    }

    @Test
    public void testAND_REF_II_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) & (context.second & 0xFF))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0xA6),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0xA6)
        );
    }

    @Test
    public void testOR_REF_II_N() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) | (context.second & 0xFF))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0xB6),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0xB6)
        );
    }

    @Test
    public void testXOR_REF_II_N() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) ^ (context.second & 0xFF))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0xAE),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0xAE)
        );
    }

    @Test
    public void testCP_REF_II_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0xBE),
                test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0xBE)
        );
    }

    @Test
    public void testNEG() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (-context.first) & 0xFF)
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

    @Test
    public void testRLC_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context ->
                ((context.first << 1) & 0xFF) | (context.first >>> 7) & 1;

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x07),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x00),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x01),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x02),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x03),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x04),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x05),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x06)
        );
    }

    @Test
    public void testRLC_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationMSBTest(
                context -> ((context.second << 1) & 0xFF) | (context.second >>> 7) & 1
        ).keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(6, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(6, 0xFD, 0xCB)
        );
    }

    @Test
    public void testRLC_REF_II_N_R() {
        Function<RunnerContext<Integer>, Integer> operation =
                context -> ((context.second << 1) & 0xFF) | (context.second >>> 7) & 1;

        IntegerTestBuilder test = prepareIIRotationMSBTest(operation)
                .verifyRegister(REG_B, operation)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0, 0xFD, 0xCB)
        );
    }


    @Test
    public void testRL_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context ->
                ((context.first << 1) & 0xFF) | (context.flags & FLAG_C);

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x17),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x10),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x11),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x12),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x13),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x14),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x15),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x16)
        );
    }

    @Test
    public void testRL_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationMSBTest(
                context -> ((context.second << 1) & 0xFF) | (context.flags & FLAG_C)
        ).keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x16, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x16, 0xFD, 0xCB)
        );
    }

    @Test
    public void testRRC_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context ->
                ((context.first >>> 1) & 0x7F) | (((context.first & 1) << 7));

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x0F),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x08),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x09),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x0A),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x0B),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x0C),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x0D),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x0E)
        );
    }

    @Test
    public void testRRC_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationLSBTest(
                context -> ((context.second >>> 1) & 0x7F) | (((context.second & 1) << 7))
        ).keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x0E, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x0E, 0xFD, 0xCB)
        );
    }

    @Test
    public void testRR_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context ->
                ((context.first >> 1) & 0x7F) | ((context.flags & FLAG_C) << 7);

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x1F),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x18),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x19),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x1A),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x1B),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x1C),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x1D),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x1E)
        );
    }

    @Test
    public void testRR_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationLSBTest(
                context -> ((context.second >> 1) & 0x7F) | ((context.flags & FLAG_C) << 7)
        ).keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x1E, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x1E, 0xFD, 0xCB)
        );
    }

    @Test
    public void testSLA_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context -> (context.first << 1) & 0xFE;

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x27),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x20),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x21),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x22),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x23),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x24),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x25),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x26)
        );
    }

    @Test
    public void testSLA_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationMSBTest(context -> (context.second << 1) & 0xFE)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x26, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x26, 0xFD, 0xCB)
        );
    }

    @Test
    public void testSRA_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context -> (context.first >> 1) & 0xFF | (context.first & 0x80);

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x2F),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x28),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x29),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x2A),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x2B),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x2C),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x2D),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x2E)
        );
    }

    @Test
    public void testSRA_REF_II_N() {
        IntegerTestBuilder test = prepareIIRotationLSBTest(
                context -> ((context.second & 0xFF) >>> 1) & 0xFF | (context.second & 0x80)
        ).keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x2E, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x2E, 0xFD, 0xCB)
        );
    }

    @Test
    public void testSRL_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(FLAG_H | FLAG_N)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context -> (context.first >>> 1) & 0x7F;

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x3F),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x38),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x39),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x3A),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x3B),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x3C),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x3D),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x3E)
        );
    }

    @Test
    public void testSRL_REF_II_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(context -> get8MSBplus8LSB(context.first), context -> ((context.second & 0xFF) >>> 1) & 0xFF)
                .setFlags(0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x3E, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x3E, 0xFD, 0xCB)
        );
    }

    @Test
    public void testSLL_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Byte>, Integer> operator = context -> (context.first << 1) & 0xFF | 1;

        Generator.forSome8bitUnary(
                test.firstIsRegister(REG_A).verifyRegister(REG_A, operator).run(0xCB, 0x37),
                test.firstIsRegister(REG_B).verifyRegister(REG_B, operator).run(0xCB, 0x30),
                test.firstIsRegister(REG_C).verifyRegister(REG_C, operator).run(0xCB, 0x31),
                test.firstIsRegister(REG_D).verifyRegister(REG_D, operator).run(0xCB, 0x32),
                test.firstIsRegister(REG_E).verifyRegister(REG_E, operator).run(0xCB, 0x33),
                test.firstIsRegister(REG_H).verifyRegister(REG_H, operator).run(0xCB, 0x34),
                test.firstIsRegister(REG_L).verifyRegister(REG_L, operator).run(0xCB, 0x35),
                test.setPair(REG_PAIR_HL, 0x301).firstIsMemoryByteAt(0x301).verifyByte(0x301, operator)
                        .run(0xCB, 0x36)
        );
    }

    @Test
    public void testSLL_REF_II_N() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(
                        context -> get8MSBplus8LSB(context.first),
                        context -> ((context.second << 1) | 1) & 0xFF)
                .setFlags(0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                test.first8MSBisIX().runWithFirst8bitOperandWithOpcodeAfter(0x36, 0xDD, 0xCB),
                test.first8MSBisIY().runWithFirst8bitOperandWithOpcodeAfter(0x36, 0xFD, 0xCB)
        );
    }

    @Test
    public void testRLD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .verifyByte(context -> context.first, context -> (context.second << 4) & 0xF0 | context.first & 0x0F)
                .verifyRegister(REG_A, context -> (context.first & 0xF0) | (context.second >>> 4) & 0x0F);

        Generator.forSome16bitBinary(
                test.run(0xED, 0x6F)
        );
    }

    @Test
    public void testRRD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .verifyByte(context -> context.first, context -> (context.first << 4) & 0xF0 | (context.second >>> 4) & 0x0F)
                .verifyRegister(REG_A, context -> (context.first & 0xF0) | context.second & 0x0F);

        Generator.forSome16bitBinary(
                test.run(0xED, 0x67)
        );
    }

    @Test
    public void testAND_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) & (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xA4)
        );
    }

    @Test
    public void testAND_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) & (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xA5)
        );
    }

    @Test
    public void testAND_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) & (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xA4)
        );
    }

    @Test
    public void testAND_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) & (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xA5)
        );
    }

    @Test
    public void testXOR_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8LSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) ^ (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xAC)
        );
    }

    @Test
    public void testXOR_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8MSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) ^ (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xAD)
        );
    }

    @Test
    public void testXOR_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8LSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) ^ (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xAC)
        );
    }

    @Test
    public void testXOR_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8MSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) ^ (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xAD)
        );
    }

    @Test
    public void testOR_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8LSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) | (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xB4)
        );
    }

    @Test
    public void testOR_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8MSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) | (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xB5)
        );
    }

    @Test
    public void testOR_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8LSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) | (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xB4)
        );
    }

    @Test
    public void testOR_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .first8MSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> (context.first & 0xFF) | (context.first >>> 8));

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xB5)
        );
    }

    @Test
    public void testCP_IXH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .firstIsIX()
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xBC)
        );
    }

    @Test
    public void testCP_IYH() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .firstIsIY()
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xBC)
        );
    }

    @Test
    public void testCP_IXL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .secondIsIX()
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome16bitBinary(
                test.run(0xDD, 0xBD)
        );
    }

    @Test
    public void testCP_IYL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8LSBisRegister(REG_A)
                .secondIsIY()
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome16bitBinary(
                test.run(0xFD, 0xBD)
        );
    }

    private ByteTestBuilder getLogicTestBuilder(Function<RunnerContext<Byte>, Integer> operator) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .setFlags(0xFF)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, operator)
                .keepCurrentInjectorsAfterRun();
    }

    private IntegerTestBuilder prepareCPxTest(Function<RunnerContext<Integer>, Integer> hlOperation) {
        return new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .registerIsRandom(REG_B, 0xFF)
                .registerIsRandom(REG_C, 0xFF)
                .verifyPair(REG_PAIR_HL, hlOperation)
                .verifyPair(REG_PAIR_BC, context ->
                        ((context.getRegister(REG_B) << 8 | context.getRegister(REG_C)) - 1)
                );
    }

    private IntegerTestBuilder prepareLogicIXYtest(Function<RunnerContext<Integer>, Integer> operation) {
        return new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, operation);
    }

    private IntegerTestBuilder prepareIIRotationMSBTest(Function<RunnerContext<Integer>, Integer> operator) {
        return new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(context -> get8MSBplus8LSB(context.first), operator)
                .setFlags(FLAG_H | FLAG_N);
    }

    private IntegerTestBuilder prepareIIRotationLSBTest(Function<RunnerContext<Integer>, Integer> operator) {
        return new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .verifyByte(context -> get8MSBplus8LSB(context.first), operator)
                .setFlags(FLAG_H | FLAG_N);
    }
}
