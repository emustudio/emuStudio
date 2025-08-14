/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.emulib.runtime.helpers.RadixUtils;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

import net.emustudio.emulib.runtime.interaction.GuiConstants;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.BANK_COLOR;
import static net.emustudio.plugins.memory.bytemem.gui.Constants.ROM_COLOR;

class MemoryCellRenderer extends JLabel implements TableCellRenderer {
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
        setFont(GuiConstants.FONT_MONOSPACED);
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
                this.setBackground(BANK_COLOR);
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
