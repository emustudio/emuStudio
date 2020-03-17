/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MemoryContextImplTest {

    @Test
    public void testAfterClearObserversAreNotified() {
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
    public void testReadWithoutWritReturnsZero() {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(0L, (long) context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadAtInvalidLocationThrows() {
        MemoryContextImpl context = new MemoryContextImpl();

        context.read(-1);
    }

    @Test
    public void testAfterReadNoObserversAreNotified() {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        replay(listener);

        context.addMemoryListener(listener);
        context.read(10);

        verify(listener);
    }

    @Test
    public void testAfterWriteObserversAreNotified() {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        listener.memoryChanged(eq(10));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.write(10, (byte) 134);

        verify(listener);
    }

    @Test
    public void testWriteReallyWritesCorrectValueAtCorrectLocation() {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(10, (byte) 134);
        assertEquals((byte) 134, (byte) context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteAtInvalidLocationThrows() {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(-1, (byte) 134);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, context.getSize());
    }

    @Test
    public void testClassTypeIsByte() {
        assertEquals(Byte.class, new MemoryContextImpl().getDataType());
    }

    @Test
    public void testReadWordIsSupported() {
        assertArrayEquals(new Byte[]{0, 0, 0, 0}, new MemoryContextImpl().readWord(0));
    }

    @Test
    public void testWriteWordIsSupported() {
        MemoryContextImpl mem = new MemoryContextImpl();

        Byte[] row = new Byte[]{1, 2, 3, 4};
        mem.writeWord(0, row);

        assertArrayEquals(row, mem.readWord(0));
    }
}
