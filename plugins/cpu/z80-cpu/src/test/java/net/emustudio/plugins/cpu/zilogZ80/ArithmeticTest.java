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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.FlagsCheckImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.get8MSBplus8LSB;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

public class ArithmeticTest extends InstructionsTest {

    private ByteTestBuilder additionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
            .keepCurrentInjectorsAfterRun();
    }

    private ByteTestBuilder subtractionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
            .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testADD_A__r() {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
            test.run(0x87)
        );
        Generator.forSome8bitBinary(
            test.secondIsRegister(REG_B).run(0x80),
            test.secondIsRegister(REG_C).run(0x81),
            test.secondIsRegister(REG_D).run(0x82),
            test.secondIsRegister(REG_E).run(0x83),
            test.secondIsRegister(REG_H).run(0x84),
            test.secondIsRegister(REG_L).run(0x85),
            test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x86)
        );
    }

    @Test
    public void testADD_A__n() {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xC6)
        );
    }

    @Test
    public void testADC_A__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
            test.run(0x8F)
        );
        Generator.forSome8bitBinary(
            test.secondIsRegister(REG_B).run(0x88),
            test.secondIsRegister(REG_C).run(0x89),
            test.secondIsRegister(REG_D).run(0x8A),
            test.secondIsRegister(REG_E).run(0x8B),
            test.secondIsRegister(REG_H).run(0x8C),
            test.secondIsRegister(REG_L).run(0x8D),
            test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x8E)
        );
    }

    @Test
    public void testADC_A__n() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsReset());

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xCE)
        );
    }

    @Test
    public void testSUB_A__r() {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
            test.run(0x97)
        );
        Generator.forSome8bitBinary(
            test.secondIsRegister(REG_B).run(0x90),
            test.secondIsRegister(REG_C).run(0x91),
            test.secondIsRegister(REG_D).run(0x92),
            test.secondIsRegister(REG_E).run(0x93),
            test.secondIsRegister(REG_H).run(0x94),
            test.secondIsRegister(REG_L).run(0x95),
            test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
        );
    }

    @Test
    public void testSUB_A__n() {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xD6)
        );
    }

    @Test
    public void testSBC_A__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
            test.run(0x9F)
        );
        Generator.forSome8bitBinary(
            test.secondIsRegister(REG_B).run(0x98),
            test.secondIsRegister(REG_C).run(0x99),
            test.secondIsRegister(REG_D).run(0x9A),
            test.secondIsRegister(REG_E).run(0x9B),
            test.secondIsRegister(REG_H).run(0x9C),
            test.secondIsRegister(REG_L).run(0x9D),
            test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x9E)
        );
    }

    @Test
    public void testSBC_A__n() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().carry().halfCarry().overflow().subtractionIsSet());

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xDE)
        );
    }

    @Test
    public void testINC__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().overflow().halfCarry().subtractionIsReset(),
                context -> context.first + 1)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(REG_B).verifyRegister(REG_B).run(0x04),
            test.firstIsRegister(REG_C).verifyRegister(REG_C).run(0x0C),
            test.firstIsRegister(REG_D).verifyRegister(REG_D).run(0x14),
            test.firstIsRegister(REG_E).verifyRegister(REG_E).run(0x1C),
            test.firstIsRegister(REG_H).verifyRegister(REG_H).run(0x24),
            test.firstIsRegister(REG_L).verifyRegister(REG_L).run(0x2C),
            test.firstIsRegister(REG_A).verifyRegister(REG_A).run(0x3C),
            test.firstIsMemoryByteAt(1).setPair(REG_PAIR_HL, 1).verifyByte(1).run(0x34)
        );
    }

    @Test
    public void testDEC__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().overflow().halfCarry().subtractionIsSet(),
                context -> context.first - 1)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(REG_B).verifyRegister(REG_B).run(0x05),
            test.firstIsRegister(REG_C).verifyRegister(REG_C).run(0x0D),
            test.firstIsRegister(REG_D).verifyRegister(REG_D).run(0x15),
            test.firstIsRegister(REG_E).verifyRegister(REG_E).run(0x1D),
            test.firstIsRegister(REG_H).verifyRegister(REG_H).run(0x25),
            test.firstIsRegister(REG_L).verifyRegister(REG_L).run(0x2D),
            test.firstIsRegister(REG_A).verifyRegister(REG_A).run(0x3D),
            test.firstIsMemoryByteAt(1).setPair(REG_PAIR_HL, 1).verifyByte(1).run(0x35)
        );
    }

    @Test
    public void testINC__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
            test.firstIsPair(REG_PAIR_BC).verifyPair(REG_PAIR_BC, verifier).run(0x03),
            test.firstIsPair(REG_PAIR_DE).verifyPair(REG_PAIR_DE, verifier).run(0x13),
            test.firstIsPair(REG_PAIR_HL).verifyPair(REG_PAIR_HL, verifier).run(0x23),
            test.firstIsPair(REG_SP).verifyPair(REG_SP, verifier).run(0x33)
        );
    }

    @Test
    public void testDEC__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
            test.firstIsPair(REG_PAIR_BC).verifyPair(REG_PAIR_BC, verifier).run(0x0B),
            test.firstIsPair(REG_PAIR_DE).verifyPair(REG_PAIR_DE, verifier).run(0x1B),
            test.firstIsPair(REG_PAIR_HL).verifyPair(REG_PAIR_HL, verifier).run(0x2B),
            test.firstIsPair(REG_SP).verifyPair(REG_SP, verifier).run(0x3B)
        );
    }

    @Test
    public void testADD_HL__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_PAIR_HL)
            .verifyPair(REG_PAIR_HL, context -> context.first + context.second)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .sign16bit().zero16bit().carry15().halfCarry11().subtractionIsReset()
            )
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

    /* 8080-incompatible */

    @Test
    public void testADD_A__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8LSBisRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x86),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x86)
        );
    }

    @Test
    public void testADC_A__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8LSBisRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x8E),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x8E)
        );
    }

    @Test
    public void testSUB_A__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8LSBisRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x96),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x96)
        );
    }

    @Test
    public void testSBC_A__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8LSBisRegister(REG_A)
            .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x9E),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x9E)
        );
    }

    @Test
    public void testINC__IX_IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(
            test.firstIsIX().verifyIX(context -> context.first + 1).run(0xDD, 0x23),
            test.firstIsIY().verifyIY(context -> context.first + 1).run(0xFD, 0x23)
        );
    }

    @Test
    public void testDEC__IX_IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(
            test.firstIsIX().verifyIX(context -> context.first - 1).run(0xDD, 0x2B),
            test.firstIsIY().verifyIY(context -> context.first - 1).run(0xFD, 0x2B)
        );
    }

    @Test
    public void testINC__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second + 1))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsReset()
            )
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x34),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x34)
        );
    }

    @Test
    public void testDEC__IX_IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second - 1))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsSet()
            )
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
            test.first8MSBisIX().runWithFirst8bitOperand(0xDD, 0x35),
            test.first8MSBisIY().runWithFirst8bitOperand(0xFD, 0x35)
        );
    }

    @Test
    public void testADD_IX__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsIX()
            .verifyIX(context -> context.first + context.second)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .sign16bit().zero16bit().halfCarry11().carry15().subtractionIsReset()
            )
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
            test.run(0xDD, 0x29)
        );

        Generator.forSome16bitBinary(
            test.secondIsPair(REG_PAIR_BC).run(0xDD, 0x09),
            test.secondIsPair(REG_PAIR_DE).run(0xDD, 0x19),
            test.secondIsPair(REG_SP).run(0xDD, 0x39)
        );
    }

    @Test
    public void testADD_IY__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsIY()
            .verifyIY(context -> context.first + context.second)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .sign16bit().zero16bit().halfCarry11().carry15().subtractionIsReset()
            )
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
            test.run(0xFD, 0x29)
        );

        Generator.forSome16bitBinary(
            test.secondIsPair(REG_PAIR_BC).run(0xFD, 0x09),
            test.secondIsPair(REG_PAIR_DE).run(0xFD, 0x19),
            test.secondIsPair(REG_SP).run(0xFD, 0x39)
        );
    }

    @Test
    public void testADC_HL__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_PAIR_HL)
            .verifyPair(REG_PAIR_HL, context -> context.first + context.second + (context.flags & FLAG_C))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .sign16bit().zero16bit().overflow16bit().carry15().halfCarry11().subtractionIsReset())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
            test.run(0xED, 0x6A)
        );

        Generator.forSome16bitBinary(
            test.secondIsPair(REG_PAIR_BC).run(0xED, 0x4A),
            test.secondIsPair(REG_PAIR_DE).run(0xED, 0x5A),
            test.secondIsPair(REG_SP).run(0xED, 0x7A)
        );
    }

    @Test
    public void testSBC_HL__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_PAIR_HL)
            .verifyPair(REG_PAIR_HL, context ->
                (context.first + ((-context.second - (context.flags & FLAG_C)) & 0xFFFF)))
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                .sign16bit().zero16bit().overflow16bit().carry15().halfCarry11().subtractionIsSet())
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
            test.run(0xED, 0x62)
        );

        Generator.forSome16bitBinary(
            test.secondIsPair(REG_PAIR_BC).run(0xED, 0x42),
            test.secondIsPair(REG_PAIR_DE).run(0xED, 0x52),
            test.secondIsPair(REG_SP).run(0xED, 0x72)
        );
    }
}
