/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.plugins.memory.bytemem.gui.AnnotationLabels;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.plugins.memory.bytemem.gui.Constants.BANK_COLOR;
import static net.emustudio.plugins.memory.bytemem.gui.Constants.ROM_COLOR;

class MemoryCellRenderer extends JLabel implements TableCellRenderer {
    private final Color selectedBackground;
    private final Color selectedForeground;

    private final MemoryTableModel tableModel;
    private final MemoryAnnotations annotations;
    private final ExecutionState executionState;
    private final JList<String> rowHeader;
    private final String[] addresses;
    private int currentPage;

    MemoryCellRenderer(JTableHeader header, MemoryTableModel tableModel, JScrollPane paneMemory, int rowHeight,
                       MemoryAnnotations annotations, ExecutionState executionState) {
        this.tableModel = Objects.requireNonNull(tableModel);
        this.annotations = annotations;
        this.executionState = executionState;

        setOpaque(true);
        setDoubleBuffered(true);
        setBorder(BorderFactory.createEmptyBorder());
        setFont(FONT_MONOSPACED);
        setHorizontalAlignment(CENTER);

        this.selectedBackground = UIManager.getColor("Table.selectionBackground");
        this.selectedForeground = UIManager.getColor("Table.selectionForeground");

        currentPage = tableModel.getPage();
        addresses = new String[tableModel.getRowCount()];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = RadixUtils.formatHex(tableModel.getColumnCount() * i
                    + tableModel.getColumnCount() * tableModel.getRowCount() * currentPage, 4);
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
        int address = tableModel.toAddress(row, column);
        boolean mapped = tableModel.isMappedAddress(address);
        String label = mapped ? AnnotationLabels.at(annotations, address) : "";
        boolean current = mapped && executionState != null && executionState.snapshot().currentInstruction() == address;
        StringBuilder tooltip = new StringBuilder("<html>");
        if (current) tooltip.append("Current instruction<br>");
        if (!label.isEmpty()) tooltip.append("Label: ").append(escape(label)).append("<br>");
        if (tableModel.isROMAt(row, column)) tooltip.append("Read-only memory<br>");
        if (tableModel.isAtBANK(row, column)) tooltip.append("Banked memory<br>");
        setToolTipText(tooltip.length() == 6 ? null : tooltip.append("</html>").toString());
        if (isSelected) {
            this.setBackground(selectedBackground);
            this.setForeground(selectedForeground);
        } else {
            if (current) {
                this.setBackground(new Color(255, 204, 204));
            } else if (!label.isEmpty()) {
                this.setBackground(new Color(230, 230, 230));
            } else if (tableModel.isROMAt(row, column)) {
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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
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
