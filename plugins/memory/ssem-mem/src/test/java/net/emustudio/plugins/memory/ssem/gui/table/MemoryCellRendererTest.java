/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;

public class MemoryCellRendererTest {

    private MemoryContextImpl context;
    private MemoryTableModel tableModel;
    private JScrollPane scrollPane;
    private MemoryCellRenderer renderer;
    private JTable table;

    @Before
    public void setUp() {
        context = new MemoryContextImpl(new Annotations());
        tableModel = new MemoryTableModel(context);
        scrollPane = new JScrollPane();
        table = new JTable(tableModel);
        renderer = new MemoryCellRenderer(table.getTableHeader(), tableModel, scrollPane, table.getRowHeight());
    }

    @Test
    public void testRendererIsOpaque() {
        assertTrue(renderer.isOpaque());
    }

    @Test
    public void testRendererIsDoubleBuffered() {
        assertTrue(renderer.isDoubleBuffered());
    }

    @Test
    public void testGetRendererComponentForSelectedCell() {
        Component comp = renderer.getTableCellRendererComponent(table, "1", true, false, 0, 0);
        assertNotNull(comp);
        assertSame(renderer, comp);
        assertEquals("1", renderer.getText());
    }

    @Test
    public void testGetRendererComponentForUnselectedEvenRow() {
        Component comp = renderer.getTableCellRendererComponent(table, "0", false, false, 0, 6);
        assertNotNull(comp);
        assertEquals("0", renderer.getText());
        // Even row should have white background (for non-line columns)
        assertEquals(Color.WHITE, renderer.getBackground());
    }

    @Test
    public void testGetRendererComponentForUnselectedOddRow() {
        Component comp = renderer.getTableCellRendererComponent(table, "0", false, false, 1, 6);
        assertNotNull(comp);
        // Odd row should have alternate background
        assertNotEquals(Color.WHITE, renderer.getBackground());
    }

    @Test
    public void testGetRendererComponentForLineColumn() {
        // Columns 0-4 are "line" columns
        Component comp = renderer.getTableCellRendererComponent(table, "1", false, false, 0, 2);
        assertNotNull(comp);
        // Line columns have a special pinkish background
        assertEquals(new Color(0xF3, 0xE3, 0xEC), renderer.getBackground());
        assertEquals(Color.BLACK, renderer.getForeground());
    }

    @Test
    public void testGetRendererComponentForInstructionColumn() {
        // Columns 13-15 are "instruction" columns
        Component comp = renderer.getTableCellRendererComponent(table, "1", false, false, 0, 14);
        assertNotNull(comp);
        assertEquals(Color.BLACK, renderer.getForeground());
    }

    @Test
    public void testGetRendererComponentForUnimportantColumn() {
        // Columns 5-12 and 16-31 are unimportant
        Component comp = renderer.getTableCellRendererComponent(table, "0", false, false, 0, 10);
        assertNotNull(comp);
        assertEquals(Color.DARK_GRAY, renderer.getForeground());
    }

    @Test
    public void testGetRendererComponentForHexColumn() {
        Component comp = renderer.getTableCellRendererComponent(table, "0xFF", false, false, 0, MemoryTableModel.COLUMN_HEX_VALUE);
        assertNotNull(comp);
        assertEquals("0xFF", renderer.getText());
    }

    @Test
    public void testScrollPaneHasRowHeader() {
        // The renderer constructor sets up the row header
        assertNotNull(scrollPane.getRowHeader());
    }
}

