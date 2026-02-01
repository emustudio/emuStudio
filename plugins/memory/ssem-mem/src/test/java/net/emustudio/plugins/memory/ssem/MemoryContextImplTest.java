/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MemoryContextImplTest {

    @Test
    public void testAfterClearObserversAreNotified() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        MemoryContext.MemoryListener listener = createMock(MemoryContext.MemoryListener.class);
        listener.memoryContentChanged(eq(-1), eq(-1));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.clear();

        verify(listener);
    }

    @Test
    public void testReadWithoutWritReturnsZero() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        assertEquals(0L, (long) context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadAtInvalidLocationThrows() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        context.read(-1);
    }

    @Test
    public void testAfterReadNoObserversAreNotified() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        MemoryContext.MemoryListener listener = createMock(MemoryContext.MemoryListener.class);
        replay(listener);

        context.addMemoryListener(listener);
        context.read(10);

        verify(listener);
    }

    @Test
    public void testAfterWriteObserversAreNotified() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        MemoryContext.MemoryListener listener = createMock(MemoryContext.MemoryListener.class);
        listener.memoryContentChanged(eq(10), eq(10));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.write(10, (byte) 134);

        verify(listener);
    }

    @Test
    public void testWriteReallyWritesCorrectValueAtCorrectLocation() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        context.write(10, (byte) 134);
        assertEquals((byte) 134, (byte) context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteAtInvalidLocationThrows() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        context.write(-1, (byte) 134);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, context.getSize());
    }

    @Test
    public void testClassTypeIsByte() {
        assertEquals(Byte.class, new MemoryContextImpl(new Annotations()).getCellTypeClass());
    }

    @Test
    public void testReadArrayIsSupported() {
        assertArrayEquals(new Byte[]{0, 0, 0, 0}, new MemoryContextImpl(new Annotations()).read(0, 4));
    }

    @Test
    public void testWriteArrayIsSupported() {
        MemoryContextImpl mem = new MemoryContextImpl(new Annotations());

        Byte[] row = new Byte[]{1, 2, 3, 4};
        mem.write(0, row);

        assertArrayEquals(row, mem.read(0, 4));
    }
}
