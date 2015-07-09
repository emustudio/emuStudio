package net.sf.emustudio.intel8080.impl;

import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
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

    private TestBuilder.BinaryByte additionTestBuilder() {
        return new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();
    }

    private TestBuilder.BinaryByte subtractionTestBuilder() {
        return new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testADD() throws Exception {
        TestBuilder.BinaryByte test = additionTestBuilder();

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
        TestBuilder.BinaryByte test = additionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xC6)
        );
    }

    @Test
    public void testADC() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A)
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
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xCE)
        );
    }

    @Test
    public void testSUB() throws Exception {
        TestBuilder.BinaryByte test = subtractionTestBuilder();

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
        TestBuilder.BinaryByte test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xD6)
        );
    }

    @Test
    public void testSBB() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A)
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
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().carry().auxCarry().parity())
                .firstIsRegister(REG_A);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xDE)
        );
    }

    @Test
    public void testINR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyFlags(new FlagsBuilder().sign().zero().parity().auxCarry(), context -> context.first + 1)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).operandIsRegister(REG_B).run(0x04)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).operandIsRegister(REG_C).run(0x0C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).operandIsRegister(REG_D).run(0x14)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).operandIsRegister(REG_E).run(0x1C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).operandIsRegister(REG_H).run(0x24)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).operandIsRegister(REG_L).run(0x2C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A).operandIsRegister(REG_A).run(0x3C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).operandIsMemoryByteAt(1).run(0x34)
        );
    }

    @Test
    public void testDCR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyFlags(new FlagsBuilder().sign().zero().parity().auxCarry(), context -> context.first - 1)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).operandIsRegister(REG_B).run(0x05)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).operandIsRegister(REG_C).run(0x0D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).operandIsRegister(REG_D).run(0x15)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).operandIsRegister(REG_E).run(0x1D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).operandIsRegister(REG_H).run(0x25)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).operandIsRegister(REG_L).run(0x2D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A).operandIsRegister(REG_A).run(0x3D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).operandIsMemoryByteAt(1).run(0x35)
        );
    }

    @Test
    public void testINX() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).operandIsPair(REG_PAIR_BC).run(0x03)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_DE, verifier).operandIsPair(REG_PAIR_DE).run(0x13)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_HL, verifier).operandIsPair(REG_PAIR_HL).run(0x23)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_SP, verifier).operandIsPair(REG_SP).run(0x33)
        );
    }

    @Test
    public void testDCX() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).operandIsPair(REG_PAIR_BC).run(0x0B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_DE, verifier).operandIsPair(REG_PAIR_DE).run(0x1B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_HL, verifier).operandIsPair(REG_PAIR_HL).run(0x2B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_SP, verifier).operandIsPair(REG_SP).run(0x3B)
        );
    }

    @Test
    public void testDAD() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsBuilder().carry15())
                .firstIsPair(REG_PAIR_HL)
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
