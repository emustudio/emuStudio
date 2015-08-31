/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.ByteTestBuilder;
import net.sf.emustudio.zilogZ80.impl.suite.FlagsBuilderImpl;
import net.sf.emustudio.zilogZ80.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;
import static net.sf.emustudio.zilogZ80.impl.suite.Utils.predicate8MSBplus8LSB;

public class LogicTest extends InstructionsTTest {

    private ByteTestBuilder getLogicTestBuilder(Function<RunnerContext<Byte>, Integer> operator) {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, operator)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl()
                        .carryIsReset().subtractionIsReset().halfCarryIsSet().parity().sign().zero())
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testAND__r() throws Exception {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first & context.second);

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
    public void testAND__n() throws Exception {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first & context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xE6)
        );
    }

    @Test
    public void testOR__r() throws Exception {
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
    public void testOR__n() throws Exception {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first | context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xF6)
        );
    }

    @Test
    public void testXOR__r() throws Exception {
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
    public void testXOR__n() throws Exception {
        ByteTestBuilder test = getLogicTestBuilder(context -> context.first ^ context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xEE)
        );
    }

    @Test
    public void testCP__r() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> context.first.intValue() & 0xFF)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet(),
                        context -> (context.first & 0xFF) - (context.second & 0xFF))
                .firstIsRegister(REG_A)
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
    public void testCP__n() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> context.first.intValue() & 0xFF)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet(),
                        context -> (context.first & 0xFF) - (context.second & 0xFF))
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xFE)
        );
    }

    @Test
    public void testDAA() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> {
                    int result = ((int) context.first) & 0xFF;
                    if (((context.flags & FLAG_H) == FLAG_H) || (result & 0x0F) > 9) {
                        result += 6;
                    }
                    if (((context.flags & FLAG_C) == FLAG_C) || (result & 0xF0) > 0x90) {
                        result += 0x60;
                    }
                    return result;
                })
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().parity()
                                .expectFlagOnlyWhen(FLAG_H, (context, result) -> {
                                    int firstInt = ((RunnerContext) context).first.intValue() & 0xFF;
                                    int diff = (((Number) result).intValue() - firstInt) & 0x0F;

                                    return ((diff == 6) && FlagsBuilderImpl.isAuxCarry(firstInt, 6));
                                })
                                .expectFlagOnlyWhen(FLAG_C, ((context, result) -> {
                                    int firstInt = ((RunnerContext) context).first.intValue() & 0xFF;
                                    int diff = (((Number) result).intValue() - firstInt) & 0xF0;

                                    return ((diff == 0x60) && ((((Number) result).intValue() & 0x100) == 0x100));
                                }))
                )
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();

        Generator.forAll8bitUnary(
                test.run(0x27)
        );
    }

    @Test
    public void testCPL() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> ~context.first)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>().halfCarryIsSet().subtractionIsSet())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.run(0x2F)
        );
    }

    @Test
    public void testSCF() throws Exception {
        cpuRunnerImpl.setProgram(0x37);
        cpuRunnerImpl.reset();

        cpuRunnerImpl.step();

        cpuVerifierImpl.checkFlags(FLAG_C);
    }

    @Test
    public void testCCF() throws Exception {
        cpuRunnerImpl.setProgram(0x3F);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setFlags(FLAG_C);

        cpuRunnerImpl.step();

        cpuVerifierImpl.checkNotFlags(FLAG_C);
    }

    @Test
    public void testRLCA() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context ->  ((context.first << 1) & 0xFF) | (context.first >>> 7) & 1)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                        .carryIsFirstOperandMSB().halfCarryIsReset().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setFlags(FLAG_H | FLAG_H).run(0x07)
        );
    }

    @Test
    public void testRRCA() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> ((context.first >> 1) & 0x7F) | ((context.first & 1) << 7))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                        .carryIsFirstOperandLSB().halfCarryIsReset().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setFlags(FLAG_H | FLAG_H).run(0x0F)
        );
    }

    @Test
    public void testRLA() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context ->  ((context.first << 1) & 0xFE) | (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                        .carryIsFirstOperandMSB().halfCarryIsReset().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setFlags(FLAG_H | FLAG_H).run(0x17)
        );
    }

    @Test
    public void testRRA() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> ((context.first >> 1) & 0x7F) | ((context.flags & 1) << 7))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                        .carryIsFirstOperandLSB().halfCarryIsReset().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setFlags(FLAG_H | FLAG_H).run(0x1F)
        );
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
                )
                .verifyFlags(new FlagsBuilderImpl<>()
                        .sign().zero().subtractionIsSet().halfCarry()
                        .expectFlagOnlyWhen(FLAG_PV, (context, result) ->
                                        ((context.getRegister(REG_B) << 8 | context.getRegister(REG_C)) - 1) != 0
                        ), context -> context.registers.get(REG_A) - (context.second & 0xFF));
    }

    @Test
    public void testCPI() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) + 1))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
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
                })
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
                test.run(0xED, 0xB1)
        );
    }

    @Test
    public void testCPD() {
        IntegerTestBuilder test = prepareCPxTest(context ->
                ((context.getRegister(REG_H) << 8 | context.getRegister(REG_L)) - 1))
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
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
                })
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
                test.run(0xED, 0xB9)
        );
    }

    private IntegerTestBuilder prepareLogicIXYtest(Function<RunnerContext<Integer>, Integer> operation) {
        return new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, operation)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl()
                        .sign().zero().carryIsReset().halfCarryIsSet().parity().subtractionIsReset());
    }


    @Test
    public void testAND__IX_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) & (context.second & 0xFF))
                .first8MSBisIX()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0xA6)
        );
    }

    @Test
    public void testAND__IY_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) & (context.second & 0xFF))
                .first8MSBisIY()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0xA6)
        );
    }

    @Test
    public void testOR__IX_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) | (context.second & 0xFF))
                .first8MSBisIX()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0xB6)
        );
    }

    @Test
    public void testOR__IY_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) | (context.second & 0xFF))
                .first8MSBisIY()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0xB6)
        );
    }

    @Test
    public void testXOR__IX_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) ^ (context.second & 0xFF))
                .first8MSBisIX()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0xAE)
        );
    }

    @Test
    public void testXOR__IY_plus_d() {
        IntegerTestBuilder test = prepareLogicIXYtest(context -> (context.first & 0xFF) ^ (context.second & 0xFF))
                .first8MSBisIY()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0xAE)
        );
    }

    @Test
    public void testCP__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .first8MSBisIX()
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet(),
                        context -> (context.first & 0xFF) - (context.second & 0xFF)
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0xBE)
        );
    }

    @Test
    public void testCP__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .first8MSBisIY()
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet(),
                        context -> (context.first & 0xFF) - (context.second & 0xFF)
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0xBE)
        );
    }

    @Test
    public void testNEG() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .printOperands()
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (0 - context.first) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>().sign().zero().halfCarry().subtractionIsSet()
                        .expectFlagOnlyWhen(FLAG_C, (context, result) -> context.first.intValue() != 0)
                        .expectFlagOnlyWhen(FLAG_PV, (context, result) -> (context.first.intValue() & 0xFF) == 0x80)
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.run(0xED, 0x44)
        );
    }

    public void testRLC__r() {

    }

    public void testRLC__mHL() {

    }

    public void testRLC__IX_plus_d() {

    }

    public void testRLC__IY_plus_d() {

    }

    public void testRL__r() {

    }

    public void testRL__mHL() {

    }

    public void testRL__IX_plus_d() {

    }

    public void testRL__IY_plus_d() {

    }

    public void testRRC__r() {

    }

    public void testRRC__mHL() {

    }

    public void testRRC__IX_plus_d() {

    }

    public void testRRC__IY_plus_d() {

    }

    public void testRR__r() {

    }

    public void testRR__mHL() {

    }

    public void testRR__IX_plus_d() {

    }

    public void testRR__IY_plus_d() {

    }

    public void testSLA__r() {

    }

    public void testSLA__mHL() {

    }

    public void testSLA__IX_plus_d() {

    }

    public void testSLA__IY_plus_d() {

    }

    public void testSRA__r() {

    }

    public void testSRA__mHL() {

    }

    public void testSRA__IX_plus_d() {

    }

    public void testSRA__IY_plus_d() {

    }

    public void testSRL__r() {

    }

    public void testSRL__mHL() {

    }

    public void testSRL__IX_plus_d() {

    }

    public void testSRL__IY_plus_d() {

    }

    public void testRLD() {

    }

    public void testRRD() {

    }

}
