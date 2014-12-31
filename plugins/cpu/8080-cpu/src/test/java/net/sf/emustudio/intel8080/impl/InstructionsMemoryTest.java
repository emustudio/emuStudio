package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

import static org.junit.Assert.*;

public class InstructionsMemoryTest extends InstructionsTest {

    @Test
    public void testMOVtoGeneralRegisters() throws Exception {
        resetProgram(
                0x7E, 0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E,0,
                0xFF // address 8
        );

        setRegisters(0,0,0,0,0,0,8);
        stepAndCheckRegisters(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);
        setRegister(EmulatorEngine.REG_H, 0); // hack
        stepAndCheck(0xFF, EmulatorEngine.REG_L);
    }

    @Test
    public void testMOVgeneralRegistersToMemory() throws Exception {
        resetProgram(
                0x77, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75,0,
                0xFF // address 8
        );

        setRegisters(1,2,3,4,5,0,8);
        stepAndCheckMemory(8, 1,2,3,4,5,0,8);
    }

    @Test
    public void testLDAX() throws Exception {
        resetProgram(
                0x0A, 0x1A, 0,
                0xFF // address 3
        );

        setRegisters(0,0,3,0,3,0,0);
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        setRegister(EmulatorEngine.REG_A,  0); // hack
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }

    @Test
    public void testSTAX() throws Exception {
        resetProgram(
                0x02, 0x12, 0,
                0, // address 3
                0  // address 4
        );

        setRegisters(0xFF,0,3,0,4,0,0);
        stepAndCheckMemory(0xFF, 3);
        stepAndCheckMemory(0xFF, 4);
    }

    @Test
    public void testLDA() throws Exception {
        resetProgram(
                0x3A, 5, 0, 0, 0,
                0xFF //address 5
        );

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }

    @Test
    public void testSTA() throws Exception {
        resetProgram(
                0x32, 5, 0, 0, 0,
                0xFF //address 5
        );

        setRegisters(0xAB);
        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testMVI() throws Exception {
        resetProgram(
                0x36, 0xAB, 0, 0, 0,
                0xFF //address 5
        );

        setRegister(EmulatorEngine.REG_L, 5);
        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testLHLD() throws Exception {
        resetProgram(
                0x2A, 5, 0, 0, 0,
                0xAB, //address 5
                0xCD  //address 6
        );

        stepAndCheck(0xCD, EmulatorEngine.REG_H);
        stepAndCheck(0xAB, EmulatorEngine.REG_L);
    }

    @Test
    public void testSHLD() throws Exception {
        resetProgram(
                0x22, 5, 0, 0, 0,
                0xFF, //address 5
                0xFF  //address 6
        );

        setRegister(EmulatorEngine.REG_H, 0xCD);
        setRegister(EmulatorEngine.REG_L, 0xAB);
        stepAndCheckMemory(0xAB, 5);
        stepAndCheckMemory(0xCD, 6);
    }

    @Test
    public void testXTHL() throws Exception {
        resetProgram(
                0xE3,
                0xAB, // address 1
                0xCD  // address 2
        );

        setRegister(EmulatorEngine.REG_H, 0x34);
        setRegister(EmulatorEngine.REG_L, 0x12);
        cpu.getEngine().SP = 1;

        stepAndCheckMemory(0x12, 1);
        assertEquals(0x34, (int) memoryStub.read(2));
        checkRegister(EmulatorEngine.REG_L, 0xAB);
        checkRegister(EmulatorEngine.REG_H, 0xCD);
    }

    @Test
    public void testADD() throws Exception {
        resetProgram(
                0x86,
                2 // address 1
        );

        // with overflow, signed
        setRegister(EmulatorEngine.REG_A, 0xFE);
        setFlags(EmulatorEngine.FLAG_S);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC);
        checkNotFlags(EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADC() throws Exception {
        resetProgram(
                0x8E,
                2 // address 1
        );

        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(3, EmulatorEngine.REG_A);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z |
                EmulatorEngine.FLAG_AC
        );
    }

    @Test
    public void testSUB() throws Exception {
        resetProgram(
                0x96,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
                | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }

    @Test
    public void testSBB() throws Exception {
        resetProgram(
                0x9E,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_A, 2);
        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
        );
        checkFlags(EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P);
    }

    @Test
    public void testINR() throws Exception {
        resetProgram(
                0x34,
                0xFF // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemory(0, 1);
        checkFlags(
                EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC
                | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_S);
    }

    @Test
    public void testDCR() throws Exception {
        resetProgram(
                0x35,
                0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemory(0xFF, 1);
        checkFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
                | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }

    @Test
    public void testANA() throws Exception {
        resetProgram(
                0xA6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0xC0, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC);
    }

    @Test
    public void testXRA() throws Exception {
        resetProgram(
                0xAE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0x3F, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C |
                EmulatorEngine.FLAG_AC
        );
    }

    @Test
    public void testORA() throws Exception {
        resetProgram(
                0xB6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(
                EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
        );
    }

    @Test
    public void testCMP() throws Exception {
        resetProgram(
                0xBE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C
                | EmulatorEngine.FLAG_AC
        );
        checkNotFlags(
                EmulatorEngine.FLAG_Z
        );
    }

}
