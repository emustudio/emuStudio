package net.sf.emustudio.intel8080.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.ByteTestBuilder;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilderImpl;
import net.sf.emustudio.intel8080.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;

public class ArithmeticTest extends InstructionsTest {

    private ByteTestBuilder additionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();
    }

    private ByteTestBuilder subtractionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testADD() throws Exception {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x87)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x80),
                test.secondIsRegister(REG_C).run(0x81),
                test.secondIsRegister(REG_D).run(0x82),
                test.secondIsRegister(REG_E).run(0x83),
                test.secondIsRegister(REG_H).run(0x84),
                test.secondIsRegister(REG_L).run(0x85),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x86)
        );
    }

    @Test
    public void testADI() throws Exception {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xC6)
        );
    }

    @Test
    public void testADC() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x8F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x88),
                test.secondIsRegister(REG_C).run(0x89),
                test.secondIsRegister(REG_D).run(0x8A),
                test.secondIsRegister(REG_E).run(0x8B),
                test.secondIsRegister(REG_H).run(0x8C),
                test.secondIsRegister(REG_L).run(0x8D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x8E)
        );
    }

    @Test
    public void testACI() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity());

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xCE)
        );
    }

    @Test
    public void testSUB() throws Exception {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x97)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x90),
                test.secondIsRegister(REG_C).run(0x91),
                test.secondIsRegister(REG_D).run(0x92),
                test.secondIsRegister(REG_E).run(0x93),
                test.secondIsRegister(REG_H).run(0x94),
                test.secondIsRegister(REG_L).run(0x95),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
        );
    }

    @Test
    public void testSUI() throws Exception {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xD6)
        );
    }

    @Test
    public void testSBB() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x9F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x98),
                test.secondIsRegister(REG_C).run(0x99),
                test.secondIsRegister(REG_D).run(0x9A),
                test.secondIsRegister(REG_E).run(0x9B),
                test.secondIsRegister(REG_H).run(0x9C),
                test.secondIsRegister(REG_L).run(0x9D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x9E)
        );
    }

    @Test
    public void testSBI() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity());

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xDE)
        );
    }

    @Test
    public void testINR() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().parity().auxCarry(), context -> context.first + 1)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).firstIsRegister(REG_B).run(0x04),
                test.verifyRegister(REG_C).firstIsRegister(REG_C).run(0x0C),
                test.verifyRegister(REG_D).firstIsRegister(REG_D).run(0x14),
                test.verifyRegister(REG_E).firstIsRegister(REG_E).run(0x1C),
                test.verifyRegister(REG_H).firstIsRegister(REG_H).run(0x24),
                test.verifyRegister(REG_L).firstIsRegister(REG_L).run(0x2C),
                test.verifyRegister(REG_A).firstIsRegister(REG_A).run(0x3C),
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x34)
        );
    }

    @Test
    public void testDCR() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().parity().auxCarry(), context -> context.first - 1)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).firstIsRegister(REG_B).run(0x05),
                test.verifyRegister(REG_C).firstIsRegister(REG_C).run(0x0D),
                test.verifyRegister(REG_D).firstIsRegister(REG_D).run(0x15),
                test.verifyRegister(REG_E).firstIsRegister(REG_E).run(0x1D),
                test.verifyRegister(REG_H).firstIsRegister(REG_H).run(0x25),
                test.verifyRegister(REG_L).firstIsRegister(REG_L).run(0x2D),
                test.verifyRegister(REG_A).firstIsRegister(REG_A).run(0x3D),
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x35)
        );
    }

    @Test
    public void testINX() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x03),
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x13),
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x23),
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x33)
        );
    }

    @Test
    public void testDCX() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .clearOtherVerifiersAfterRun();
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x0B),
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x1B),
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x2B),
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x3B)
        );
    }

    @Test
    public void testDAD() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().carry15())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0x29)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0x09),
                test.secondIsPair(REG_PAIR_DE).run(0x19),
                test.secondIsPair(REG_SP).run(0x39)
        );
    }


}
