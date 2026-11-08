/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryImplTest {
    private MemoryImpl memory;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        memory = new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", memory.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", memory.getCopyright());
    }

    @Test
    public void testMemoryContextIsRegisteredInContextPool() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().once();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);

        verify(contextPool);
    }

    @Test
    public void testRegisteredContextSizeIsNumberOfCells() {
        Capture<MemoryContext> context = newCapture();
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), capture(context), same(MemoryContext.class));
        expectLastCall().once();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, context.getValue().getSize());
    }

    @Test
    public void testIsShowSettingsSupportedReturnsTrueByDefault() {
        assertTrue(memory.isShowSettingsSupported());
    }

    @Test
    public void testGetVersionDoesNotReturnNull() {
        assertNotNull(memory.getVersion());
    }

    @Test
    public void testGetCopyrightDoesNotReturnNull() {
        assertNotNull(memory.getCopyright());
    }

    @Test
    public void testGetDescriptionReturnsExpectedValue() {
        assertEquals("Main store for SSEM machine", memory.getDescription());
    }

    @Test
    public void testDestroyDoesNotThrow() {
        memory.destroy();
    }

    @Test
    public void testShowSettingsWithGuiNotSupportedDoesNothing() {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI)).andReturn(Optional.of(true)).anyTimes();
        replay(mockSettings);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        MemoryImpl mem = new MemoryImpl(0L, applicationApi, mockSettings);
        mem.showSettings(null); // should not throw or create GUI
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorPropagatesInvalidContext() {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().andThrow(new IllegalArgumentException("test error"));
        replay(contextPool);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(dialogs).anyTimes();
        replay(applicationApi);

        new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorPropagatesAlreadyRegisteredContext() {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().andThrow(new IllegalStateException("already registered"));
        replay(contextPool);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(dialogs).anyTimes();
        replay(applicationApi);

        new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
    }
}
