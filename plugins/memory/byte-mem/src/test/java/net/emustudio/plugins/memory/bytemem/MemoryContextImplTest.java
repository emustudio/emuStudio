/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class MemoryContextImplTest {
    private MemoryContextImpl context;
    private MemoryContextAnnotations annotations;

    @Before
    public void setUp() {
        annotations = createNiceMock(MemoryContextAnnotations.class);
        replay(annotations);
        context = new MemoryContextImpl(annotations);
        context.init(256, 2, 128);
    }

    @After
    public void tearDown() {
        if (context != null && context.getRawMemory() != null) context.destroy();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }

    private void expect(Class<? extends Throwable> type, ThrowingRunnable run) {
        try {
            run.run();
            fail("Expected " + type.getSimpleName());
        } catch (Throwable e) {
            assertTrue(type.isInstance(e));
        }
    }

    @Test
    public void validatesConstructionAndInitialization() {
        expect(NullPointerException.class, () -> new MemoryContextImpl(null));
        expect(IllegalArgumentException.class, () -> context.init(256, 0, 128));
        expect(IllegalArgumentException.class, () -> context.init(256, -1, 128));
        assertEquals(256, context.getSize());
        assertEquals(2, context.getBanksCount());
        assertEquals(0, context.getSelectedBank());
        assertEquals(128, context.getCommonBoundary());
        context.selectBank(1);
        assertEquals(1, context.getSelectedBank());
        context.selectBank(5);
        assertEquals(1, context.getSelectedBank());
        context.init(100, 1, 0);
        context.write(50, (byte) 77);
        assertEquals(Byte.valueOf((byte) 77), context.read(50));
    }

    @Test
    public void readsAndWritesRespectBankingAndArrays() {
        context.write(10, (byte) 1);
        context.selectBank(1);
        context.write(10, (byte) 2);
        context.write(200, (byte) 99);
        context.writeBank(11, (byte) 33, 0);
        context.writeBank(12, (byte) 44, 1);
        context.write(0, new Byte[]{10, 20, 30}, 3);
        assertEquals(Byte.valueOf((byte) 1), context.readBank(10, 0));
        assertEquals(Byte.valueOf((byte) 2), context.readBank(10, 1));
        assertEquals(Byte.valueOf((byte) 99), context.readBank(200, 1));
        assertEquals(Byte.valueOf((byte) 33), context.readBank(11, 0));
        assertEquals(Byte.valueOf((byte) 44), context.readBank(12, 1));
        assertEquals(Byte.valueOf((byte) 99), context.read(200));
        assertArrayEquals(new Byte[]{10, 20, 30}, context.read(0, 3));
        assertEquals(6, context.read(250, 100).length);
        context.clear();
        assertEquals(Byte.valueOf((byte) 0), context.readBank(10, 0));
        assertEquals(Byte.valueOf((byte) 0), context.readBank(10, 1));
    }

    @Test
    public void readOnlyRangesBlockWritesAndCanBeRemoved() {
        RangeTree.Range range = new RangeTree.Range(10, 20);
        context.setReadOnly(range);
        context.setReadOnly(new RangeTree.Range(50, 60));
        assertTrue(context.isReadOnly(10));
        assertTrue(context.isReadOnly(15));
        assertTrue(context.isReadOnly(20));
        assertFalse(context.isReadOnly(9));
        assertFalse(context.isReadOnly(21));
        assertEquals(2, context.getReadOnly().size());
        context.write(15, (byte) 42);
        context.write(5, (byte) 42);
        context.write(0, new Byte[]{10, 20, 30}, 3);
        context.writeBank(15, (byte) 42, 0);
        assertEquals(Byte.valueOf((byte) 0), context.read(15));
        assertEquals(Byte.valueOf((byte) 42), context.read(5));
        assertEquals(Byte.valueOf((byte) 10), context.read(0));
        context.setReadOnly(new RangeTree.Range(0, 10));
        context.write(0, new Byte[]{1, 2, 3}, 3);
        assertEquals(Byte.valueOf((byte) 10), context.read(0));
        context.setReadWrite(range);
        context.write(15, (byte) 42);
        assertEquals(Byte.valueOf((byte) 42), context.read(15));
        expect(IllegalArgumentException.class, () -> context.setReadOnly(new RangeTree.Range(20, 10)));
        expect(IllegalArgumentException.class, () -> context.setReadWrite(new RangeTree.Range(20, 10)));
    }

    @Test
    public void exposesMetadataRawMemoryAndDestroy() {
        assertEquals(Byte.class, context.getCellTypeClass());
        assertSame(annotations, context.annotations());
        Byte[][] raw = context.getRawMemory();
        assertNotNull(raw);
        assertEquals(2, raw.length);
        assertEquals(256, raw[0].length);
        context.destroy();
        assertEquals(0, context.getBanksCount());
        context.init(256, 2, 128);
    }
}
