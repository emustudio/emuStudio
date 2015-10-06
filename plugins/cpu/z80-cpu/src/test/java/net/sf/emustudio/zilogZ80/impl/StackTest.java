/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.zilogZ80.impl.suite.IntegerTestBuilder;
import org.junit.Test;

public class StackTest extends InstructionsTest {

    @Test
    public void testPUSH_qq() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.SP - 2) & 0xFFFF)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
                test.firstIsPair(REG_PAIR_BC).run(0xC5),
                test.firstIsPair(REG_PAIR_DE).run(0xD5),
                test.firstIsPair(REG_PAIR_HL).run(0xE5),
                test.firstIsPSW().run(0xF5)
        );
    }

    @Test
    public void testPUSH_IX_IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first - 2) & 0xFFFF)
                .verifyWord(context -> (context.first - 2) & 0xFFFF, context -> context.second)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(
                test.secondIsIX().run(0xDD, 0xE5),
                test.secondIsIY().run(0xFD, 0xE5)
        );
    }

    @Test
    public void testPOP_qq() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).verifyPair(REG_PAIR_BC, context -> context.second).run(0xC1),
                test.secondIsPair(REG_PAIR_DE).verifyPair(REG_PAIR_DE, context -> context.second).run(0xD1),
                test.secondIsPair(REG_PAIR_HL).verifyPair(REG_PAIR_HL, context -> context.second).run(0xE1),
                test.secondIsPSW().verifyPSW(context -> context.second).run(0xF1)
        );
    }

    @Test
    public void testPOP_IX_IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(
                test.verifyIX(context -> context.second).run(0xDD, 0xE1),
                test.verifyIY(context -> context.second).run(0xFD, 0xE1)
        );
    }
    
}