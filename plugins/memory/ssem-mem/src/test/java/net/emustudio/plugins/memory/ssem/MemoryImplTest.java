/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

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
    public void testGetSizeReturnsNumberOfCells() {
        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, memory.getSize());
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
        expect(mockSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        MemoryImpl mem = new MemoryImpl(0L, applicationApi, mockSettings);
        mem.showSettings(null); // should not throw or create GUI
    }

    @Test
    public void testConstructorHandlesInvalidContextException() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().andThrow(new InvalidContextException("test error"));
        replay(contextPool);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(dialogs).anyTimes();
        replay(applicationApi);

        // Should not throw - error is caught and logged
        MemoryImpl mem = new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
        assertNotNull(mem);
    }

    @Test
    public void testConstructorHandlesContextAlreadyRegisteredException() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().andThrow(new ContextAlreadyRegisteredException());
        replay(contextPool);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(dialogs).anyTimes();
        replay(applicationApi);

        // Should not throw - error is caught and logged
        MemoryImpl mem = new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
        assertNotNull(mem);
    }
}
