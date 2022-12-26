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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.intel8080.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.intel8080.suite.FlagsCheckImpl;
import org.junit.Test;

import java.util.function.Function;

public class LogicTest extends InstructionsTest {

    private ByteTestBuilder logicTest(Function<RunnerContext<Byte>, Integer> operation) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, operation)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().parity().carryIsReset().auxCarryIsReset())
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testANA() {
        ByteTestBuilder test = logicTest(context -> context.first & context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xA7)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0xA0),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0xA1),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0xA2),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0xA3),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0xA4),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0xA5),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xA6)
        );
    }

    @Test
    public void testANI() {
        ByteTestBuilder test = logicTest(context -> context.first & context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xE6)
        );
    }

    @Test
    public void testXRA() {
        ByteTestBuilder test = logicTest(context -> context.first ^ context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xAF)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0xA8),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0xA9),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0xAA),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0xAB),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0xAC),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0xAD),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xAE)
        );
    }

    @Test
    public void testXRI() {
        ByteTestBuilder test = logicTest(context -> context.first ^ context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xEE)
        );
    }

    @Test
    public void testORA() {
        ByteTestBuilder test = logicTest(context -> context.first | context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xB7)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0xB0),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0xB1),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0xB2),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0xB3),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0xB4),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0xB5),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xB6)
        );
    }

    @Test
    public void testORI() {
        Function<RunnerContext<Byte>, Integer> op = context -> context.first | context.second;

        Generator.forSome8bitBinary(
                logicTest(op).runWithSecondOperand(0xF6)
        );
    }

    @Test
    public void testDAA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(EmulatorEngine.REG_A, context -> {
                    int result = ((int) context.first) & 0xFF;
                    if (((context.flags & EmulatorEngine.FLAG_AC) == EmulatorEngine.FLAG_AC) || (result & 0x0F) > 9) {
                        result += 6;
                    }
                    if ((context.flags & EmulatorEngine.FLAG_C) == EmulatorEngine.FLAG_C || (result & 0xF0) > 0x90) {
                        result += 0x60;
                    }
                    return result;
                })
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().sign().zero().parity().carry()
                        .expectFlagOnlyWhen(EmulatorEngine.FLAG_AC, (context, result) -> {
                            int firstInt = context.first.intValue();
                            int diff = (result.intValue() - firstInt) & 0x0F;

                            return ((diff == 6) && FlagsCheckImpl.isAuxCarry(firstInt, 6));
                        }))
                .firstIsRegister(EmulatorEngine.REG_A);

        Generator.forSome8bitUnary(
                test.run(0x27)
        );
    }

    @Test
    public void testCMA() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(EmulatorEngine.REG_A, context -> (~context.first) & 0xFF)
                .firstIsRegister(EmulatorEngine.REG_A);

        Generator.forSome8bitUnary(
                test.run(0x2F)
        );
    }

    @Test
    public void testSTC() {
        cpuRunnerImpl.setProgram(0x37);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.step();

        cpuVerifierImpl.checkFlags(EmulatorEngine.FLAG_C);
        cpuVerifierImpl.checkNotFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P);
    }

    @Test
    public void testCMC() {
        cpuRunnerImpl.setProgram(0x3F);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setFlags(EmulatorEngine.FLAG_C);
        cpuRunnerImpl.step();

        cpuVerifierImpl.checkNotFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P);
    }

    @Test
    public void testRLC() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first << 1) | ((context.first >>> 7) & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().carryIsFirstOperandMSB());

        Generator.forSome8bitUnary(
                test.run(0x07)
        );
    }

    @Test
    public void testRRC() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (((context.first & 0xFF) >>> 1) | ((context.first & 1) << 7)) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().carryIsFirstOperandLSB());

        Generator.forSome8bitUnary(
                test.run(0x0F)
        );
    }

    @Test
    public void testRAL() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (context.first << 1) | (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().carryIsFirstOperandMSB());

        Generator.forSome8bitUnary(
                test.run(0x17)
        );
    }

    @Test
    public void testRAR() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> (((context.first & 0xFF) >>> 1) | ((context.flags & 1) << 7)) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Byte>().carryIsFirstOperandLSB());

        Generator.forSome8bitUnary(
                test.run(0x1F)
        );
    }

    @Test
    public void testCMP() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> context.first.intValue())
                .verifyFlags(
                        new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity(),
                        context -> (context.first & 0xFF) - (context.second & 0xFF))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xBF)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(EmulatorEngine.REG_B).run(0xB8),
                test.secondIsRegister(EmulatorEngine.REG_C).run(0xB9),
                test.secondIsRegister(EmulatorEngine.REG_D).run(0xBA),
                test.secondIsRegister(EmulatorEngine.REG_E).run(0xBB),
                test.secondIsRegister(EmulatorEngine.REG_H).run(0xBC),
                test.secondIsRegister(EmulatorEngine.REG_L).run(0xBD),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xBE)
        );
    }

    @Test
    public void testCPI() {
        FlagsCheckImpl<Byte> flagsToCheck = new FlagsCheckImpl<Byte>().sign().zero().carry().auxCarry().parity();
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(EmulatorEngine.REG_A)
                .verifyRegister(EmulatorEngine.REG_A, context -> context.first.intValue())
                .verifyFlags(flagsToCheck, context -> (context.first & 0xFF) - (context.second & 0xFF));

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xFE)
        );
    }
}
