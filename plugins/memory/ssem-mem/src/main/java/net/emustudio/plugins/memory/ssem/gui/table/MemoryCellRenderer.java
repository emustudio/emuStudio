/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem.gui.table;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static net.emustudio.plugins.memory.ssem.gui.Constants.*;

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
        setFont(DEFAULT_FONT);
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
                setFont(BOLD_FONT);
                front = COLOR_FORE;
            } else {
                setFont(DEFAULT_FONT);
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
