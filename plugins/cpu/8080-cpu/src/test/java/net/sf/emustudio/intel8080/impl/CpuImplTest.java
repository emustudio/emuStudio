package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.Utils.concat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class CpuImplTest {
    private static final long PLUGIN_ID = 0L;

    private CpuImpl cpu;
    private MemoryStub memoryStub;
    private RunStateListener runStateListener;

    private short[] program;

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        runStateListener = new RunStateListener();
        cpu = new CpuImpl(PLUGIN_ID, contextPool);
        cpu.addCPUListener(runStateListener);

        // simulate emuStudio boot
        cpu.initialize(EasyMock.createNiceMock(SettingsManager.class));
    }

    @After
    public void tearDown() {
        cpu.destroy();
        program = null;
    }

    private void resetProgram() {
        memoryStub.setMemory(program);
        cpu.reset();
    }

    private void stepAndCheck(int value, int register) {
        cpu.step();
        assertEquals(value, cpu.getEngine().regs[register]);
    }

    private void stepCount(int count) {
        for (int i = 0; i < count; i++) {
            cpu.step();
        }
    }

    private short[] fillGeneralRegisters(short[] values) {
        return new short[]{
                0x3E, values[0], // MVI A,byte
                0x06, values[1], // MVI B,byte
                0x0E, values[2], // MVI C,byte
                0x16, values[3], // MVI D,byte
                0x1E, values[4], // MVI E,byte
                0x26, values[5], // MVI H,byte
                0x2E, values[6], // MVI L,byte
        };
    }

    private void stepAndCheckGeneralRegisters(short[] values) {
        stepAndCheck(values[0], EmulatorEngine.REG_A);
        stepAndCheck(values[1], EmulatorEngine.REG_B);
        stepAndCheck(values[2], EmulatorEngine.REG_C);
        stepAndCheck(values[3], EmulatorEngine.REG_D);
        stepAndCheck(values[4], EmulatorEngine.REG_E);
        stepAndCheck(values[5], EmulatorEngine.REG_H);
        stepAndCheck(values[6], EmulatorEngine.REG_L);
    }

    private void stepAndCheckRegister(short[] values, int register) {
        for (short value : values) {
            stepAndCheck(value, register);
        }
    }

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