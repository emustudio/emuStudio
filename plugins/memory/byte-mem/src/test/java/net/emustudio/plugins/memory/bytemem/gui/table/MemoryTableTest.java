/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.emulib.runtime.DebuggerMemory;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.Color;

import static org.junit.Assert.*;

public class MemoryTableTest {
    private MemoryTableModel tableModel;
    private final DebuggerMemory metadata = new DebuggerMemory() {
        public String label(int address) { return address == 1 ? "<start>" : ""; }
        public String comment(int address) { return address == 2 ? "Comment & details" : ""; }
        public boolean isCurrentInstruction(int address) { return address == 3; }
        public void addListener(Runnable listener) { }
        public void removeListener(Runnable listener) { }
    };

    @Before
    public void setUp() {
        tableModel = new MemoryTableModel(TestMemoryContextFactory.create(65536, 2, 4));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new MemoryTable(null, new JScrollPane(), metadata);
    }

    @Test
    public void testGetTableModel() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertSame(tableModel, table.getTableModel());
    }

    @Test
    public void testTableHasCorrectColumnCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertEquals(16, table.getColumnCount());
    }

    @Test
    public void testTableHasCorrectRowCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertEquals(16, table.getRowCount());
    }

    @Test
    public void testCellSelectionEnabled() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertTrue(table.getCellSelectionEnabled());
    }

    @Test
    public void testSelectionModeIsSingle() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
    }

    @Test
    public void testTableIsOpaque() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        assertTrue(table.isOpaque());
    }

    @Test
    public void metadataColorsAndTooltipsFollowEachAddress() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata);
        tableModel.setCurrentBank(1);
        JLabel initial = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);
        assertFalse(initial.getToolTipText().contains("Label:"));
        tableModel.setCurrentBank(0);

        JLabel label = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);
        assertEquals(new Color(230, 230, 230), label.getBackground());
        assertTrue(label.getToolTipText().contains("&lt;start&gt;"));
        JLabel annotation = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 2), 0, 2);
        assertEquals(new Color(200, 200, 200), annotation.getBackground());
        assertTrue(annotation.getToolTipText().contains("Comment &amp; details"));
        JLabel instruction = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3);
        assertEquals(new Color(255, 204, 204), instruction.getBackground());
        assertTrue(instruction.getToolTipText().contains("Current instruction"));
        JLabel empty = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4);
        assertNull(empty.getToolTipText());
    }

    @Test
    public void unavailableMetadataLeavesMemoryUsable() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), null);

        JLabel cell = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);

        assertEquals("00", cell.getText());
    }
}
