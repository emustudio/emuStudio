package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;

public class ControlTest extends InstructionsTest {

    @Test
    public void testEI_DI() throws Exception {
        cpuRunner.setProgram(0xFB, 0xF3);
        cpuRunner.reset();
        cpuRunner.step();
        cpuVerifier.checkInterruptsAreEnabled();
        cpuRunner.step();
        cpuVerifier.checkInterruptsAreDisabled();
    }

    @Test
    public void testJMP() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(
                test.runWithOperand(0xC3),
                test.runWithOperand(0xC2),
                test.setFlags(FLAG_Z).runWithOperand(0xCA),
                test.runWithOperand(0xD2),
                test.setFlags(FLAG_C).runWithOperand(0xDA),
                test.runWithOperand(0xE2),
                test.setFlags(FLAG_P).runWithOperand(0xEA),
                test.runWithOperand(0xF2),
                test.setFlags(FLAG_S).runWithOperand(0xFA)
        );

        test.clearVerifiers().verifyPC(context -> (context.PC + 3) & 0xFFFF);
        Generator.forSome16bitUnary(
                test.setFlags(FLAG_Z).runWithOperand(0xC2),
                test.runWithOperand(0xCA),
                test.setFlags(FLAG_C).runWithOperand(0xD2),
                test.runWithOperand(0xDA),
                test.setFlags(FLAG_P).runWithOperand(0xE2),
                test.runWithOperand(0xEA),
                test.setFlags(FLAG_S).runWithOperand(0xF2),
                test.runWithOperand(0xFA)
        );
    }

    @Test
    public void testCALL() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.second)
                .verifyWord(context -> context.PC + 3, context -> context.first - 2)
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

        test.clearVerifiers().verifyPC(context -> (context.PC + 3) & 0xFFFF);
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
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
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
        test.clearVerifiers()
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
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_SP, context -> context.first - 2)
                .verifyWord(context -> 1, context -> context.SP - 2)
                .operandIsPair(REG_SP)
                .keepCurrentInjectorsAfterRun();

        List<Consumer<RunnerContext<Integer>>> verifiers = test.getVerifiers();

        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0).run(0xC7)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x8).run(0xCF)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x10).run(0xD7)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x18).run(0xDF)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x20).run(0xE7)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x28).run(0xEF)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x30).run(0xF7)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x38).run(0xFF)
        );
    }

    @Test
    public void testPCHL() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.first)
                .operandIsPair(REG_PAIR_HL);

        Generator.forSome16bitUnary(
                test.run(0xE9)
        );
    }

    @Test
    public void testHLT() throws Exception {
        cpuRunner.setProgram(0x76);
        cpuRunner.reset();
        cpuRunner.expectRunState(CPU.RunState.STATE_STOPPED_NORMAL);
        cpuRunner.step();
    }


}
