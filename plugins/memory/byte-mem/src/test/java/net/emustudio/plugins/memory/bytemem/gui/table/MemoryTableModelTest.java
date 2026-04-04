/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.RangeTree;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class MemoryTableModelTest {
    private MemoryContextImpl context;
    private MemoryTableModel model;

    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(65536, 2, 32768);
        model = new MemoryTableModel(context);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemoryThrows() {
        new MemoryTableModel(null);
    }

    @Test
    public void testRowCount() {
        assertEquals(16, model.getRowCount());
    }

    @Test
    public void testColumnCount() {
        assertEquals(16, model.getColumnCount());
    }

    @Test
    public void testColumnNames() {
        assertEquals("00", model.getColumnName(0));
        assertEquals("01", model.getColumnName(1));
        assertEquals("0A", model.getColumnName(10));
        assertEquals("0F", model.getColumnName(15));
    }

    @Test
    public void testGetValueAtDefaultIsHexZero() {
        assertEquals("00", model.getValueAt(0, 0));
    }

    @Test
    public void testGetValueAtReturnsHexByDefault() {
        context.write(0, (byte) 0xAB);
        assertEquals("AB", model.getValueAt(0, 0));
    }

    @Test
    public void testGetValueAtAsciiMode() {
        context.write(0, (byte) 65); // 'A'
        model.setAsciiMode(true);
        assertEquals('A', model.getValueAt(0, 0));
    }

    @Test
    public void testSetAsciiModeSwitchesBack() {
        context.write(0, (byte) 65);
        model.setAsciiMode(true);
        assertEquals('A', model.getValueAt(0, 0));

        model.setAsciiMode(false);
        assertEquals("41", model.getValueAt(0, 0));
    }

    @Test
    public void testGetRawValueAt() {
        context.write(5, (byte) 0xFF);
        assertEquals(0xFF, model.getRawValueAt(0, 5));
    }

    @Test
    public void testGetRawValueAtBeyondMemoryReturnsZero() {
        MemoryContextImpl smallContext = TestMemoryContextFactory.create(16, 1, 0);
        MemoryTableModel smallModel = new MemoryTableModel(smallContext);
        // Row 1, column 0 = address 16, which is at or beyond size=16
        assertEquals(0, smallModel.getRawValueAt(1, 0));
    }

    @Test
    public void testSetValueAt() {
        model.setValueAt("0xFF", 0, 0);
        assertEquals(0xFF, model.getRawValueAt(0, 0));
    }

    @Test
    public void testSetValueAtDecimal() {
        model.setValueAt("42", 0, 0);
        assertEquals(42, model.getRawValueAt(0, 0));
    }

    @Test
    public void testSetValueAtInvalidNumberDoesNotThrow() {
        model.setValueAt("not_a_number", 0, 0);
        assertEquals(0, model.getRawValueAt(0, 0));
    }

    @Test
    public void testIsCellEditable() {
        assertTrue(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(15, 15));
    }

    @Test
    public void testGetPageDefaultIsZero() {
        assertEquals(0, model.getPage());
    }

    @Test
    public void testSetPage() {
        model.setPage(1);
        assertEquals(1, model.getPage());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetPageNegativeThrows() {
        model.setPage(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetPageBeyondCountThrows() {
        model.setPage(model.getPageCount());
    }

    @Test
    public void testGetPageCount() {
        // 65536 / (16 * 16) = 256
        assertEquals(256, model.getPageCount());
    }

    @Test
    public void testSetPageAndReadCorrectAddress() {
        // Page 1 starts at address 256
        context.write(256, (byte) 0xAB);
        model.setPage(1);
        assertEquals(0xAB, model.getRawValueAt(0, 0));
    }

    @Test
    public void testGetCurrentBankDefaultIsZero() {
        assertEquals(0, model.getCurrentBank());
    }

    @Test
    public void testSetCurrentBank() {
        model.setCurrentBank(1);
        assertEquals(1, model.getCurrentBank());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetCurrentBankNegativeThrows() {
        model.setCurrentBank(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetCurrentBankBeyondCountThrows() {
        model.setCurrentBank(2); // only 2 banks (0, 1)
    }

    @Test
    public void testSetCurrentBankReadsFromCorrectBank() {
        // Write to bank 0
        context.writeBank(5, (byte) 11, 0);
        // Write to bank 1
        context.writeBank(5, (byte) 22, 1);

        model.setCurrentBank(0);
        assertEquals(11, model.getRawValueAt(0, 5));

        model.setCurrentBank(1);
        assertEquals(22, model.getRawValueAt(0, 5));
    }

    @Test
    public void testToAddress() {
        // Page 0, row 0, column 0 -> address 0
        assertEquals(0, model.toAddress(0, 0));
        // Page 0, row 0, column 5 -> address 5
        assertEquals(5, model.toAddress(0, 5));
        // Page 0, row 1, column 0 -> address 16
        assertEquals(16, model.toAddress(1, 0));
        // Page 0, row 2, column 3 -> 2*16 + 3 = 35
        assertEquals(35, model.toAddress(2, 3));
    }

    @Test
    public void testToAddressWithPage() {
        model.setPage(1);
        // Page 1: base=256, row 0, column 0 -> 256
        assertEquals(256, model.toAddress(0, 0));
        // Page 1, row 1, column 5 -> 256 + 16 + 5 = 277
        assertEquals(277, model.toAddress(1, 5));
    }

    @Test
    public void testIsROMAt() {
        context.setReadOnly(new RangeTree.Range(0, 15));
        assertTrue(model.isROMAt(0, 0));
        assertTrue(model.isROMAt(0, 15));
        assertFalse(model.isROMAt(1, 0)); // address 16 is not ROM
    }

    @Test
    public void testIsAtBANK() {
        // commonBoundary is 32768
        assertTrue(model.isAtBANK(0, 0)); // address 0 < 32768
        // Page where address is >= 32768: page 128 (128 * 256 = 32768)
        model.setPage(128);
        assertFalse(model.isAtBANK(0, 0)); // address 32768 is not < 32768
    }

    @Test
    public void testFindSequenceFound() {
        context.write(10, (byte) 0xAA);
        context.write(11, (byte) 0xBB);
        context.write(12, (byte) 0xCC);

        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, 0);
        assertTrue(result.isPresent());
        assertEquals(10, result.get().intValue());
    }

    @Test
    public void testFindSequenceNotFound() {
        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0xDE, (byte) 0xAD}, 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindSequenceFromOffset() {
        context.write(5, (byte) 0xAA);
        context.write(10, (byte) 0xAA);

        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0xAA}, 6);
        assertTrue(result.isPresent());
        assertEquals(10, result.get().intValue());
    }

    @Test
    public void testFindSequenceSingleByte() {
        context.write(100, (byte) 0x42);

        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0x42}, 0);
        assertTrue(result.isPresent());
        assertEquals(100, result.get().intValue());
    }

    @Test
    public void testFindSequencePartialMatchThenFail() {
        context.write(0, (byte) 0xAA);
        context.write(1, (byte) 0xBB);
        // 0xCC is missing at position 2

        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindSequenceAtEndOfMemory() {
        int size = context.getSize();
        context.write(size - 2, (byte) 0xFE);
        context.write(size - 1, (byte) 0xFF);

        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0xFE, (byte) 0xFF}, 0);
        assertTrue(result.isPresent());
        assertEquals(size - 2, result.get().intValue());
    }

    @Test
    public void testFindSequenceEmptyFromHighOffset() {
        Optional<Integer> result = model.findSequence(new byte[]{(byte) 0x42}, 70000);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSetValueAtMasksTo8Bits() {
        model.setValueAt("0x1FF", 0, 0);
        assertEquals(0xFF, model.getRawValueAt(0, 0));
    }

    @Test
    public void testIsROMAtNotROM() {
        assertFalse(model.isROMAt(0, 0));
    }
}
