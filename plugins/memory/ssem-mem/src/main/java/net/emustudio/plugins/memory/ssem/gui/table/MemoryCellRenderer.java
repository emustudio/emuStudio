/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.FONT_MONOSPACED;
import static net.emustudio.emulib.runtime.interaction.GuiConstants.FONT_MONOSPACED_BOLD;

class MemoryCellRenderer extends JLabel implements TableCellRenderer {
    private final static Color COLOR_FORE = Color.BLACK;
    private final static Color COLOR_BACK_LINE = new Color(0xF3, 0xE3, 0xEC);
    private final static Color COLOR_FORE_UNIMPORTANT = Color.DARK_GRAY;
    private final static Color COLOR_CELL_BACK = Color.WHITE;
    private final static Color COLOR_CELL_BACK_MOD2 = new Color((int) (0xFF * 0.8), (int) (0xFF * 0.8), (int) (0xFF * 0.8));

    private final Color selectedBackground;
    private final Color selectedForeground;

    MemoryCellRenderer(JTableHeader header, MemoryTableModel tableModel, JScrollPane paneMemory, int rowHeight) {
        setOpaque(true);
        setDoubleBuffered(true);
        setBorder(BorderFactory.createEmptyBorder());
        setFont(FONT_MONOSPACED);
        setHorizontalAlignment(CENTER);

        this.selectedBackground = UIManager.getColor("Table.selectionBackground");
        this.selectedForeground = UIManager.getColor("Table.selectionForeground");

        String[] rowNames = new String[tableModel.getColumnCount()];
        for (int i = 0; i < rowNames.length; i++) {
            rowNames[i] = String.format("%02X / %02d", i, i);
        }

        JList<String> rowHeader = new JList<>(rowNames);

        FontMetrics fm = rowHeader.getFontMetrics(rowHeader.getFont());
        int char_width = 17;
        if (fm != null) {
            char_width = fm.stringWidth("FF");
        }

        rowHeader.setFixedCellWidth(char_width * 4);
        rowHeader.setFixedCellHeight(rowHeight);
        rowHeader.setCellRenderer(new MemoryRowHeaderRenderer(header));

        paneMemory.setRowHeaderView(rowHeader);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setBackground(selectedBackground);
            setForeground(selectedForeground);
        } else {
            Color back = ((row % 2) == 0) ? COLOR_CELL_BACK : COLOR_CELL_BACK_MOD2;
            Color front = COLOR_FORE_UNIMPORTANT;

            if (MemoryTableModel.isBitInstruction(column) || MemoryTableModel.isBitLine(column)) {
                setFont(FONT_MONOSPACED_BOLD);
                front = COLOR_FORE;
            } else {
                setFont(FONT_MONOSPACED);
            }

            if (MemoryTableModel.isBitLine(column)) {
                setBackground(COLOR_BACK_LINE);
            } else {
                setBackground(back);
            }
            setForeground(front);
        }
        setText(value.toString());
        return this;
    }
}
