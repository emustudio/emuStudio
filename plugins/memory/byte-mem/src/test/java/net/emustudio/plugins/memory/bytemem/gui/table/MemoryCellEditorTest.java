/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import java.awt.*;
import static org.junit.Assert.*;
public class MemoryCellEditorTest {
    private MemoryContextImpl context;
    private MemoryTableModel tableModel;
    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(256, 1, 0);
        tableModel = new MemoryTableModel(context);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new MemoryCellEditor(null);
    }
    @Test
    public void testGetTableCellEditorComponentNotSelected() {
        MemoryCellEditor editor = new MemoryCellEditor(tableModel);
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "00", false, 0, 0);
        assertNull(comp);
    }
    @Test
    public void testGetTableCellEditorComponentSelected() {
        MemoryCellEditor editor = new MemoryCellEditor(tableModel);
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "00", true, 0, 0);
        assertNotNull(comp);
        assertTrue(comp instanceof JTextField);
    }
    @Test
    public void testGetTableCellEditorComponentDisplaysCurrentValue() {
        context.write(0, (byte) 0xAB);
        MemoryCellEditor editor = new MemoryCellEditor(tableModel);
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "AB", true, 0, 0);
        assertTrue(comp instanceof JTextField);
        JTextField textField = (JTextField) comp;
        assertEquals("0xAB", textField.getText());
    }
    @Test
    public void testGetCellEditorValueReturnsTextFieldContent() {
        MemoryCellEditor editor = new MemoryCellEditor(tableModel);
        JTable table = new JTable(tableModel);
        editor.getTableCellEditorComponent(table, "00", true, 0, 0);
        assertEquals("0x00", editor.getCellEditorValue());
    }
    @Test
    public void testGetCellEditorValueDifferentAddress() {
        context.write(3, (byte) 0xFF);
        MemoryCellEditor editor = new MemoryCellEditor(tableModel);
        JTable table = new JTable(tableModel);
        editor.getTableCellEditorComponent(table, "FF", true, 0, 3);
        assertEquals("0xFF", editor.getCellEditorValue());
    }
}
