/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class MemoryTableTest {

    private MemoryTableModel tableModel;

    @Before
    public void setUp() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());
        tableModel = new MemoryTableModel(context);
    }

    @Test
    public void testTableHasCorrectColumnCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertEquals(35, table.getColumnCount());
    }

    @Test
    public void testTableHasCorrectRowCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertEquals(32, table.getRowCount());
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

    @Test
    public void testTableIsFocusCycleRoot() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertTrue(table.isFocusCycleRoot());
    }

    @Test
    public void testDeleteKeyMappingExists() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        InputMap im = table.getInputMap(JTable.WHEN_FOCUSED);
        assertNotNull(im.get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0)));
    }

    @Test
    public void testDeleteActionMappingExists() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        ActionMap am = table.getActionMap();
        assertNotNull(am.get("delete"));
    }

    @Test
    public void testAllColumnsHaveCellEditor() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            assertNotNull(table.getColumnModel().getColumn(i).getCellEditor());
        }
    }

    @Test
    public void testTableModelIsSet() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane());
        assertSame(tableModel, table.getModel());
    }
}

