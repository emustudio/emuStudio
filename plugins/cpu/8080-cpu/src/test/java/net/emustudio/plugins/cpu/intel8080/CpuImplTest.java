/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
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
    private CPUContext context;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws PluginInitializationException {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        replay(memory);
        expect(contextPool.getMemory(0, Byte.class)).andReturn(memory).anyTimes();
        contextPool.register(eq(0L), anyObject(), eq(CPUContext.class));
        expectLastCall().andAnswer(() -> { context = getCurrentArgument(1); return null; }).once();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
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
    public void contextReadsLiveInstructionLocationWithoutStatePublication() {
        cpu.getEngine().PC = 42;

        assertEquals(42, context.getInstructionLocation());
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
