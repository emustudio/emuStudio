package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
}