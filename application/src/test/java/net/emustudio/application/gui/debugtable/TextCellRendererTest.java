/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.debugger.AddressColumn;
import org.junit.Test;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TextCellRendererTest {
    @Test
    public void selectionFocusAndPcMarkerRemainIndependentInLightAndDark() {
        for (Color background : new Color[] {Color.WHITE, new Color(60, 63, 65)}) {
            DebugTableModel model = mock(DebugTableModel.class);
            when(model.getColumnCount()).thenReturn(1);
            doReturn(new AddressColumn()).when(model).getColumnAt(0);
            when(model.getColumnName(0)).thenReturn("address");
            when(model.isRowAtCurrentInstruction(0)).thenReturn(true);
            JTable table = new JTable(new Object[][] {{"0000"}, {"0002"}}, new Object[] {"address"});
            table.setBackground(background);
            table.setForeground(background.equals(Color.WHITE) ? Color.BLACK : Color.WHITE);
            table.setSelectionBackground(new Color(38, 117, 191));
            table.setSelectionForeground(Color.WHITE);
            TextCellRenderer renderer = new TextCellRenderer(model);

            renderer.getTableCellRendererComponent(table, "0000", false, false, 0, 0);
            assertEquals(table.getBackground(), renderer.getBackground());
            assertTrue(paintedPixels(renderer.getIcon(), renderer) > 0);
            assertEquals("Current instruction (PC)", renderer.getToolTipText());

            Border focus = BorderFactory.createLineBorder(Color.ORANGE);
            Object previous = UIManager.get("Table.focusCellHighlightBorder");
            try {
                UIManager.put("Table.focusCellHighlightBorder", focus);
                renderer.getTableCellRendererComponent(table, "0000", true, true, 0, 0);
                assertEquals(table.getSelectionBackground(), renderer.getBackground());
                assertEquals(table.getSelectionForeground(), renderer.getForeground());
                assertSame(focus, renderer.getBorder());
                assertTrue(paintedPixels(renderer.getIcon(), renderer) > 0);

                renderer.getTableCellRendererComponent(table, "0002", true, false, 1, 0);
                assertEquals(table.getSelectionBackground(), renderer.getBackground());
                assertNotSame(focus, renderer.getBorder());
                assertEquals(0, paintedPixels(renderer.getIcon(), renderer));
                assertNull(renderer.getToolTipText());
            } finally { UIManager.put("Table.focusCellHighlightBorder", previous); }
        }
    }

    @Test
    public void pcMarkerStaysInAddressCellWithWrappedAndReorderedColumns() {
        DebugTableModel model = mock(DebugTableModel.class);
        when(model.getColumnCount()).thenReturn(1);
        doReturn(new AddressColumn()).when(model).getColumnAt(0);
        when(model.getColumnName(0)).thenReturn("address");
        when(model.isRowAtCurrentInstruction(0)).thenReturn(true);
        JTable table = new JTable(new Object[][] {{"", "0000", "nop"}}, new Object[] {"bp", "address", "instruction"});
        table.moveColumn(1, 2);
        TextCellRenderer renderer = new TextCellRenderer(model);
        renderer.getTableCellRendererComponent(table, "nop", false, false, 0, 1);
        assertNull(renderer.getIcon());
        renderer.getTableCellRendererComponent(table, "0000", false, false, 0, 2);
        assertTrue(paintedPixels(renderer.getIcon(), renderer) > 0);
    }

    @Test
    public void machineDataUsesMonospacedFontWithTableSizeAndStyle() {
        DebugTableModel model = mock(DebugTableModel.class);
        when(model.isRowAtCurrentInstruction(0)).thenReturn(true);
        JTable table = new JTable(new Object[][] {{"nop"}, {"hlt"}}, new Object[] {"instruction"});
        TextCellRenderer renderer = new TextCellRenderer(model);
        for (String family : new String[] {Font.DIALOG, Font.SANS_SERIF}) {
            table.setFont(new Font(family, Font.ITALIC, 16).deriveFont(16.5f));
            renderer.getTableCellRendererComponent(table, "nop", false, false, 0, 0);
            assertEquals(Font.MONOSPACED, renderer.getFont().getFamily());
            assertEquals(16.5f, renderer.getFont().getSize2D(), 0);
            assertEquals(Font.BOLD | Font.ITALIC, renderer.getFont().getStyle());
            renderer.getTableCellRendererComponent(table, "hlt", false, false, 1, 0);
            assertEquals(Font.MONOSPACED, renderer.getFont().getFamily());
            assertEquals(16.5f, renderer.getFont().getSize2D(), 0);
            assertEquals(Font.ITALIC, renderer.getFont().getStyle());
        }
    }

    private static int paintedPixels(Icon icon, Component component) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.createGraphics();
        icon.paintIcon(component, graphics, 0, 0);
        graphics.dispose();
        int painted = 0;
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                if ((image.getRGB(x, y) >>> 24) != 0) painted++;
        return painted;
    }
}
