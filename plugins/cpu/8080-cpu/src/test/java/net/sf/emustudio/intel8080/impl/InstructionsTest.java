package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public abstract class InstructionsTest {
    protected static final long PLUGIN_ID = 0L;

    protected CpuImpl cpu;
    protected MemoryStub memoryStub;
    protected RunStateListenerStub runStateListener;

    protected short[] program;

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        runStateListener = new RunStateListenerStub();
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

    protected void resetProgram() {
        memoryStub.setMemory(program);
        cpu.reset();
    }

    protected void stepAndCheck(int value, int register) {
        cpu.step();
        assertEquals(value, cpu.getEngine().regs[register]);
    }

    protected void stepAndCheckMemory(int value, int address) {
        cpu.step();
        assertEquals(value, (int)memoryStub.read(address));
    }

    protected void stepCount(int count) {
        for (int i = 0; i < count; i++) {
            cpu.step();
        }
    }

    protected short[] fillGeneralRegisters(short[] values) {
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

    protected void stepAndCheckGeneralRegisters(short value) {
        stepAndCheck(value, EmulatorEngine.REG_A);
        stepAndCheck(value, EmulatorEngine.REG_B);
        stepAndCheck(value, EmulatorEngine.REG_C);
        stepAndCheck(value, EmulatorEngine.REG_D);
        stepAndCheck(value, EmulatorEngine.REG_E);
        stepAndCheck(value, EmulatorEngine.REG_H);
        stepAndCheck(value, EmulatorEngine.REG_L);
    }

    protected void stepAndCheckGeneralRegisters(short[] values) {
        stepAndCheck(values[0], EmulatorEngine.REG_A);
        stepAndCheck(values[1], EmulatorEngine.REG_B);
        stepAndCheck(values[2], EmulatorEngine.REG_C);
        stepAndCheck(values[3], EmulatorEngine.REG_D);
        stepAndCheck(values[4], EmulatorEngine.REG_E);
        stepAndCheck(values[5], EmulatorEngine.REG_H);
        stepAndCheck(values[6], EmulatorEngine.REG_L);
    }

    protected void stepAndCheckRegister(short[] values, int register) {
        for (short value : values) {
            stepAndCheck(value, register);
        }
    }

}