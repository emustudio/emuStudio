package net.sf.emustudio.ssem.memory.impl;

import emulib.plugins.memory.Memory;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class MemoryContextImplTest {

    @Test
    public void testAfterClearObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        listener.memoryChanged(eq(-1));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.clear();

        verify(listener);
    }

    @Test
    public void testReadWithoutWritReturnsZero() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(0L, (long)context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadAtInvalidLocationThrows() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.read(-1);
    }

    @Test
    public void testAfterReadNoObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        replay(listener);

        context.addMemoryListener(listener);
        context.read(10);

        verify(listener);
    }

    @Test
    public void testAfterWriteObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        listener.memoryChanged(eq(10));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.write(10, 1234);

        verify(listener);
    }

    @Test
    public void testWriteReallyWritesCorrectValueAtCorrectLocation() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(10, 1234);
        assertEquals(1234L, (int)context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteAtInvalidLocationThrows() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(-1, 1234);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, context.getSize());
    }

    @Test
    public void testClassTypeIsInteger() throws Exception {
        assertEquals(Integer.class, new MemoryContextImpl().getDataType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadWordIsNotSupported() throws Exception {
        new MemoryContextImpl().readWord(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testWriteWordIsNotSupported() throws Exception {
        new MemoryContextImpl().writeWord(0, new Integer[] {0});
    }
}
