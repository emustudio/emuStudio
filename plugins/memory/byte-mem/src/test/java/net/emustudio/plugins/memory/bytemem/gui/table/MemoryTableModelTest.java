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
    public void constructorRejectsNullMemory() {
        new MemoryTableModel(null);
    }

    @Test
    public void tableShapeAndNamesAreFixed() {
        assertEquals(16, model.getRowCount());
        assertEquals(16, model.getColumnCount());
        assertEquals(256, model.getPageCount());
        assertEquals("00", model.getColumnName(0));
        assertEquals("0A", model.getColumnName(10));
        assertEquals("0F", model.getColumnName(15));
        assertTrue(model.isCellEditable(0, 0));
    }

    @Test
    public void valuesFormatAndWriteAsExpected() {
        assertEquals("00", model.getValueAt(0, 0));
        model.setValueAt("0xAB", 0, 0);
        assertEquals(0xAB, model.getRawValueAt(0, 0));
        model.setAsciiMode(true);
        assertEquals((char) 0xAB, model.getValueAt(0, 0));
        model.setAsciiMode(false);
        assertEquals("AB", model.getValueAt(0, 0));
        model.setValueAt("0x1FF", 0, 1);
        assertEquals(0xFF, model.getRawValueAt(0, 1));
        model.setValueAt("nope", 0, 2);
        assertEquals(0, model.getRawValueAt(0, 2));
    }

    @Test
    public void pagingAndBanksDriveAddressing() {
        assertEquals(0, model.getPage());
        assertEquals(0, model.getCurrentBank());
        model.setValueAt("11", 0, 5);
        model.setCurrentBank(1);
        model.setValueAt("22", 0, 5);
        assertEquals(1, model.getCurrentBank());
        assertEquals(22, model.getRawValueAt(0, 5));
        model.setCurrentBank(0);
        assertEquals(11, model.getRawValueAt(0, 5));
        model.setPage(1);
        model.setValueAt("0xAB", 0, 0);
        assertEquals(1, model.getPage());
        assertEquals(256, model.toAddress(0, 0));
        assertEquals(277, model.toAddress(1, 5));
        assertEquals(0xAB, model.getRawValueAt(0, 0));
        assertEquals(0, new MemoryTableModel(TestMemoryContextFactory.create(16, 1, 0)).getRawValueAt(1, 0));
    }

    @Test
    public void romBankAndSequenceQueriesUseCurrentState() {
        model.setValueAt("0xAA", 0, 10);
        model.setValueAt("0xBB", 0, 11);
        model.setValueAt("0xCC", 0, 12);
        model.setValueAt("0x42", 6, 4);
        context.write(context.getSize() - 2, (byte) 0xFE);
        context.write(context.getSize() - 1, (byte) 0xFF);
        assertEquals(Optional.of(10), model.findSequence(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, 0));
        assertEquals(Optional.of(100), model.findSequence(new byte[]{0x42}, 0));
        assertEquals(Optional.of(65534), model.findSequence(new byte[]{(byte) 0xFE, (byte) 0xFF}, 0));
        assertEquals(Optional.empty(), model.findSequence(new byte[]{(byte) 0xAA}, 70000));
        assertFalse(model.isROMAt(0, 0));
        assertTrue(model.isAtBANK(0, 0));
        context.setReadOnly(new RangeTree.Range(0, 15));
        assertTrue(model.isROMAt(0, 0));
        model.setPage(128);
        assertFalse(model.isAtBANK(0, 0));
    }

    @Test
    public void validationRejectsInvalidPagesAndBanks() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.setPage(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.setPage(model.getPageCount()));
        assertThrows(IndexOutOfBoundsException.class, () -> model.setCurrentBank(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.setCurrentBank(2));
    }
}
