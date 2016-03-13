package net.sf.emustudio.ssem.memory.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MemoryImplTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullContextPoolThrows() throws Exception {
        new MemoryImpl(0L, null);
    }

    @Test
    public void testMemoryContextIsRegisteredInContextPool() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().once();
        replay(contextPool);

        new MemoryImpl(0L, contextPool);

        verify(contextPool);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() throws Exception {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, memory.getSize());
    }

    @Test
    public void testIsShowSettingsSupportedReturnsTrueByDefault() throws Exception {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertTrue(memory.isShowSettingsSupported());
    }

    @Test
    public void testInitializeWithGUIdoesNotThrow() throws Exception {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        // just to increase coverage
        memory.initialize(createNiceMock(SettingsManager.class));
    }

    @Test
    public void testGetVersionDoesNotReturnNull() throws Exception {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertNotNull(memory.getVersion());
    }
}
