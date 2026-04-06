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

public class MemoryCellEditorTest {

    private MemoryTableModel tableModel;

    @Before
    public void setUp() {
        MemoryContextImpl context = new MemoryContextImpl(new Annotations());
        tableModel = new MemoryTableModel(context);
    }

    @Test
    public void testGetTableCellEditorComponentNotSelectedReturnsNull() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "00", false, 0, 0);
        assertNull(comp);
    }

    @Test
    public void testGetTableCellEditorComponentSelectedReturnsTextField() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "test", true, 0, 0);
        assertNotNull(comp);
        assertTrue(comp instanceof JTextField);
    }

    @Test
    public void testGetTableCellEditorComponentSetsText() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, "hello", true, 0, 0);
        assertTrue(comp instanceof JTextField);
        assertEquals("hello", ((JTextField) comp).getText());
    }

    @Test
    public void testGetCellEditorValueReturnsTextFieldContent() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        editor.getTableCellEditorComponent(table, "0xFF", true, 0, 0);
        assertEquals("0xFF", editor.getCellEditorValue());
    }

    @Test
    public void testGetCellEditorValueAfterMultipleCalls() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);

        editor.getTableCellEditorComponent(table, "first", true, 0, 0);
        assertEquals("first", editor.getCellEditorValue());

        editor.getTableCellEditorComponent(table, "second", true, 1, 0);
        assertEquals("second", editor.getCellEditorValue());
    }

    @Test
    public void testGetTableCellEditorComponentWithNullValue() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, null, true, 0, 0);
        assertNotNull(comp);
        assertTrue(comp instanceof JTextField);
        assertEquals("null", ((JTextField) comp).getText());
    }

    @Test
    public void testGetTableCellEditorComponentWithIntegerValue() {
        MemoryCellEditor editor = new MemoryCellEditor();
        JTable table = new JTable(tableModel);
        Component comp = editor.getTableCellEditorComponent(table, 42, true, 0, 0);
        assertNotNull(comp);
        assertEquals("42", ((JTextField) comp).getText());
    }
}

