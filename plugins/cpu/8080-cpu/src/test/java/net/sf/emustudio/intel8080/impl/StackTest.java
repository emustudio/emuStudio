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

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public class StackTest extends InstructionsTest {

    @Test
    public void testPUSH() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_SP)
            .verifyWord(context -> context.first - 2, context -> context.second)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
            test.secondIsPair(REG_PAIR_BC).run(0xC5),
            test.secondIsPair(REG_PAIR_DE).run(0xD5),
            test.secondIsPair(REG_PAIR_HL).run(0xE5)
        );
    }

    @Test
    public void testPUSH_PSW() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_SP)
            .verifyWord(context -> context.first - 2, context -> context.second & 0xFFD7 | 2)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
            test.secondIsRegisterPairPSW(REG_PSW).run(0xF5)
        );
    }

    @Test
    public void testPOP() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsPair(REG_SP)
            .firstIsAddressAndSecondIsMemoryWord()
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Function<RunnerContext<Integer>, Integer> verifier = context -> context.second;

        Generator.forSome16bitBinary(2,
            test.verifyPair(REG_PAIR_BC, verifier).run(0xC1),
            test.verifyPair(REG_PAIR_DE, verifier).run(0xD1),
            test.verifyPair(REG_PAIR_HL, verifier).run(0xE1),
            test.verifyPairAndPSW(REG_PSW, context -> context.second & 0xFFD7 | 2).run(0xF1)
        );
    }

}
