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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.intel8080.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.intel8080.suite.IntegerTestBuilder;
import org.junit.Test;

public class TransferTest extends InstructionsTest {

    @Test
    public void testMVI() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
            test.verifyRegister(EmulatorEngine.REG_A, context -> context.first & 0xFF).runWithFirstOperand(0x3E),
            test.verifyRegister(EmulatorEngine.REG_B).runWithFirstOperand(0x06),
            test.verifyRegister(EmulatorEngine.REG_C).runWithFirstOperand(0x0E),
            test.verifyRegister(EmulatorEngine.REG_D).runWithFirstOperand(0x16),
            test.verifyRegister(EmulatorEngine.REG_E).runWithFirstOperand(0x1E),
            test.verifyRegister(EmulatorEngine.REG_H).runWithFirstOperand(0x26),
            test.verifyRegister(EmulatorEngine.REG_L).runWithFirstOperand(0x2E),
            test.setPair(REG_PAIR_HL, 0x20)
                .verifyByte(0x20, context -> context.first & 0xFF)
                .runWithFirstOperand(0x36)
        );
    }

    @Test
    public void testMOV_A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_A, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x7F),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x78),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x79),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x7A),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x7B),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x7C),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x7D),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x7E)
        );
    }

    @Test
    public void testMOV_B() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_B, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x47),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x40),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x41),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x42),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x43),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x44),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x45),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x46)
        );
    }

    @Test
    public void testMOV_C() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_C, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x4F),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x48),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x49),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x4A),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x4B),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x4C),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x4D),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x4E)
        );
    }

    @Test
    public void testMOV_D() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_D, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x57),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x50),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x51),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x52),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x53),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x54),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x55),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x56)
        );
    }

    @Test
    public void testMOV_E() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_E, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x5F),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x58),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x59),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x5A),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x5B),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x5C),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x5D),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x5E)
        );
    }

    @Test
    public void testMOV_H() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_H, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x67),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x60),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x61),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x62),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x63),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x64),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x65),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x66)
        );
    }

    @Test
    public void testMOV_L() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_L, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.firstIsRegister(EmulatorEngine.REG_A).run(0x6F),
            test.firstIsRegister(EmulatorEngine.REG_B).run(0x68),
            test.firstIsRegister(EmulatorEngine.REG_C).run(0x69),
            test.firstIsRegister(EmulatorEngine.REG_D).run(0x6A),
            test.firstIsRegister(EmulatorEngine.REG_E).run(0x6B),
            test.firstIsRegister(EmulatorEngine.REG_H).run(0x6C),
            test.firstIsRegister(EmulatorEngine.REG_L).run(0x6D),
            test.setPair(REG_PAIR_HL, 0x20).firstIsMemoryByteAt(0x20).run(0x6E)
        );
    }

    @Test
    public void testMOV_M_r() {
        final int address = 0x35;

        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyByte(address, context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
            test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_A).run(0x77),
            test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_B).run(0x70),
            test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_C).run(0x71),
            test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_D).run(0x72),
            test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_E).run(0x73)
        );
        test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_H).run(0x74).accept((byte) 0, (byte) 0);
        test.setPair(REG_PAIR_HL, address).firstIsRegister(EmulatorEngine.REG_L).run(0x75).accept((byte) address, (byte) 0);
    }

    @Test
    public void testLDAX() {
        final int value = 0x25;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_A, context -> value)
            .firstIsMemoryAddressByte(value)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
            test.firstIsPair(REG_PAIR_BC).run(0x0A),
            test.firstIsPair(REG_PAIR_DE).run(0x1A)
        );
    }

    @Test
    public void testSTAX() {
        final int value = 0x25;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyByte(context -> context.first, context -> value)
            .setRegister(EmulatorEngine.REG_A, value)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
            test.firstIsPair(REG_PAIR_BC).run(0x02),
            test.firstIsPair(REG_PAIR_DE).run(0x12)
        );
    }

    @Test
    public void testLDA() {
        byte value = -120;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyRegister(EmulatorEngine.REG_A, context -> value & 0xFF)
            .firstIsMemoryAddressByte(value);

        Generator.forSome16bitUnary(3,
            test.runWithFirstOperand(0x3A)
        );
    }

    @Test
    public void testSTA() {
        byte value = -120;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyByte(context -> context.first, context -> value & 0xFF)
            .setRegister(EmulatorEngine.REG_A, value);

        Generator.forSome16bitUnary(3,
            test.runWithFirstOperand(0x32)
        );
    }

    @Test
    public void testLHLD() {
        int value = 0x1234;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_PAIR_HL, context -> value)
            .firstIsMemoryAddressWord(value);

        Generator.forSome16bitUnary(3,
            test.runWithFirstOperand(0x2A)
        );
    }

    @Test
    public void testSHLD() {
        int value = 0x1236;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyWord(context -> context.first, context -> value)
            .setPair(REG_PAIR_HL, value);

        Generator.forSome16bitUnary(3,
            test.runWithFirstOperand(0x22)
        );
    }

    @Test
    public void testLXI_B() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_PAIR_BC, context -> context.first);

        Generator.forSome16bitUnary(
            test.runWithFirstOperand(0x01)
        );
    }

    @Test
    public void testLXI_D() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_PAIR_DE, context -> context.first);

        Generator.forSome16bitUnary(
            test.runWithFirstOperand(0x11)
        );
    }

    @Test
    public void testLXI_H() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_PAIR_HL, context -> context.first);

        Generator.forSome16bitUnary(
            test.runWithFirstOperand(0x21)
        );
    }

    @Test
    public void testLXI_SP() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitUnary(
            test.runWithFirstOperand(0x31)
        );
    }

    @Test
    public void testSPHL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitUnary(
            test.firstIsPair(REG_PAIR_HL).run(0xF9)
        );
    }

    @Test
    public void testXCHG() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_PAIR_HL)
            .secondIsPair(REG_PAIR_DE)
            .verifyPair(REG_PAIR_DE, context -> context.first)
            .verifyPair(REG_PAIR_HL, context -> context.second)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
            test.run(0xEB)
        );
    }

    @Test
    public void testXTHL() {
        int address = 0x23;

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsMemoryWordAt(address)
            .secondIsPair(REG_PAIR_HL)
            .setPair(REG_SP, address)
            .verifyWord(context -> address, context -> context.second)
            .verifyPair(REG_PAIR_HL, context -> context.first)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
            test.run(0xE3)
        );
    }

    @Test
    public void testMemoryOverflow() {
        cpuRunnerImpl.resetProgram();
        cpuRunnerImpl.reset();
        cpuRunnerImpl.expectRunState(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT);
        cpuRunnerImpl.step();
    }
}
