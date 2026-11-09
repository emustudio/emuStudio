/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;

public class CpuImplTest {
    private CpuImpl cpu;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws PluginInitializationException {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        replay(memory);
        expect(contextPool.getMemory(0, Byte.class)).andReturn(memory).anyTimes();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        net.emustudio.emulib.application.ExecutionState execution = createNiceMock(net.emustudio.emulib.application.ExecutionState.class);
        replay(execution);
        expect(applicationApi.getExecutionState()).andReturn(execution).anyTimes();
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.cpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        this.cpu.initialize();
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
    public void interruptedExecutionReturnsContinuingStateForHostToResolve() throws Exception {
        Thread.currentThread().interrupt();
        try {
            assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_RUNNING, cpu.execute());
        } finally {
            Thread.interrupted();
        }
    }
}
