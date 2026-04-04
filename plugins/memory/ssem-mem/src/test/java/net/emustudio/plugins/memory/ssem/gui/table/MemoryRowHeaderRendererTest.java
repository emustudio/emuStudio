/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;

public class MemoryRowHeaderRendererTest {

    @Test
    public void testRendererIsOpaque() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());
        assertTrue(renderer.isOpaque());
    }

    @Test
    public void testRendererIsDoubleBuffered() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());
        assertTrue(renderer.isDoubleBuffered());
    }

    @Test
    public void testRendererHorizontalAlignment() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());
        assertEquals(JLabel.CENTER, renderer.getHorizontalAlignment());
    }

    @Test
    public void testGetListCellRendererComponentWithValue() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());

        JList<String> list = new JList<>();
        Component comp = renderer.getListCellRendererComponent(list, "00 / 00", 0, false, false);
        assertNotNull(comp);
        assertSame(renderer, comp);
        assertEquals("00 / 00", renderer.getText());
    }

    @Test
    public void testGetListCellRendererComponentWithNullValue() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());

        JList<String> list = new JList<>();
        Component comp = renderer.getListCellRendererComponent(list, null, 0, false, false);
        assertNotNull(comp);
        assertEquals("", renderer.getText());
    }

    @Test
    public void testPreferredSizeWidth() {
        JTable table = new JTable();
        MemoryRowHeaderRenderer renderer = new MemoryRowHeaderRenderer(table.getTableHeader());
        Dimension prefSize = renderer.getPreferredSize();
        assertEquals(4 * net.emustudio.plugins.memory.ssem.gui.Constants.CHAR_WIDTH, prefSize.width);
    }
}

