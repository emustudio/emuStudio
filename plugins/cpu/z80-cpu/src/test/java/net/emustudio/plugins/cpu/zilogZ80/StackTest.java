/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

/**
 * Stack instructions: PUSH/POP for general register pairs (qq = BC/DE/HL/AF) and the IX/IY index
 * registers. Each test verifies behavior, T-state cycles, R-register delta and the resulting
 * memory image around SP. PUSH/POP do not touch flags (POP AF restores them from memory) and do
 * not modify MEMPTR.
 */
public class StackTest extends InstructionsTest {

    /**
     * PUSH qq: SP-=2, mem(SP)=qq_low, mem(SP+1)=qq_high; 11 T-states; R += 1.
     */
    @Test
    public void testPUSH__qq() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.SP - 2) & 0xFFFF)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.first)
                .verifyR(1)
                .verifyCycles(11)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(0, 6,
                test.firstIsPair(REG_PAIR_BC).run(0xC5),
                test.firstIsPair(REG_PAIR_DE).run(0xD5),
                test.firstIsPair(REG_PAIR_HL).run(0xE5),
                test.firstIsPSW().run(0xF5)
        );
    }

    /**
     * PUSH IX/IY: same as PUSH qq but with DD/FD prefix; 15 T-states; R += 2.
     */
    @Test
    public void testPUSH__II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first - 2) & 0xFFFF)
                .verifyWord(context -> (context.first - 2) & 0xFFFF, context -> context.second)
                .verifyR(2)
                .verifyCycles(15)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(6,
                test.secondIsIX().run(0xDD, 0xE5),
                test.secondIsIY().run(0xFD, 0xE5)
        );
    }

    /**
     * POP qq: qq=mem word at SP, SP+=2; 10 T-states; R += 1. POP AF restores F from memory.
     */
    @Test
    public void testPOP__qq() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .verifyR(1)
                .verifyCycles(10)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(3,
                test.secondIsPair(REG_PAIR_BC).verifyPair(REG_PAIR_BC, context -> context.second).run(0xC1),
                test.secondIsPair(REG_PAIR_DE).verifyPair(REG_PAIR_DE, context -> context.second).run(0xD1),
                test.secondIsPair(REG_PAIR_HL).verifyPair(REG_PAIR_HL, context -> context.second).run(0xE1),
                test.secondIsPSW().verifyPSW(context -> context.second).run(0xF1)
        );
    }

    /**
     * POP IX/IY: same as POP qq but with DD/FD prefix; 14 T-states; R += 2.
     */
    @Test
    public void testPOP__II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .verifyR(2)
                .verifyCycles(14)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitBinary(4,
                test.verifyIX(context -> context.second).run(0xDD, 0xE1),
                test.verifyIY(context -> context.second).run(0xFD, 0xE1)
        );
    }
}

