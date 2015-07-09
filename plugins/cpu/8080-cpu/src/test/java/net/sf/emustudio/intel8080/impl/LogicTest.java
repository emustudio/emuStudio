package net.sf.emustudio.intel8080.impl;

import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import org.junit.Test;

import java.util.function.Function;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_AC;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;

public class LogicTest extends InstructionsTest {

    private TestBuilder.BinaryByte logicTest(Function<RunnerContext<Byte>, Integer> operation) {
        return new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, operation)
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().parity().carryIsReset().auxCarryIsReset())
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testANA() throws Exception {
        TestBuilder.BinaryByte test = logicTest(context -> context.first & context.second);

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
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xA6)
        );
    }

    @Test
    public void testANI() throws Exception {
        TestBuilder.BinaryByte test = logicTest(context -> context.first & context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xE6)
        );
    }

    @Test
    public void testXRA() throws Exception {
        TestBuilder.BinaryByte test = logicTest(context -> context.first ^ context.second);

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
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xAE)
        );
    }

    @Test
    public void testXRI() throws Exception {
        TestBuilder.BinaryByte test = logicTest(context -> context.first ^ context.second);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xEE)
        );
    }

    @Test
    public void testORA() throws Exception {
        TestBuilder.BinaryByte test = logicTest(context -> context.first | context.second);

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
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xB6)
        );
    }

    @Test
    public void testORI() throws Exception {
        Function<RunnerContext<Byte>, Integer> op = context -> context.first | context.second;

        Generator.forSome8bitBinary(
                logicTest(op).runWithSecondOperand(0xF6)
        );
    }

    @Test
    public void testDAA() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> {
                    int result = ((int)context.first)& 0xFF;
                    if (((context.flags & FLAG_AC) == FLAG_AC) || (result & 0x0F) > 9) {
                        result += 6;
                    }
                    if ((context.flags & FLAG_C) == FLAG_C || (result & 0xF0) > 0x90) {
                        result += 0x60;
                    }
                    return result;
                })
                .verifyFlagsOfLastOp(new FlagsBuilder().sign().zero().parity().carry().auxCarryDAA())
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x27)
        );
    }

    @Test
    public void testCMA() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (~context.first) & 0xFF)
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x2F)
        );
    }

    @Test
    public void testSTC() throws Exception {
        cpuRunner.setProgram(0x37);
        cpuRunner.reset();
        cpuRunner.step();

        cpuVerifier.checkFlags(FLAG_C);
        cpuVerifier.checkNotFlags(FLAG_S | FLAG_Z | FLAG_AC | FLAG_P);
    }

    @Test
    public void testCMC() throws Exception {
        cpuRunner.setProgram(0x3F);
        cpuRunner.reset();
        cpuRunner.setFlags(FLAG_C);
        cpuRunner.step();

        cpuVerifier.checkNotFlags(FLAG_C | FLAG_S | FLAG_Z | FLAG_AC | FLAG_P);
    }

    @Test
    public void testRLC() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first << 1) | ((context.first >>> 7) & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().carryIsFirstOperandMSB())
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x07)
        );
    }

    @Test
    public void testRRC() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (((context.first &0xFF) >>> 1) | ((context.first & 1) << 7)) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsBuilder().carryIsFirstOperandLSB())
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x0F)
        );
    }

    @Test
    public void testRAL() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (context.first << 1) | (context.flags & 1))
                .verifyFlagsOfLastOp(new FlagsBuilder().carryIsFirstOperandMSB())
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x17)
        );
    }

    @Test
    public void testRAR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> (((context.first &0xFF) >>> 1) | ((context.flags & 1) << 7)) & 0xFF)
                .verifyFlagsOfLastOp(new FlagsBuilder().carryIsFirstOperandLSB())
                .operandIsRegister(REG_A);

        Generator.forSome8bitUnary(
                test.run(0x1F)
        );
    }

    @Test
    public void testCMP() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> ((Number)context.first).intValue())
                .verifyFlags(
                        new FlagsBuilder().sign().zero().carry().auxCarry().parity(),
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
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0xBE)
        );
    }

    @Test
    public void testCPI() throws Exception {
        FlagsBuilder flagsToCheck = new FlagsBuilder().sign().zero().carry().auxCarry().parity();
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> ((Number)context.first).intValue())
                .verifyFlags(flagsToCheck, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .firstIsRegister(REG_A);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xFE)
        );
    }


}
