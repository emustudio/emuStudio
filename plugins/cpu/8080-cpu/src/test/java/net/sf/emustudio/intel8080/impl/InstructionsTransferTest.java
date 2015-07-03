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
