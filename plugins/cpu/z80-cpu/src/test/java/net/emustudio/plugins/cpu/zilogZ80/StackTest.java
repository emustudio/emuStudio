/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

public class StackTest extends InstructionsTest {

    @Test
    public void testPUSH_qq() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.SP - 2) & 0xFFFF)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(0, 6,
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
    public void testPOP_qq() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(3,
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

        Generator.forSome16bitBinary(3,
                test.verifyIX(context -> context.second).run(0xDD, 0xE1),
                test.verifyIY(context -> context.second).run(0xFD, 0xE1)
        );
    }

}
