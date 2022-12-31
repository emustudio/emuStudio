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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.intel8080.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.intel8080.suite.FlagsCheckImpl;
import net.emustudio.plugins.cpu.intel8080.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

public class ArithmeticTest extends InstructionsTest {

    private ByteTestBuilder additionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();
    }

    private ByteTestBuilder subtractionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testADD() {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x87)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0x80),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0x81),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0x82),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0x83),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0x84),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0x85),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x86)
        );
    }

    @Test
    public void testADI() {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xC6)
        );
    }

    @Test
    public void testADC() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x8F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0x88),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0x89),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0x8A),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0x8B),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0x8C),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0x8D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x8E)
        );
    }

    @Test
    public void testACI() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity());

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xCE)
        );
    }

    @Test
    public void testSUB() {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x97)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0x90),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0x91),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0x92),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0x93),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0x94),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0x95),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
        );
    }

    @Test
    public void testSUI() {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xD6)
        );
    }

    @Test
    public void testSBB() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x9F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0x98),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0x99),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0x9A),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0x9B),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0x9C),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0x9D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x9E)
        );
    }

    @Test
    public void testSBI() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity());

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xDE)
        );
    }

    @Test
    public void testINR() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().parity().auxCarry(), context -> context.first + 1)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(EmulatorEngine.REG_B).firstIsRegister(EmulatorEngine.REG_B).run(0x04),
                test.verifyRegister(EmulatorEngine.REG_C).firstIsRegister(EmulatorEngine.REG_C).run(0x0C),
                test.verifyRegister(EmulatorEngine.REG_D).firstIsRegister(EmulatorEngine.REG_D).run(0x14),
                test.verifyRegister(EmulatorEngine.REG_E).firstIsRegister(EmulatorEngine.REG_E).run(0x1C),
                test.verifyRegister(EmulatorEngine.REG_H).firstIsRegister(EmulatorEngine.REG_H).run(0x24),
                test.verifyRegister(EmulatorEngine.REG_L).firstIsRegister(EmulatorEngine.REG_L).run(0x2C),
                test.verifyRegister(EmulatorEngine.REG_A).firstIsRegister(EmulatorEngine.REG_A).run(0x3C),
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x34)
        );
    }

    @Test
    public void testDCR() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().parity().auxCarry(), context -> context.first - 1)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(EmulatorEngine.REG_B).firstIsRegister(EmulatorEngine.REG_B).run(0x05),
                test.verifyRegister(EmulatorEngine.REG_C).firstIsRegister(EmulatorEngine.REG_C).run(0x0D),
                test.verifyRegister(EmulatorEngine.REG_D).firstIsRegister(EmulatorEngine.REG_D).run(0x15),
                test.verifyRegister(EmulatorEngine.REG_E).firstIsRegister(EmulatorEngine.REG_E).run(0x1D),
                test.verifyRegister(EmulatorEngine.REG_H).firstIsRegister(EmulatorEngine.REG_H).run(0x25),
                test.verifyRegister(EmulatorEngine.REG_L).firstIsRegister(EmulatorEngine.REG_L).run(0x2D),
                test.verifyRegister(EmulatorEngine.REG_A).firstIsRegister(EmulatorEngine.REG_A).run(0x3D),
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x35)
        );
    }

    @Test
    public void testINX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x03),
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x13),
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x23),
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x33)
        );
    }

    @Test
    public void testDCX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x0B),
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x1B),
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x2B),
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x3B)
        );
    }

    @Test
    public void testDAD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().carry15())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0x29)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0x09),
                test.secondIsPair(REG_PAIR_DE).run(0x19),
                test.secondIsPair(REG_SP).run(0x39)
        );
    }
}
