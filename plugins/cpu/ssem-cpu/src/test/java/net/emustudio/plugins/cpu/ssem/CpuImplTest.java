/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CpuImplTest {
    private CpuImpl cpu;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.cpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", cpu.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", cpu.getCopyright());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(cpu.getDescription());
        assertFalse(cpu.getDescription().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private CpuImpl createInitializedCpu() throws PluginInitializationException {
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        expect(memory.getSize()).andReturn(128).anyTimes(); // 32 lines * 4 bytes
        expect(memory.read(anyInt())).andReturn((byte) 0).anyTimes();
        expect(memory.read(anyInt(), anyInt())).andReturn(new Byte[]{0, 0, 0, 0}).anyTimes();
        replay(memory);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        CpuImpl result = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        result.initialize();
        return result;
    }

    @SuppressWarnings("unchecked")
    private CpuImpl createInitializedCpuWithMemory(short[] memoryContent) throws PluginInitializationException {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(memoryContent);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(anyLong(), eq(MemoryContext.class))).andReturn(memory).anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        CpuImpl result = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        result.initialize();
        return result;
    }

    @Test
    public void testInitializeSuccess() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            assertNotNull(initialized.getEngine());
            assertNotNull(initialized.getDisassembler());
        } finally {
            initialized.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(expected = PluginInitializationException.class)
    public void testInitializeWithWrongCellType() throws PluginInitializationException {
        MemoryContext<Integer> memory = createNiceMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Integer.class).anyTimes();
        replay(memory);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        CpuImpl badCpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        try {
            badCpu.initialize();
        } finally {
            badCpu.destroy();
        }
    }

    @Test
    public void testGetInstructionLocation() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            initialized.getEngine().CI.set(0);
            assertEquals(4, initialized.getInstructionLocation());
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testGetInstructionLocationAfterCI16() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            initialized.getEngine().CI.set(16);
            assertEquals(20, initialized.getInstructionLocation());
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testSetInstructionLocation() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            assertTrue(initialized.setInstructionLocation(5));
            // CI = max(0, 5*4 - 4) = 16
            assertEquals(16, initialized.getEngine().CI.get());
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testSetInstructionLocationZero() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            assertTrue(initialized.setInstructionLocation(0));
            // CI = max(0, 0*4 - 4) = max(0, -4) = 0
            assertEquals(0, initialized.getEngine().CI.get());
        } finally {
            initialized.destroy();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInstructionLocationNegative() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            initialized.setInstructionLocation(-1);
        } finally {
            initialized.destroy();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInstructionLocationOutOfRange() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            initialized.setInstructionLocation(128); // >= memSize (128)
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testGetStatusPanelWithNoGui() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            // applicationApi.getGUI() returns null (niceMock)
            assertNull(initialized.getStatusPanel());
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testGetEngine() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            EmulatorEngine engine = initialized.getEngine();
            assertNotNull(engine);
            // Engine should have default CI and Acc
            assertNotNull(engine.CI);
            assertNotNull(engine.Acc);
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testDestroyWithoutAutoEmulation() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        // destroy should not throw even without automaticEmulation
        initialized.destroy();
    }

    @Test
    public void testResetInternal() throws PluginInitializationException {
        CpuImpl initialized = createInitializedCpu();
        try {
            initialized.reset(10);
            assertEquals(0, initialized.getEngine().Acc.get());
            assertEquals(10, initialized.getEngine().CI.get());
        } finally {
            initialized.destroy();
        }
    }

    @Test(timeout = 2000)
    public void testCallWithSTP() throws PluginInitializationException {
        // STP at line 1 (byte position 4): byte[0]=0x00, byte[1]=0x07
        short[] memContent = new short[32 * 4];
        memContent[4] = 0x00;
        memContent[5] = 0x07;
        CpuImpl initialized = createInitializedCpuWithMemory(memContent);
        try {
            initialized.reset(0);
            CPU.RunState state = initialized.call();
            assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, state);
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testStepReturnsRunningForLDN() throws Exception {
        // LDN at line 1 (byte position 4): byte[0]=0x00, byte[1]=0x02 (opcode 010)
        // LDN loads -mem[line] into Acc, returns STATE_RUNNING
        short[] memContent = new short[32 * 4];
        memContent[4] = 0x00; // line = 0
        memContent[5] = 0x02; // opcode = 010 (LDN)
        CpuImpl initialized = createInitializedCpuWithMemory(memContent);
        try {
            initialized.reset(0);
            // engine.step() returns the raw RunState (before CpuImpl wraps it)
            CPU.RunState state = initialized.getEngine().step();
            assertEquals(CPU.RunState.STATE_RUNNING, state);
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testStepReturnsStoppedNormalForSTP() throws Exception {
        // STP at line 1 (byte position 4): returns STATE_STOPPED_NORMAL
        short[] memContent = new short[32 * 4];
        memContent[4] = 0x00;
        memContent[5] = 0x07; // STP
        CpuImpl initialized = createInitializedCpuWithMemory(memContent);
        try {
            initialized.reset(0);
            CPU.RunState state = initialized.getEngine().step();
            assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, state);
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testStepInternalConvertsRunningToStoppedBreak() throws Exception {
        // LDN returns STATE_RUNNING from engine.step()
        // stepInternal should convert it to STATE_STOPPED_BREAK
        short[] memContent = new short[32 * 4];
        memContent[4] = 0x00; // line = 0
        memContent[5] = 0x02; // opcode = 010 (LDN)
        CpuImpl initialized = createInitializedCpuWithMemory(memContent);
        try {
            initialized.reset(0);
            final CPU.RunState[] capturedState = new CPU.RunState[1];
            initialized.addCPUListener(new CPU.CPUListener() {
                @Override
                public void runStateChanged(CPU.RunState state) {
                    capturedState[0] = state;
                }

                @Override
                public void internalStateChanged() {
                }
            });
            initialized.step(); // calls stepInternal -> STATE_RUNNING -> STATE_STOPPED_BREAK
            assertEquals(CPU.RunState.STATE_STOPPED_BREAK, capturedState[0]);
        } finally {
            initialized.destroy();
        }
    }

    @Test
    public void testStepInternalForwardsSTPState() throws Exception {
        // STP returns STATE_STOPPED_NORMAL from engine.step()
        // stepInternal should forward it as-is
        short[] memContent = new short[32 * 4];
        memContent[4] = 0x00;
        memContent[5] = 0x07; // STP
        CpuImpl initialized = createInitializedCpuWithMemory(memContent);
        try {
            initialized.reset(0);
            final CPU.RunState[] capturedState = new CPU.RunState[1];
            initialized.addCPUListener(new CPU.CPUListener() {
                @Override
                public void runStateChanged(CPU.RunState state) {
                    capturedState[0] = state;
                }

                @Override
                public void internalStateChanged() {
                }
            });
            initialized.step(); // calls stepInternal -> STATE_STOPPED_NORMAL
            assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, capturedState[0]);
        } finally {
            initialized.destroy();
        }
    }
}
