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
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.emulib.runtime.helpers.RadixUtils;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.MEMORY_CELLS_FONT;

class MemoryCellRenderer extends JLabel implements TableCellRenderer {
    private final static Color ROM_COLOR = new Color(0xE8, 0x68, 0x50);
    private final Color selectedBackground;
    private final Color selectedForeground;

    private final MemoryTableModel tableModel;
    private final JList<String> rowHeader;
    private final String[] addresses;
    private int currentPage;

    MemoryCellRenderer(JTableHeader header, MemoryTableModel tableModel, JScrollPane paneMemory, int rowHeight) {
        this.tableModel = Objects.requireNonNull(tableModel);

        setOpaque(true);
        setDoubleBuffered(true);
        setBorder(BorderFactory.createEmptyBorder());
        setFont(MEMORY_CELLS_FONT);
        setHorizontalAlignment(CENTER);

        this.selectedBackground = UIManager.getColor("Table.selectionBackground");
        this.selectedForeground = UIManager.getColor("Table.selectionForeground");

        currentPage = tableModel.getPage();
        addresses = new String[tableModel.getRowCount()];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = RadixUtils.formatWordHexString(tableModel.getColumnCount() * i
                    + tableModel.getColumnCount() * tableModel.getRowCount() * currentPage);
        }
        rowHeader = new JList<>(addresses);

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
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            this.setBackground(selectedBackground);
            this.setForeground(selectedForeground);
        } else {
            if (tableModel.isROMAt(row, column)) {
                this.setBackground(ROM_COLOR);
            } else if (tableModel.isAtBANK(row, column)) {
                this.setBackground(Color.decode("0xFFE6BF"));
            } else {
                this.setBackground(Color.WHITE);
            }
            this.setForeground(Color.BLACK);
        }
        remakeAddresses();
        setText(value.toString());
        return this;
    }

    private void remakeAddresses() {
        if (currentPage == tableModel.getPage()) {
            return;
        }
        currentPage = tableModel.getPage();
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = String.format("%1$04Xh",
                    tableModel.getColumnCount() * i + tableModel.getColumnCount()
                            * tableModel.getRowCount() * currentPage);
        }
        rowHeader.setListData(addresses);
    }
}
