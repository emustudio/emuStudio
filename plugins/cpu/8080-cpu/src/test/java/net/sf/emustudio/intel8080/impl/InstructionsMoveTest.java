package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import org.junit.Before;
import org.junit.Test;

public class InstructionsMoveTest extends InstructionsTest {
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

}
