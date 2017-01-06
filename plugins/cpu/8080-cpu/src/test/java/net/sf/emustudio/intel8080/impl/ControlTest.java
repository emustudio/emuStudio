/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.intel8080.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;

@SuppressWarnings("unchecked")
public class ControlTest extends InstructionsTest {

    @Test
    public void testEI_DI() throws Exception {
        cpuRunnerImpl.setProgram(0xFB, 0xF3);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreEnabled();
        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreDisabled();
    }

    @Test
    public void testJMP() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPC(context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(
                test.runWithFirstOperand(0xC3),
                test.runWithFirstOperand(0xC2),
                test.setFlags(FLAG_Z).runWithFirstOperand(0xCA),
                test.runWithFirstOperand(0xD2),
                test.setFlags(FLAG_C).runWithFirstOperand(0xDA),
                test.runWithFirstOperand(0xE2),
                test.setFlags(FLAG_P).runWithFirstOperand(0xEA),
                test.runWithFirstOperand(0xF2),
                test.setFlags(FLAG_S).runWithFirstOperand(0xFA)
        );

        test.clearAllVerifiers().verifyPC(context -> (context.PC + 3) & 0xFFFF);
        Generator.forSome16bitUnary(
                test.setFlags(FLAG_Z).runWithFirstOperand(0xC2),
                test.runWithFirstOperand(0xCA),
                test.setFlags(FLAG_C).runWithFirstOperand(0xD2),
                test.runWithFirstOperand(0xDA),
                test.setFlags(FLAG_P).runWithFirstOperand(0xE2),
                test.runWithFirstOperand(0xEA),
                test.setFlags(FLAG_S).runWithFirstOperand(0xF2),
                test.runWithFirstOperand(0xFA)
        );
    }

    @Test
    public void testCALL() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPC(context -> context.second)
                .verifyWord(context -> context.first - 2, context -> context.PC + 3)
                .firstIsPair(REG_SP)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(3,
                test.runWithSecondOperand(0xCD),
                test.runWithSecondOperand(0xC4),
                test.setFlags(FLAG_Z).runWithSecondOperand(0xCC),
                test.runWithSecondOperand(0xD4),
                test.setFlags(FLAG_C).runWithSecondOperand(0xDC),
                test.runWithSecondOperand(0xE4),
                test.setFlags(FLAG_P).runWithSecondOperand(0xEC),
                test.runWithSecondOperand(0xF4),
                test.setFlags(FLAG_S).runWithSecondOperand(0xFC)
        );

        test.clearAllVerifiers().verifyPC(context -> (context.PC + 3) & 0xFFFF);
        Generator.forSome16bitBinary(3,
                test.setFlags(FLAG_Z).runWithSecondOperand(0xC4),
                test.runWithSecondOperand(0xCC),
                test.setFlags(FLAG_C).runWithSecondOperand(0xD4),
                test.runWithSecondOperand(0xDC),
                test.setFlags(FLAG_P).runWithSecondOperand(0xE4),
                test.runWithSecondOperand(0xEC),
                test.setFlags(FLAG_S).runWithSecondOperand(0xF4),
                test.runWithSecondOperand(0xFC)
        );
    }

    @Test
    public void testRET() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_SP, context -> context.first + 2)
                .verifyPC(context -> context.second)
                .firstIsPair(REG_SP)
                .firstIsAddressAndSecondIsMemoryWord()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
                test.run(0xC9),
                test.run(0xC0),
                test.setFlags(FLAG_Z).run(0xC8),
                test.run(0xD0),
                test.setFlags(FLAG_C).run(0xD8),
                test.run(0xE0),
                test.setFlags(FLAG_P).run(0xE8),
                test.run(0xF0),
                test.setFlags(FLAG_S).run(0xF8)
        );

        // negative tests
        test.clearAllVerifiers()
                .verifyPC(context -> 1)
                .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitBinary(2,
                test.setFlags(FLAG_Z).run(0xC0),
                test.run(0xC8),
                test.setFlags(FLAG_C).run(0xD0),
                test.run(0xD8),
                test.setFlags(FLAG_P).run(0xE0),
                test.run(0xE8),
                test.setFlags(FLAG_S).run(0xF0),
                test.run(0xF8)
        );

    }

    @Test
    public void testRST() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> context.first - 2)
                .verifyWord(context -> context.SP - 2, context -> 1)
                .clearOtherVerifiersAfterRun()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0).run(0xC7),
                test.verifyPC(context -> 0x8).run(0xCF),
                test.verifyPC(context -> 0x10).run(0xD7),
                test.verifyPC(context -> 0x18).run(0xDF),
                test.verifyPC(context -> 0x20).run(0xE7),
                test.verifyPC(context -> 0x28).run(0xEF),
                test.verifyPC(context -> 0x30).run(0xF7),
                test.verifyPC(context -> 0x38).run(0xFF)
        );
    }

    @Test
    public void testPCHL() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_HL)
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(
                test.run(0xE9)
        );
    }

    @Test
    public void testHLT() throws Exception {
        cpuRunnerImpl.setProgram(0x76);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.expectRunState(CPU.RunState.STATE_STOPPED_NORMAL);
        cpuRunnerImpl.step();
    }


}
