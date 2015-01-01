package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstructionsTransferTest extends InstructionsTest {
    private int[] defaultValues;

    @Before
    public void setupValues() {
        defaultValues = new int[] { 1,2,3,4,5,6,7 };
    }

    @Test
    public void testMemoryOverflow() throws Exception {
        resetProgram(new short[] {});

        cpu.step();
        checkRunState(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT);
    }

    @Test
    public void testMVI() throws Exception {
        resetProgram(generateMVI(defaultValues));
        stepAndCheckRegisters(defaultValues);
    }

    @Test
    public void testMVI_M() throws Exception {
        resetProgram(
                0x36, 0xAB, 0, 0, 0,
                0xFF //address 5
        );

        setRegister(EmulatorEngine.REG_L, 5);
        stepAndCheckMemory(0xAB, 5);
    }


    @Test
    public void testMOV_A() throws Exception {
        resetProgram(0x7F, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D);
        setRegisters(defaultValues);

        stepAndCheckRegister(EmulatorEngine.REG_A, defaultValues);
    }

    @Test
    public void testMOV_B() throws Exception {
        resetProgram(0x47, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_B, 1,1,3,4,5,6,7);
    }

    @Test
    public void testMOV_C() throws Exception {
        resetProgram(0x4F, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_C, 1,2,2,4,5,6,7);
    }

    @Test
    public void testMOV_D() throws Exception {
        resetProgram(0x57, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_D, 1,2,3,3,5,6,7);
    }

    @Test
    public void testMOV_E() throws Exception {
        resetProgram(0x5F, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_E, 1,2,3,4,4,6,7);
    }

    @Test
    public void testMOV_H() throws Exception {
        resetProgram(0x67, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_H, 1,2,3,4,5,5,7);
    }

    @Test
    public void testMOV_L() throws Exception {
        resetProgram(0x6F, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_L, 1,2,3,4,5,6,6);
    }

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
    public void testLXI_B() throws Exception {
        resetProgram(
                0x01, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_C);
        checkRegister(EmulatorEngine.REG_B, 0x34);
    }

    @Test
    public void testLXI_D() throws Exception {
        resetProgram(
                0x11, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_E);
        checkRegister(EmulatorEngine.REG_D, 0x34);
    }

    @Test
    public void testLXI_H() throws Exception {
        resetProgram(
                0x21, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_L);
        checkRegister(EmulatorEngine.REG_H, 0x34);
    }

    @Test
    public void testLXI_SP() throws Exception {
        resetProgram(
                0x31, 0x12, 0x34
        );
        cpu.step();
        assertEquals(0x3412, cpu.getEngine().SP);
    }

    @Test
    public void testSPHL() throws Exception {
        resetProgram(0xF9);

        setRegister(EmulatorEngine.REG_H, 0x12);
        setRegister(EmulatorEngine.REG_L, 0x34);

        cpu.step();
        assertEquals(0x1234, cpu.getEngine().SP);
    }

    @Test
    public void testXCHG() throws Exception {
        resetProgram(0xEB);

        setRegister(EmulatorEngine.REG_H, 0x12);
        setRegister(EmulatorEngine.REG_L, 0x34);

        stepAndCheck(0x12, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0x34);

        checkRegister(EmulatorEngine.REG_H, 0);
        checkRegister(EmulatorEngine.REG_L, 0);
    }

    @Test
    public void testXTHL() throws Exception {
        resetProgram(
                0xE3, 0,
                0x34, // address 2
                0x12  // address 3
        );

        cpu.getEngine().SP = 2;

        stepAndCheck(0x12, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x34);

        checkMemory(0, 2);
        checkMemory(0, 3);
    }

}
