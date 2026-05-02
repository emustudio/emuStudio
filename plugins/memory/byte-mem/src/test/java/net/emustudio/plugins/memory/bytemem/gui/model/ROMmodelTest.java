/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.RangeTree;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ROMmodelTest {
    private MemoryContextImpl context;

    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(65536, 1, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemoryThrows() {
        new ROMmodel(null);
    }

    @Test
    public void testRowCountNoROM() {
        ROMmodel model = new ROMmodel(context);
        assertEquals(0, model.getRowCount());
    }

    @Test
    public void testRowCountOneRange() {
        context.setReadOnly(new RangeTree.Range(100, 200));
        ROMmodel model = new ROMmodel(context);
        assertEquals(1, model.getRowCount());
    }

    @Test
    public void testRowCountMultipleRanges() {
        context.setReadOnly(new RangeTree.Range(100, 200));
        context.setReadOnly(new RangeTree.Range(500, 600));
        ROMmodel model = new ROMmodel(context);
        assertEquals(2, model.getRowCount());
    }

    @Test
    public void testColumnCount() {
        ROMmodel model = new ROMmodel(context);
        assertEquals(2, model.getColumnCount());
    }

    @Test
    public void testColumnNames() {
        ROMmodel model = new ROMmodel(context);
        assertEquals("From (hex)", model.getColumnName(0));
        assertEquals("To (hex)", model.getColumnName(1));
    }

    @Test
    public void testColumnClass() {
        ROMmodel model = new ROMmodel(context);
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
    }

    @Test
    public void testIsCellEditable() {
        ROMmodel model = new ROMmodel(context);
        assertFalse(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(0, 1));
    }

    @Test
    public void testGetValueAtFromColumn() {
        context.setReadOnly(new RangeTree.Range(0x100, 0x200));
        ROMmodel model = new ROMmodel(context);
        assertEquals("0x0100", model.getValueAt(0, 0));
    }

    @Test
    public void testGetValueAtToColumn() {
        context.setReadOnly(new RangeTree.Range(0x100, 0x200));
        ROMmodel model = new ROMmodel(context);
        assertEquals("0x0200", model.getValueAt(0, 1));
    }

    @Test
    public void testMultipleRangesValues() {
        context.setReadOnly(new RangeTree.Range(0x0010, 0x0020));
        context.setReadOnly(new RangeTree.Range(0xFF00, 0xFFFF));
        ROMmodel model = new ROMmodel(context);
        assertEquals("0x0010", model.getValueAt(0, 0));
        assertEquals("0x0020", model.getValueAt(0, 1));
        assertEquals("0xFF00", model.getValueAt(1, 0));
        assertEquals("0xFFFF", model.getValueAt(1, 1));
    }
}
