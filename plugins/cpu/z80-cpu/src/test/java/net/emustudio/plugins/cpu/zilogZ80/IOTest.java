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
import net.emustudio.plugins.cpu.zilogZ80.suite.FlagsCheckImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

public class IOTest extends InstructionsTest {

    @Test
    public void testIN_A_REF_N() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_A)
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xDB)
        );
    }

    @Test
    public void testIN_R_REF_C() {
        Function<RunnerContext<Byte>, Integer> operation = context -> context.first & 0xFF;

        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_C)
                .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().parity().halfCarryIsReset().subtractionIsReset(),
                        operation)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(
                test.verifyRegister(REG_A, operation).runWithSecondOperand(0xED, 0x78),
                test.verifyRegister(REG_B, operation).runWithSecondOperand(0xED, 0x40),
                test.verifyRegister(REG_C, operation).runWithSecondOperand(0xED, 0x48),
                test.verifyRegister(REG_D, operation).runWithSecondOperand(0xED, 0x50),
                test.verifyRegister(REG_E, operation).runWithSecondOperand(0xED, 0x58),
                test.verifyRegister(REG_H, operation).runWithSecondOperand(0xED, 0x60),
                test.verifyRegister(REG_L, operation).runWithSecondOperand(0xED, 0x68)
        );
    }

    @Test
    public void testIN_REF_C() {
        Function<RunnerContext<Byte>, Integer> operation = context -> context.first & 0xFF;

        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsDeviceAndSecondIsPort()
                .secondIsRegister(REG_C)
                .verifyFlags(new FlagsCheckImpl<Byte>().sign().zero().parity().halfCarryIsReset().subtractionIsReset(),
                        operation)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xED, 0x70)
        );
    }

    @Test
    public void testINI() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisDeviceAndFirst8LSBIsPort()
                .first8LSBisRegister(REG_C)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8MSBisRegister(REG_B)
                .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().zero()
                        .carryIsPreserved()
                        .expectFlagOnlyWhen(FLAG_N, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> (context.flags & FLAG_C) == FLAG_C)
                );

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xA2)
        );
    }

    @Test
    public void testINIR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisDeviceAndFirst8LSBIsPort()
                .first8LSBisRegister(REG_C)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8MSBisRegister(REG_B)
                .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                        .zeroIsSet()
                        .subtractionIsSet()
                        .carryIsPreserved()
                )
                .verifyPC(context -> {
                    if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                        return context.PC;
                    }
                    return context.PC + 2;
                });

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xB2)
        );
    }

    @Test
    public void testIND() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisDeviceAndFirst8LSBIsPort()
                .first8LSBisRegister(REG_C)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8MSBisRegister(REG_B)
                .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().zero()
                        .subtractionIsSet()
                        .carryIsPreserved()
                );

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xAA)
        );
    }

    @Test
    public void testINDR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBisDeviceAndFirst8LSBIsPort()
                .first8LSBisRegister(REG_C)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8MSBisRegister(REG_B)
                .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                        .zeroIsSet()
                        .subtractionIsSet()
                        .carryIsPreserved())
                .verifyPC(context -> {
                    if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                        return context.PC;
                    }
                    return context.PC + 2;
                });

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xBA)
        );
    }

    @Test
    public void testOUT_REF_N_A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xD3)
        );
    }

    @Test
    public void testOUT_REF_C_R() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsRegister(REG_C)
                .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.firstIsRegister(REG_C).run(0xED, 0x49)
        );

        Generator.forSome8bitBinary(
                test.firstIsRegister(REG_B).run(0xED, 0x41),
                test.firstIsRegister(REG_D).run(0xED, 0x51),
                test.firstIsRegister(REG_E).run(0xED, 0x59),
                test.firstIsRegister(REG_H).run(0xED, 0x61),
                test.firstIsRegister(REG_L).run(0xED, 0x69),
                test.firstIsRegister(REG_A).run(0xED, 0x79)
        );
    }

    @Test
    public void testOUT_REF_C_0() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsRegister(REG_C)
                .verifyDeviceWhenSecondIsPort(context -> 0)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.firstIsRegister(REG_C).run(0xED, 0x71)
        );
    }

    @Test
    public void testOUTI() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_C)
                .first8MSBisRegister(REG_B)
                .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().zero()
                        .expectFlagOnlyWhen(FLAG_N, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> (context.flags & FLAG_C) == FLAG_C)
                );

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xA3)
        );
    }

    @Test
    public void testOTIR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_C)
                .first8MSBisRegister(REG_B)
                .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                        .expectFlagOnlyWhen(FLAG_N, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_Z, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> (context.flags & FLAG_C) == FLAG_C)
                )
                .verifyPC(context -> {
                    if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                        return context.PC;
                    }
                    return context.PC + 2;
                });

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xB3)
        );
    }

    @Test
    public void testOUTD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_C)
                .first8MSBisRegister(REG_B)
                .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().zero()
                        .subtractionIsSet()
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> (context.flags & FLAG_C) == FLAG_C)
                );

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xAB)
        );
    }

    @Test
    public void testOTDR() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_C)
                .first8MSBisRegister(REG_B)
                .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
                .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
                .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>()
                        .expectFlagOnlyWhen(FLAG_N, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_Z, (context, result) -> true)
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> (context.flags & FLAG_C) == FLAG_C)
                )
                .verifyPC(context -> {
                    if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                        return context.PC;
                    }
                    return context.PC + 2;
                });

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0xBB)
        );
    }
}

