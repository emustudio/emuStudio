/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryContextImplTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorNullAnnotationsThrows() {
        new MemoryContextImpl(null);
    }

    @Test
    public void testAnnotationsReturnsProvidedAnnotations() {
        MemoryContextAnnotations annotations = new Annotations();
        MemoryContextImpl context = new MemoryContextImpl(annotations);
        assertSame(annotations, context.annotations());
    }

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
    public void testClearResetsAllMemoryToZero() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());
        context.write(0, (byte) 0xFF);
        context.write(10, (byte) 0xAB);

        context.clear();

        assertEquals(0, (byte) context.read(0));
        assertEquals(0, (byte) context.read(10));
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

    @Test
    public void testWriteArrayWithCountIsSupported() {
        MemoryContextImpl mem = new MemoryContextImpl(new Annotations());

        Byte[] values = new Byte[]{10, 20, 30, 40};
        mem.write(4, values, 4);

        assertEquals((byte) 10, (byte) mem.read(4));
        assertEquals((byte) 20, (byte) mem.read(5));
        assertEquals((byte) 30, (byte) mem.read(6));
        assertEquals((byte) 40, (byte) mem.read(7));
    }

    @Test
    public void testWriteArrayWithCountPartialWrite() {
        MemoryContextImpl mem = new MemoryContextImpl(new Annotations());

        Byte[] values = new Byte[]{10, 20, 30, 40};
        mem.write(0, values, 2);

        assertEquals((byte) 10, (byte) mem.read(0));
        assertEquals((byte) 20, (byte) mem.read(1));
        assertEquals((byte) 0, (byte) mem.read(2));
        assertEquals((byte) 0, (byte) mem.read(3));
    }

    @Test
    public void testWriteArrayNotifiesObservers() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        MemoryContext.MemoryListener listener = createMock(MemoryContext.MemoryListener.class);
        listener.memoryContentChanged(eq(4), eq(7));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.write(4, new Byte[]{1, 2, 3, 4}, 4);

        verify(listener);
    }

    @Test
    public void testReadArrayClampsToBounds() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        // Reading beyond memory should return up to the memory boundary
        Byte[] result = context.read(MemoryContextImpl.NUMBER_OF_CELLS - 2, 10);
        assertEquals(2, result.length);
    }

    @Test
    public void testReadFirstAndLastCell() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());

        context.write(0, (byte) 0x11);
        context.write(MemoryContextImpl.NUMBER_OF_CELLS - 1, (byte) 0x22);

        assertEquals((byte) 0x11, (byte) context.read(0));
        assertEquals((byte) 0x22, (byte) context.read(MemoryContextImpl.NUMBER_OF_CELLS - 1));
    }

    @Test
    public void testNumberOfCellsIs128() {
        assertEquals(128, MemoryContextImpl.NUMBER_OF_CELLS);
    }
}
