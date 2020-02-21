/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.memory.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static net.sf.emustudio.ssem.memory.gui.Constants.*;

class CellRenderer extends JLabel implements TableCellRenderer {
    private final JList rowHeader;
    private final String[] rowNames;
    private final RowHeaderRenderer rowHeaderRenderer;

    private Color selectionForeground;
    private Color selectionBackground;

    CellRenderer(final MemoryTableModel model) {
        this.rowHeaderRenderer = new RowHeaderRenderer();

        rowNames = new String[model.getColumnCount()];
        for (int i = 0; i < rowNames.length; i++) {
            rowNames[i] = String.format("%02X", i);
        }
        rowHeader = new JList(rowNames);
        rowHeader.setCellRenderer(rowHeaderRenderer);

        super.setOpaque(true);
        super.setFont(DEFAULT_FONT);
        super.setHorizontalAlignment(CENTER);
    }

    public void setup(JTable table) {
        rowHeader.setFixedCellHeight(table.getRowHeight() + CHAR_HEIGHT);
        rowHeaderRenderer.setup(table);

        selectionBackground = table.getSelectionBackground();
        selectionForeground = table.getSelectionForeground();
    }

    public JList getRowHeader() {
        return rowHeader;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setBackground(selectionBackground);
            setForeground(selectionForeground);
        } else {
            Color back = ((row % 2) == 0) ? COLOR_CELL_BACK : COLOR_CELL_BACK_MOD2;
            Color front = COLOR_FORE_UNIMPORTANT;

            if (MemoryTableModel.isBitInstruction(column) || MemoryTableModel.isBitLine(column)) {
                setFont(BOLD_FONT);
                front = COLOR_FORE;
            } else {
                setFont(DEFAULT_FONT);
            }

            setBackground(back);
            setForeground(front);
        }
        setText(value.toString());
        return this;
    }

}
