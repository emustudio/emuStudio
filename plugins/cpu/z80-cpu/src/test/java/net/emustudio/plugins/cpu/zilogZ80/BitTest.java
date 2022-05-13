/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

public class BitTest extends InstructionsTest {

    private ByteTestBuilder prepareBITtest(int register) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(register)
            .setFlags(FLAG_N)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();
    }

    private ByteTestBuilder verifyFlags(ByteTestBuilder test, FlagsCheckImpl<Byte> flagsBuilder, int bitShift) {
        return test.verifyFlags(flagsBuilder, context -> (context.first & (1 << bitShift)));
    }

    private IntegerTestBuilder verifyFlags(IntegerTestBuilder test, FlagsCheckImpl<Integer> flagsBuilder, int bitShift) {
        return test.verifyFlags(flagsBuilder, context -> (context.second & (1 << bitShift)));
    }

    @Test
    public void testBIT_b__A() {
        ByteTestBuilder test = prepareBITtest(REG_A);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x47),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4F),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x57),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5F),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x67),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6F),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x77),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7F)
        );
    }

    @Test
    public void testBIT_b__B() {
        ByteTestBuilder test = prepareBITtest(REG_B);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x40),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x48),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x50),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x58),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x60),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x68),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x70),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x78)
        );
    }

    @Test
    public void testBIT_b__C() {
        ByteTestBuilder test = prepareBITtest(REG_C);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x41),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x49),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x51),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x59),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x61),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x69),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x71),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x79)
        );
    }

    @Test
    public void testBIT_b__D() {
        ByteTestBuilder test = prepareBITtest(REG_D);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x42),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4A),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x52),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5A),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x62),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6A),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x72),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7A)
        );
    }

    @Test
    public void testBIT_b__E() {
        ByteTestBuilder test = prepareBITtest(REG_E);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x43),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4B),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x53),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5B),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x63),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6B),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x73),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7B)
        );
    }

    @Test
    public void testBIT_b__H() {
        ByteTestBuilder test = prepareBITtest(REG_H);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x44),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4C),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x54),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5C),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x64),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6C),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x74),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7C)
        );
    }

    @Test
    public void testBIT_b__L() {
        ByteTestBuilder test = prepareBITtest(REG_L);

        FlagsCheckImpl<Byte> flagsBuilder = new FlagsCheckImpl<Byte>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Byte> signFlagBuilder = new FlagsCheckImpl<Byte>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome8bitUnary(
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x45),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4D),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x55),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5D),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x65),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6D),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x75),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7D)
        );
    }

    @Test
    public void testBIT_b__mHL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .setFlags(FLAG_N)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        FlagsCheckImpl<Integer> flagsBuilder = new FlagsCheckImpl<Integer>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Integer> signFlagBuilder = new FlagsCheckImpl<Integer>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome16bitBinary(2,
            verifyFlags(test, flagsBuilder, 0).run(0xCB, 0x46),
            verifyFlags(test, flagsBuilder, 1).run(0xCB, 0x4E),
            verifyFlags(test, flagsBuilder, 2).run(0xCB, 0x56),
            verifyFlags(test, flagsBuilder, 3).run(0xCB, 0x5E),
            verifyFlags(test, flagsBuilder, 4).run(0xCB, 0x66),
            verifyFlags(test, flagsBuilder, 5).run(0xCB, 0x6E),
            verifyFlags(test, flagsBuilder, 6).run(0xCB, 0x76),
            verifyFlags(test, signFlagBuilder, 7).run(0xCB, 0x7E)
        );
    }

    @Test
    public void testBIT_b__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8MSBisIX()
            .setFlags(FLAG_N)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        FlagsCheckImpl<Integer> flagsBuilder = new FlagsCheckImpl<Integer>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Integer> signFlagBuilder = new FlagsCheckImpl<Integer>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome16bitBinary(0x100,
            verifyFlags(test, flagsBuilder, 0).runWithFirst8bitOperandWithOpcodeAfter(0x46, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 1).runWithFirst8bitOperandWithOpcodeAfter(0x4E, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 2).runWithFirst8bitOperandWithOpcodeAfter(0x56, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 3).runWithFirst8bitOperandWithOpcodeAfter(0x5E, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 4).runWithFirst8bitOperandWithOpcodeAfter(0x66, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 5).runWithFirst8bitOperandWithOpcodeAfter(0x6E, 0xDD, 0xCB),
            verifyFlags(test, flagsBuilder, 6).runWithFirst8bitOperandWithOpcodeAfter(0x76, 0xDD, 0xCB),
            verifyFlags(test, signFlagBuilder, 7).runWithFirst8bitOperandWithOpcodeAfter(0x7E, 0xDD, 0xCB)
        );
    }

    @Test
    public void testBIT_b__IX_plus_d_undocumented() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8MSBisIX()
            .setFlags(FLAG_N)
            .verifyFlags(new FlagsCheckImpl<Integer>().halfCarryIsSet().subtractionIsReset().zero().parity().sign(),
                context -> (context.second & (1 << 7)))
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(0x100,
            test.runWithFirst8bitOperandWithOpcodeAfter(0x78, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x79, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7A, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7B, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7C, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7D, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7E, 0xDD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7F, 0xDD, 0xCB)
        );
    }

    @Test
    public void testBIT_b__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8MSBisIY()
            .setFlags(FLAG_N)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        FlagsCheckImpl<Integer> flagsBuilder = new FlagsCheckImpl<Integer>().halfCarryIsSet().subtractionIsReset().zero()
            .parity();
        FlagsCheckImpl<Integer> signFlagBuilder = new FlagsCheckImpl<Integer>().sign().halfCarryIsSet().subtractionIsReset()
            .zero().parity();
        Generator.forSome16bitBinary(0x100,
            verifyFlags(test, flagsBuilder, 0).runWithFirst8bitOperandWithOpcodeAfter(0x46, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 1).runWithFirst8bitOperandWithOpcodeAfter(0x4E, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 2).runWithFirst8bitOperandWithOpcodeAfter(0x56, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 3).runWithFirst8bitOperandWithOpcodeAfter(0x5E, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 4).runWithFirst8bitOperandWithOpcodeAfter(0x66, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 5).runWithFirst8bitOperandWithOpcodeAfter(0x6E, 0xFD, 0xCB),
            verifyFlags(test, flagsBuilder, 6).runWithFirst8bitOperandWithOpcodeAfter(0x76, 0xFD, 0xCB),
            verifyFlags(test, signFlagBuilder, 7).runWithFirst8bitOperandWithOpcodeAfter(0x7E, 0xFD, 0xCB)
        );
    }


    @Test
    public void testBIT_b__IY_plus_d_undocumented() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
            .first8MSBisIY()
            .setFlags(FLAG_N)
            .verifyFlags(new FlagsCheckImpl<Integer>().halfCarryIsSet().subtractionIsReset().zero().parity().sign(),
                context -> (context.second & (1 << 7)))
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(0x100,
            test.runWithFirst8bitOperandWithOpcodeAfter(0x78, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x79, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7A, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7B, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7C, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7D, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7E, 0xFD, 0xCB),
            test.runWithFirst8bitOperandWithOpcodeAfter(0x7F, 0xFD, 0xCB)
        );
    }

    @Test
    public void testSET_b__A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_A, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_A, context -> 1).run(0xCB, 0xC7),
            test.verifyRegister(REG_A, context -> 2).run(0xCB, 0xCF),
            test.verifyRegister(REG_A, context -> 4).run(0xCB, 0xD7),
            test.verifyRegister(REG_A, context -> 8).run(0xCB, 0xDF),
            test.verifyRegister(REG_A, context -> 16).run(0xCB, 0xE7),
            test.verifyRegister(REG_A, context -> 32).run(0xCB, 0xEF),
            test.verifyRegister(REG_A, context -> 64).run(0xCB, 0xF7),
            test.verifyRegister(REG_A, context -> 128).run(0xCB, 0xFF)
        );
    }

    @Test
    public void testSET_b__B() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_B, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_B, context -> 1).run(0xCB, 0xC0),
            test.verifyRegister(REG_B, context -> 2).run(0xCB, 0xC8),
            test.verifyRegister(REG_B, context -> 4).run(0xCB, 0xD0),
            test.verifyRegister(REG_B, context -> 8).run(0xCB, 0xD8),
            test.verifyRegister(REG_B, context -> 16).run(0xCB, 0xE0),
            test.verifyRegister(REG_B, context -> 32).run(0xCB, 0xE8),
            test.verifyRegister(REG_B, context -> 64).run(0xCB, 0xF0),
            test.verifyRegister(REG_B, context -> 128).run(0xCB, 0xF8)
        );
    }

    @Test
    public void testSET_b__C() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_C, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_C, context -> 1).run(0xCB, 0xC1),
            test.verifyRegister(REG_C, context -> 2).run(0xCB, 0xC9),
            test.verifyRegister(REG_C, context -> 4).run(0xCB, 0xD1),
            test.verifyRegister(REG_C, context -> 8).run(0xCB, 0xD9),
            test.verifyRegister(REG_C, context -> 16).run(0xCB, 0xE1),
            test.verifyRegister(REG_C, context -> 32).run(0xCB, 0xE9),
            test.verifyRegister(REG_C, context -> 64).run(0xCB, 0xF1),
            test.verifyRegister(REG_C, context -> 128).run(0xCB, 0xF9)
        );
    }

    @Test
    public void testSET_b__D() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_D, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_D, context -> 1).run(0xCB, 0xC2),
            test.verifyRegister(REG_D, context -> 2).run(0xCB, 0xCA),
            test.verifyRegister(REG_D, context -> 4).run(0xCB, 0xD2),
            test.verifyRegister(REG_D, context -> 8).run(0xCB, 0xDA),
            test.verifyRegister(REG_D, context -> 16).run(0xCB, 0xE2),
            test.verifyRegister(REG_D, context -> 32).run(0xCB, 0xEA),
            test.verifyRegister(REG_D, context -> 64).run(0xCB, 0xF2),
            test.verifyRegister(REG_D, context -> 128).run(0xCB, 0xFA)
        );
    }

    @Test
    public void testSET_b__E() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_E, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_E, context -> 1).run(0xCB, 0xC3),
            test.verifyRegister(REG_E, context -> 2).run(0xCB, 0xCB),
            test.verifyRegister(REG_E, context -> 4).run(0xCB, 0xD3),
            test.verifyRegister(REG_E, context -> 8).run(0xCB, 0xDB),
            test.verifyRegister(REG_E, context -> 16).run(0xCB, 0xE3),
            test.verifyRegister(REG_E, context -> 32).run(0xCB, 0xEB),
            test.verifyRegister(REG_E, context -> 64).run(0xCB, 0xF3),
            test.verifyRegister(REG_E, context -> 128).run(0xCB, 0xFB)
        );
    }

    @Test
    public void testSET_b__H() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_H, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_H, context -> 1).run(0xCB, 0xC4),
            test.verifyRegister(REG_H, context -> 2).run(0xCB, 0xCC),
            test.verifyRegister(REG_H, context -> 4).run(0xCB, 0xD4),
            test.verifyRegister(REG_H, context -> 8).run(0xCB, 0xDC),
            test.verifyRegister(REG_H, context -> 16).run(0xCB, 0xE4),
            test.verifyRegister(REG_H, context -> 32).run(0xCB, 0xEC),
            test.verifyRegister(REG_H, context -> 64).run(0xCB, 0xF4),
            test.verifyRegister(REG_H, context -> 128).run(0xCB, 0xFC)
        );
    }

    @Test
    public void testSET_b__L() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_L, 0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_L, context -> 1).run(0xCB, 0xC5),
            test.verifyRegister(REG_L, context -> 2).run(0xCB, 0xCD),
            test.verifyRegister(REG_L, context -> 4).run(0xCB, 0xD5),
            test.verifyRegister(REG_L, context -> 8).run(0xCB, 0xDD),
            test.verifyRegister(REG_L, context -> 16).run(0xCB, 0xE5),
            test.verifyRegister(REG_L, context -> 32).run(0xCB, 0xED),
            test.verifyRegister(REG_L, context -> 64).run(0xCB, 0xF5),
            test.verifyRegister(REG_L, context -> 128).run(0xCB, 0xFD)
        );
    }


    @Test
    public void testSET_b__mHL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsMemoryAddressByte(0)
            .firstIsPair(REG_PAIR_HL)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> context.first;
        Generator.forSome16bitUnary(2,
            test.verifyByte(address, context -> 1).run(0xCB, 0xC6),
            test.verifyByte(address, context -> 2).run(0xCB, 0xCE),
            test.verifyByte(address, context -> 4).run(0xCB, 0xD6),
            test.verifyByte(address, context -> 8).run(0xCB, 0xDE),
            test.verifyByte(address, context -> 16).run(0xCB, 0xE6),
            test.verifyByte(address, context -> 32).run(0xCB, 0xEE),
            test.verifyByte(address, context -> 64).run(0xCB, 0xF6),
            test.verifyByte(address, context -> 128).run(0xCB, 0xFE)
        );
    }

    @Test
    public void testSET_b__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIX()
            .first8MSBplus8LSBisMemoryByte(0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> get8MSBplus8LSB(context.first);
        Generator.forSome16bitUnary(0x100,
            test.verifyByte(address, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 2).runWithFirst8bitOperandWithOpcodeAfter(0xCE, 0xDD, 0xCB),
            test.verifyByte(address, context -> 4).runWithFirst8bitOperandWithOpcodeAfter(0xD6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 8).runWithFirst8bitOperandWithOpcodeAfter(0xDE, 0xDD, 0xCB),
            test.verifyByte(address, context -> 16).runWithFirst8bitOperandWithOpcodeAfter(0xE6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 32).runWithFirst8bitOperandWithOpcodeAfter(0xEE, 0xDD, 0xCB),
            test.verifyByte(address, context -> 64).runWithFirst8bitOperandWithOpcodeAfter(0xF6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 128).runWithFirst8bitOperandWithOpcodeAfter(0xFE, 0xDD, 0xCB)
        );
    }

    @Test
    public void testSET_b__IX_plus_d_undocumented() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIX()
            .first8MSBplus8LSBisMemoryByte(0)
            .verifyByte(context -> get8MSBplus8LSB(context.first), context -> 1)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(0x100,
            test.verifyRegister(REG_B, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC0, 0xDD, 0xCB),
            test.verifyRegister(REG_C, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC1, 0xDD, 0xCB),
            test.verifyRegister(REG_D, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC2, 0xDD, 0xCB),
            test.verifyRegister(REG_E, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC3, 0xDD, 0xCB),
            test.verifyRegister(REG_H, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC4, 0xDD, 0xCB),
            test.verifyRegister(REG_L, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC5, 0xDD, 0xCB),
            test.verifyRegister(REG_A, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC7, 0xDD, 0xCB)
        );
    }

    @Test
    public void testSET_b__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIY()
            .first8MSBplus8LSBisMemoryByte(0)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> get8MSBplus8LSB(context.first);
        Generator.forSome16bitUnary(0x100,
            test.verifyByte(address, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 2).runWithFirst8bitOperandWithOpcodeAfter(0xCE, 0xFD, 0xCB),
            test.verifyByte(address, context -> 4).runWithFirst8bitOperandWithOpcodeAfter(0xD6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 8).runWithFirst8bitOperandWithOpcodeAfter(0xDE, 0xFD, 0xCB),
            test.verifyByte(address, context -> 16).runWithFirst8bitOperandWithOpcodeAfter(0xE6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 32).runWithFirst8bitOperandWithOpcodeAfter(0xEE, 0xFD, 0xCB),
            test.verifyByte(address, context -> 64).runWithFirst8bitOperandWithOpcodeAfter(0xF6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 128).runWithFirst8bitOperandWithOpcodeAfter(0xFE, 0xFD, 0xCB)
        );
    }


    @Test
    public void testSET_b__IY_plus_d_undocumented() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIY()
            .first8MSBplus8LSBisMemoryByte(0)
            .verifyByte(context -> get8MSBplus8LSB(context.first), context -> 1)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(0x100,
            test.verifyRegister(REG_B, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC0, 0xFD, 0xCB),
            test.verifyRegister(REG_C, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC1, 0xFD, 0xCB),
            test.verifyRegister(REG_D, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC2, 0xFD, 0xCB),
            test.verifyRegister(REG_E, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC3, 0xFD, 0xCB),
            test.verifyRegister(REG_H, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC4, 0xFD, 0xCB),
            test.verifyRegister(REG_L, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC5, 0xFD, 0xCB),
            test.verifyRegister(REG_A, context -> 1).runWithFirst8bitOperandWithOpcodeAfter(0xC7, 0xFD, 0xCB)
        );
    }

    @Test
    public void testRES_b__A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_A, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_A, context -> 0xFE).run(0xCB, 0x87),
            test.verifyRegister(REG_A, context -> 0xFD).run(0xCB, 0x8F),
            test.verifyRegister(REG_A, context -> 0xFB).run(0xCB, 0x97),
            test.verifyRegister(REG_A, context -> 0xF7).run(0xCB, 0x9F),
            test.verifyRegister(REG_A, context -> 0xEF).run(0xCB, 0xA7),
            test.verifyRegister(REG_A, context -> 0xDF).run(0xCB, 0xAF),
            test.verifyRegister(REG_A, context -> 0xBF).run(0xCB, 0xB7),
            test.verifyRegister(REG_A, context -> 0x7F).run(0xCB, 0xBF)
        );
    }

    @Test
    public void testRES_b__B() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_B, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_B, context -> 0xFE).run(0xCB, 0x80),
            test.verifyRegister(REG_B, context -> 0xFD).run(0xCB, 0x88),
            test.verifyRegister(REG_B, context -> 0xFB).run(0xCB, 0x90),
            test.verifyRegister(REG_B, context -> 0xF7).run(0xCB, 0x98),
            test.verifyRegister(REG_B, context -> 0xEF).run(0xCB, 0xA0),
            test.verifyRegister(REG_B, context -> 0xDF).run(0xCB, 0xA8),
            test.verifyRegister(REG_B, context -> 0xBF).run(0xCB, 0xB0),
            test.verifyRegister(REG_B, context -> 0x7F).run(0xCB, 0xB8)
        );
    }

    @Test
    public void testRES_b__C() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_C, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_C, context -> 0xFE).run(0xCB, 0x81),
            test.verifyRegister(REG_C, context -> 0xFD).run(0xCB, 0x89),
            test.verifyRegister(REG_C, context -> 0xFB).run(0xCB, 0x91),
            test.verifyRegister(REG_C, context -> 0xF7).run(0xCB, 0x99),
            test.verifyRegister(REG_C, context -> 0xEF).run(0xCB, 0xA1),
            test.verifyRegister(REG_C, context -> 0xDF).run(0xCB, 0xA9),
            test.verifyRegister(REG_C, context -> 0xBF).run(0xCB, 0xB1),
            test.verifyRegister(REG_C, context -> 0x7F).run(0xCB, 0xB9)
        );
    }

    @Test
    public void testRES_b__D() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_D, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_D, context -> 0xFE).run(0xCB, 0x82),
            test.verifyRegister(REG_D, context -> 0xFD).run(0xCB, 0x8A),
            test.verifyRegister(REG_D, context -> 0xFB).run(0xCB, 0x92),
            test.verifyRegister(REG_D, context -> 0xF7).run(0xCB, 0x9A),
            test.verifyRegister(REG_D, context -> 0xEF).run(0xCB, 0xA2),
            test.verifyRegister(REG_D, context -> 0xDF).run(0xCB, 0xAA),
            test.verifyRegister(REG_D, context -> 0xBF).run(0xCB, 0xB2),
            test.verifyRegister(REG_D, context -> 0x7F).run(0xCB, 0xBA)
        );
    }

    @Test
    public void testRES_b__E() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_E, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_E, context -> 0xFE).run(0xCB, 0x83),
            test.verifyRegister(REG_E, context -> 0xFD).run(0xCB, 0x8B),
            test.verifyRegister(REG_E, context -> 0xFB).run(0xCB, 0x93),
            test.verifyRegister(REG_E, context -> 0xF7).run(0xCB, 0x9B),
            test.verifyRegister(REG_E, context -> 0xEF).run(0xCB, 0xA3),
            test.verifyRegister(REG_E, context -> 0xDF).run(0xCB, 0xAB),
            test.verifyRegister(REG_E, context -> 0xBF).run(0xCB, 0xB3),
            test.verifyRegister(REG_E, context -> 0x7F).run(0xCB, 0xBB)
        );
    }

    @Test
    public void testRES_b__H() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_H, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_H, context -> 0xFE).run(0xCB, 0x84),
            test.verifyRegister(REG_H, context -> 0xFD).run(0xCB, 0x8C),
            test.verifyRegister(REG_H, context -> 0xFB).run(0xCB, 0x94),
            test.verifyRegister(REG_H, context -> 0xF7).run(0xCB, 0x9C),
            test.verifyRegister(REG_H, context -> 0xEF).run(0xCB, 0xA4),
            test.verifyRegister(REG_H, context -> 0xDF).run(0xCB, 0xAC),
            test.verifyRegister(REG_H, context -> 0xBF).run(0xCB, 0xB4),
            test.verifyRegister(REG_H, context -> 0x7F).run(0xCB, 0xBC)
        );
    }

    @Test
    public void testRES_b__L() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .setRegister(REG_L, 0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forGivenOperandsAndSingleRun((byte) 0,
            test.verifyRegister(REG_L, context -> 0xFE).run(0xCB, 0x85),
            test.verifyRegister(REG_L, context -> 0xFD).run(0xCB, 0x8D),
            test.verifyRegister(REG_L, context -> 0xFB).run(0xCB, 0x95),
            test.verifyRegister(REG_L, context -> 0xF7).run(0xCB, 0x9D),
            test.verifyRegister(REG_L, context -> 0xEF).run(0xCB, 0xA5),
            test.verifyRegister(REG_L, context -> 0xDF).run(0xCB, 0xAD),
            test.verifyRegister(REG_L, context -> 0xBF).run(0xCB, 0xB5),
            test.verifyRegister(REG_L, context -> 0x7F).run(0xCB, 0xBD)
        );
    }

    @Test
    public void testRES_b__mHL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsMemoryAddressByte(0xFF)
            .firstIsPair(REG_PAIR_HL)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> context.first;
        Generator.forSome16bitUnary(2,
            test.verifyByte(address, context -> 0xFE).run(0xCB, 0x86),
            test.verifyByte(address, context -> 0xFD).run(0xCB, 0x8E),
            test.verifyByte(address, context -> 0xFB).run(0xCB, 0x96),
            test.verifyByte(address, context -> 0xF7).run(0xCB, 0x9E),
            test.verifyByte(address, context -> 0xEF).run(0xCB, 0xA6),
            test.verifyByte(address, context -> 0xDF).run(0xCB, 0xAE),
            test.verifyByte(address, context -> 0xBF).run(0xCB, 0xB6),
            test.verifyByte(address, context -> 0x7F).run(0xCB, 0xBE)
        );
    }

    @Test
    public void testRES_b__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIX()
            .first8MSBplus8LSBisMemoryByte(0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> get8MSBplus8LSB(context.first);
        Generator.forSome16bitUnary(0x100,
            test.verifyByte(address, context -> 0xFE).runWithFirst8bitOperandWithOpcodeAfter(0x86, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xFD).runWithFirst8bitOperandWithOpcodeAfter(0x8E, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xFB).runWithFirst8bitOperandWithOpcodeAfter(0x96, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xF7).runWithFirst8bitOperandWithOpcodeAfter(0x9E, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xEF).runWithFirst8bitOperandWithOpcodeAfter(0xA6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xDF).runWithFirst8bitOperandWithOpcodeAfter(0xAE, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0xBF).runWithFirst8bitOperandWithOpcodeAfter(0xB6, 0xDD, 0xCB),
            test.verifyByte(address, context -> 0x7F).runWithFirst8bitOperandWithOpcodeAfter(0xBE, 0xDD, 0xCB)
        );
    }

    @Test
    public void testRES_b__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisIY()
            .first8MSBplus8LSBisMemoryByte(0xFF)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> address = context -> get8MSBplus8LSB(context.first);
        Generator.forSome16bitUnary(0x100,
            test.verifyByte(address, context -> 0xFE).runWithFirst8bitOperandWithOpcodeAfter(0x86, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xFD).runWithFirst8bitOperandWithOpcodeAfter(0x8E, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xFB).runWithFirst8bitOperandWithOpcodeAfter(0x96, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xF7).runWithFirst8bitOperandWithOpcodeAfter(0x9E, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xEF).runWithFirst8bitOperandWithOpcodeAfter(0xA6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xDF).runWithFirst8bitOperandWithOpcodeAfter(0xAE, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0xBF).runWithFirst8bitOperandWithOpcodeAfter(0xB6, 0xFD, 0xCB),
            test.verifyByte(address, context -> 0x7F).runWithFirst8bitOperandWithOpcodeAfter(0xBE, 0xFD, 0xCB)
        );
    }

}
