package net.sf.emustudio.intel8080.impl;

import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import org.junit.Test;

import java.util.function.Function;

public class StackTest extends InstructionsTest {

    @Test
    public void testPUSH() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyWord(context -> context.second, context -> context.first - 2)
                .firstIsPair(REG_SP)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
                test.secondIsPair(REG_PAIR_BC).run(0xC5),
                test.secondIsPair(REG_PAIR_DE).run(0xD5),
                test.secondIsPair(REG_PAIR_HL).run(0xE5)
        );
    }

    @Test
    public void testPUSH_PSW() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyWord(context -> context.second & 0xFFD7 | 2, context -> context.first - 2)
                .firstIsPair(REG_SP)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
                test.secondIsRegisterPairPSW(REG_PSW).run(0xF5)
        );
    }

    @Test
    public void testPOP() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .firstIsPair(REG_SP)
                .firstIsAddressAndSecondIsMemoryWord()
                .keepCurrentInjectorsAfterRun();

        Function<RunnerContext<Integer>, Integer> verifier = context -> context.second;

        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPair(REG_PAIR_BC, verifier).run(0xC1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPair(REG_PAIR_DE, verifier).run(0xD1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPair(REG_PAIR_HL, verifier).run(0xE1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPairAndPSW(REG_PSW, context -> context.second & 0xFFD7 | 2).run(0xF1)
        );
    }


}
