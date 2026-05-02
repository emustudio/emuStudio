/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class MemoryTableTest {
    private MemoryTableModel tableModel;

    @Before
    public void setUp() {
        tableModel = new MemoryTableModel(TestMemoryContextFactory.create(65536, 1, 0));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new MemoryTable(null, new JScrollPane());
    }

    @Test
    public void testGetTableModel() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertSame(tableModel, table.getTableModel());
    }

    @Test
    public void testTableHasCorrectColumnCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertEquals(16, table.getColumnCount());
    }

    @Test
    public void testTableHasCorrectRowCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertEquals(16, table.getRowCount());
    }

    @Test
    public void testCellSelectionEnabled() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertTrue(table.getCellSelectionEnabled());
    }

    @Test
    public void testSelectionModeIsSingle() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
    }

    @Test
    public void testTableIsOpaque() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertTrue(table.isOpaque());
    }
}
