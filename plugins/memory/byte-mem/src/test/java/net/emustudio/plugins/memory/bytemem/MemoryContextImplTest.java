/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.easymock.EasyMock.*;
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
        context.destroy();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullAnnotations() {
        new MemoryContextImpl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitZeroBanksThrows() {
        context.init(256, 0, 128);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitNegativeBanksThrows() {
        context.init(256, -1, 128);
    }

    @Test
    public void testInitSetsMemorySize() {
        assertEquals(256, context.getSize());
    }

    @Test
    public void testInitSetsBanksCount() {
        assertEquals(2, context.getBanksCount());
    }

    @Test
    public void testDefaultBankSelect() {
        assertEquals(0, context.getSelectedBank());
    }

    @Test
    public void testSelectBank() {
        context.selectBank(1);
        assertEquals(1, context.getSelectedBank());
    }

    @Test
    public void testSelectBankOutOfRangeIsIgnored() {
        context.selectBank(5);
        assertEquals(0, context.getSelectedBank());
    }

    @Test
    public void testGetCommonBoundary() {
        assertEquals(128, context.getCommonBoundary());
    }

    @Test
    public void testWriteAndRead() {
        context.write(10, (byte) 42);
        assertEquals(Byte.valueOf((byte) 42), context.read(10));
    }

    @Test
    public void testWriteAndReadInDifferentBanks() {
        context.selectBank(0);
        context.write(10, (byte) 1);

        context.selectBank(1);
        context.write(10, (byte) 2);

        context.selectBank(0);
        assertEquals(Byte.valueOf((byte) 1), context.read(10));

        context.selectBank(1);
        assertEquals(Byte.valueOf((byte) 2), context.read(10));
    }

    @Test
    public void testWriteAboveCommonBoundaryIsSharedAcrossBanks() {
        // Address 200 is above commonBoundary=128, so it should always use bank 0
        context.selectBank(0);
        context.write(200, (byte) 99);

        context.selectBank(1);
        assertEquals(Byte.valueOf((byte) 99), context.read(200));
    }

    @Test
    public void testReadBankBelowCommonBoundary() {
        context.selectBank(0);
        context.write(10, (byte) 11);
        context.selectBank(1);
        context.write(10, (byte) 22);

        assertEquals(Byte.valueOf((byte) 11), context.readBank(10, 0));
        assertEquals(Byte.valueOf((byte) 22), context.readBank(10, 1));
    }

    @Test
    public void testReadBankAboveCommonBoundaryAlwaysReturnsBank0() {
        context.selectBank(0);
        context.write(200, (byte) 55);

        assertEquals(Byte.valueOf((byte) 55), context.readBank(200, 1));
    }

    @Test
    public void testWriteBankBelowCommonBoundary() {
        context.writeBank(10, (byte) 33, 1);
        assertEquals(Byte.valueOf((byte) 33), context.readBank(10, 1));
        assertEquals(Byte.valueOf((byte) 0), context.readBank(10, 0));
    }

    @Test
    public void testWriteBankAboveCommonBoundaryWritesToBank0() {
        context.writeBank(200, (byte) 44, 1);
        assertEquals(Byte.valueOf((byte) 44), context.readBank(200, 0));
    }

    @Test
    public void testReadArray() {
        context.write(0, (byte) 1);
        context.write(1, (byte) 2);
        context.write(2, (byte) 3);

        Byte[] result = context.read(0, 3);
        assertArrayEquals(new Byte[]{1, 2, 3}, result);
    }

    @Test
    public void testReadArrayClampedToMemorySize() {
        Byte[] result = context.read(250, 100);
        assertEquals(6, result.length); // 256 - 250 = 6
    }

    @Test
    public void testWriteArrayOfValues() {
        Byte[] values = {10, 20, 30};
        context.write(0, values, 3);

        assertEquals(Byte.valueOf((byte) 10), context.read(0));
        assertEquals(Byte.valueOf((byte) 20), context.read(1));
        assertEquals(Byte.valueOf((byte) 30), context.read(2));
    }

    @Test
    public void testClear() {
        context.write(10, (byte) 42);
        context.clear();
        assertEquals(Byte.valueOf((byte) 0), context.read(10));
    }

    @Test
    public void testClearAllBanks() {
        context.selectBank(0);
        context.write(10, (byte) 1);
        context.selectBank(1);
        context.write(10, (byte) 2);

        context.clear();

        context.selectBank(0);
        assertEquals(Byte.valueOf((byte) 0), context.read(10));
        context.selectBank(1);
        assertEquals(Byte.valueOf((byte) 0), context.read(10));
    }

    @Test
    public void testSetReadOnlyPreventsWrite() {
        context.setReadOnly(new RangeTree.Range(10, 20));

        context.write(15, (byte) 42);
        assertEquals(Byte.valueOf((byte) 0), context.read(15));
    }

    @Test
    public void testSetReadOnlyDoesNotAffectOtherAddresses() {
        context.setReadOnly(new RangeTree.Range(10, 20));

        context.write(5, (byte) 42);
        assertEquals(Byte.valueOf((byte) 42), context.read(5));
    }

    @Test
    public void testSetReadWriteAllowsWriteAgain() {
        RangeTree.Range range = new RangeTree.Range(10, 20);
        context.setReadOnly(range);

        context.setReadWrite(range);

        context.write(15, (byte) 42);
        assertEquals(Byte.valueOf((byte) 42), context.read(15));
    }

    @Test
    public void testIsReadOnly() {
        context.setReadOnly(new RangeTree.Range(10, 20));

        assertTrue(context.isReadOnly(10));
        assertTrue(context.isReadOnly(15));
        assertTrue(context.isReadOnly(20));
        assertFalse(context.isReadOnly(9));
        assertFalse(context.isReadOnly(21));
    }

    @Test
    public void testGetReadOnly() {
        context.setReadOnly(new RangeTree.Range(10, 20));
        context.setReadOnly(new RangeTree.Range(50, 60));

        List<? extends ByteMemoryContext.AddressRange> ranges = context.getReadOnly();
        assertEquals(2, ranges.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetReadOnlyInvalidRangeThrows() {
        context.setReadOnly(new RangeTree.Range(20, 10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetReadWriteInvalidRangeThrows() {
        context.setReadWrite(new RangeTree.Range(20, 10));
    }

    @Test
    public void testWriteArrayToReadOnlyRangeIsIgnored() {
        context.setReadOnly(new RangeTree.Range(0, 10));

        Byte[] values = {10, 20, 30};
        context.write(0, values, 3);

        assertEquals(Byte.valueOf((byte) 0), context.read(0));
    }

    @Test
    public void testWriteBankToReadOnlyIsIgnored() {
        context.setReadOnly(new RangeTree.Range(10, 20));

        context.writeBank(15, (byte) 42, 0);
        assertEquals(Byte.valueOf((byte) 0), context.readBank(15, 0));
    }

    @Test
    public void testGetCellTypeClass() {
        assertEquals(Byte.class, context.getCellTypeClass());
    }

    @Test
    public void testAnnotations() {
        assertSame(annotations, context.annotations());
    }

    @Test
    public void testGetRawMemory() {
        Byte[][] raw = context.getRawMemory();
        assertNotNull(raw);
        assertEquals(2, raw.length);
        assertEquals(256, raw[0].length);
    }

    @Test
    public void testDestroyNullsMemory() {
        context.destroy();
        assertEquals(0, context.getBanksCount());
        // After destroy, reinit for tearDown
        context.init(256, 2, 128);
    }

    @Test
    public void testSingleBankNoCommon() {
        context.init(100, 1, 0);
        context.write(50, (byte) 77);
        assertEquals(Byte.valueOf((byte) 77), context.read(50));
    }
}

