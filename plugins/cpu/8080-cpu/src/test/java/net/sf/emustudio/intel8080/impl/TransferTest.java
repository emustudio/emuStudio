package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;

public class TransferTest extends InstructionsTest {

    @Test
    public void testMVI() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier);

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A, context -> context.first & 0xFF).runWithOperand(0x3E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).runWithOperand(0x06)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).runWithOperand(0x0E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).runWithOperand(0x16)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).runWithOperand(0x1E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).runWithOperand(0x26)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).runWithOperand(0x2E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.setPair(REG_PAIR_HL, 0x20)
                        .verifyByte(0x20, context -> context.first & 0xFF)
                        .runWithOperand(0x36)
        );
    }

    @Test
    public void testMOV_A() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x7F),
                test.operandIsRegister(REG_B).run(0x78),
                test.operandIsRegister(REG_C).run(0x79),
                test.operandIsRegister(REG_D).run(0x7A),
                test.operandIsRegister(REG_E).run(0x7B),
                test.operandIsRegister(REG_H).run(0x7C),
                test.operandIsRegister(REG_L).run(0x7D),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x7E)
        );
    }

    @Test
    public void testMOV_B() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_B, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x47),
                test.operandIsRegister(REG_B).run(0x40),
                test.operandIsRegister(REG_C).run(0x41),
                test.operandIsRegister(REG_D).run(0x42),
                test.operandIsRegister(REG_E).run(0x43),
                test.operandIsRegister(REG_H).run(0x44),
                test.operandIsRegister(REG_L).run(0x45),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x46)
        );
    }

    @Test
    public void testMOV_C() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_C, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x4F),
                test.operandIsRegister(REG_B).run(0x48),
                test.operandIsRegister(REG_C).run(0x49),
                test.operandIsRegister(REG_D).run(0x4A),
                test.operandIsRegister(REG_E).run(0x4B),
                test.operandIsRegister(REG_H).run(0x4C),
                test.operandIsRegister(REG_L).run(0x4D),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x4E)
        );
    }

    @Test
    public void testMOV_D() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_D, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x57),
                test.operandIsRegister(REG_B).run(0x50),
                test.operandIsRegister(REG_C).run(0x51),
                test.operandIsRegister(REG_D).run(0x52),
                test.operandIsRegister(REG_E).run(0x53),
                test.operandIsRegister(REG_H).run(0x54),
                test.operandIsRegister(REG_L).run(0x55),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x56)
        );
    }

    @Test
    public void testMOV_E() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_E, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x5F),
                test.operandIsRegister(REG_B).run(0x58),
                test.operandIsRegister(REG_C).run(0x59),
                test.operandIsRegister(REG_D).run(0x5A),
                test.operandIsRegister(REG_E).run(0x5B),
                test.operandIsRegister(REG_H).run(0x5C),
                test.operandIsRegister(REG_L).run(0x5D),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x5E)
        );
    }

    @Test
    public void testMOV_H() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_H, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x67),
                test.operandIsRegister(REG_B).run(0x60),
                test.operandIsRegister(REG_C).run(0x61),
                test.operandIsRegister(REG_D).run(0x62),
                test.operandIsRegister(REG_E).run(0x63),
                test.operandIsRegister(REG_H).run(0x64),
                test.operandIsRegister(REG_L).run(0x65),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x66)
        );
    }

    @Test
    public void testMOV_L() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_L, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.operandIsRegister(REG_A).run(0x6F),
                test.operandIsRegister(REG_B).run(0x68),
                test.operandIsRegister(REG_C).run(0x69),
                test.operandIsRegister(REG_D).run(0x6A),
                test.operandIsRegister(REG_E).run(0x6B),
                test.operandIsRegister(REG_H).run(0x6C),
                test.operandIsRegister(REG_L).run(0x6D),
                test.setPair(REG_PAIR_HL, 0x20).operandIsMemoryByteAt(0x20).run(0x6E)
        );
    }

    @Test
    public void testMOV_M_r() throws Exception {
        final int address = 0x35;

        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyByte(address, context -> context.first & 0xFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_A).run(0x77),
                test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_B).run(0x70),
                test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_C).run(0x71),
                test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_D).run(0x72),
                test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_E).run(0x73)
        );
        test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_H).run(0x74).accept((byte)0);
        test.setPair(REG_PAIR_HL, address).operandIsRegister(REG_L).run(0x75).accept((byte)address);
    }

    @Test
    public void testLDAX() throws Exception {
        final int value = 0x25;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> value)
                .operandIsMemoryAddressByte(value)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(
                test.operandIsPair(REG_PAIR_BC).run(0x0A),
                test.operandIsPair(REG_PAIR_DE).run(0x1A)
        );
    }

    @Test
    public void testSTAX() throws Exception {
        final int value = 0x25;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyByte(context -> context.first, context -> value)
                .setRegister(REG_A, value)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(
                test.operandIsPair(REG_PAIR_BC).run(0x02),
                test.operandIsPair(REG_PAIR_DE).run(0x12)
        );
    }

    @Test
    public void testLDA() throws Exception {
        byte value = -120;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> value & 0xFF)
                .operandIsMemoryAddressByte(value);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x3A)
        );
    }

    @Test
    public void testSTA() throws Exception {
        byte value = -120;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyByte(context -> context.first, context -> value & 0xFF)
                .setRegister(REG_A, value);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x32)
        );
    }

    @Test
    public void testLHLD() throws Exception {
        int value = 0x1234;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_HL, context -> value)
                .operandIsMemoryAddressWord(value);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x2A)
        );
    }

    @Test
    public void testSHLD() throws Exception {
        int value = 0x1236;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyWord(context -> value, context -> context.first)
                .setPair(REG_PAIR_HL, value);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x22)
        );
    }

    @Test
    public void testLXI_B() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_BC, context -> context.first);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x01)
        );
    }

    @Test
    public void testLXI_D() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_DE, context -> context.first);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x11)
        );
    }

    @Test
    public void testLXI_H() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_HL, context -> context.first);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x21)
        );
    }

    @Test
    public void testLXI_SP() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitUnary(
                test.runWithOperand(0x31)
        );
    }

    @Test
    public void testSPHL() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_SP, context -> context.first);

        Generator.forSome16bitUnary(
                test.operandIsPair(REG_PAIR_HL).run(0xF9)
        );
    }

    @Test
    public void testXCHG() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REG_PAIR_DE, context -> context.first)
                .verifyPair(REG_PAIR_HL, context -> context.second)
                .firstIsPair(REG_PAIR_HL)
                .secondIsPair(REG_PAIR_DE);

        Generator.forSome16bitBinary(
                test.run(0xEB)
        );
    }

    @Test
    public void testXTHL() throws Exception {
        int address = 0x23;

        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyWord(context -> context.second, context -> address)
                .verifyPair(REG_PAIR_HL, context -> context.first)
                .firstIsMemoryWordAt(address)
                .secondIsPair(REG_PAIR_HL)
                .setPair(REG_SP, address);

        Generator.forSome16bitBinary(
                test.run(0xE3)
        );
    }

    @Test
    public void testMemoryOverflow() throws Exception {
        cpuRunner.resetProgram(new short[]{});
        cpuRunner.reset();
        cpuRunner.expectRunState(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT);
        cpuRunner.step();
    }


}
