package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.Utils.concat;
import static org.junit.Assert.assertEquals;

public class InstructionsMoveTest extends InstructionsTest {

    @Test
    public void testMemoryOverflow() throws Exception {
        program = new short[] {};
        resetProgram();

        cpu.step();

        assertEquals(
                CPU.RunState.STATE_STOPPED_ADDR_FALLOUT,
                runStateListener.runState
        );
    }

    @Test
    public void testMVI() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = fillGeneralRegisters(values);

        resetProgram();

        stepAndCheckGeneralRegisters(values);
    }

    @Test
    public void testMOV_A() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x7F, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D
        });

        resetProgram();
        stepCount(7);

        stepAndCheckRegister(values, EmulatorEngine.REG_A);
    }

    @Test
    public void testMOV_B() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x47, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,1,3,4,5,6,7}, EmulatorEngine.REG_B);
    }

    @Test
    public void testMOV_C() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x4F, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,2,2,4,5,6,7}, EmulatorEngine.REG_C);
    }

    @Test
    public void testMOV_D() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x57, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,2,3,3,5,6,7}, EmulatorEngine.REG_D);
    }

    @Test
    public void testMOV_E() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x5F, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,2,3,4,4,6,7}, EmulatorEngine.REG_E);
    }

    @Test
    public void testMOV_H() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x67, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,2,3,4,5,5,7}, EmulatorEngine.REG_H);
    }

    @Test
    public void testMOV_L() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,6,7 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x6F, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D
        });
        resetProgram();
        stepCount(7);

        stepAndCheckRegister(new short[] { 1,2,3,4,5,6,6}, EmulatorEngine.REG_L);
    }

}
