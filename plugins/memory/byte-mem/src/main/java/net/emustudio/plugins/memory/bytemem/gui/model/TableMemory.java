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
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.emulib.runtime.helpers.RadixUtils;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.MEMORY_CELLS_FONT;

public class TableMemory extends JTable {
    private final MemoryTableModel tableModel;
    private final JScrollPane paneMemory;

    public TableMemory(MemoryTableModel tableModel, JScrollPane pm) {
        this.paneMemory = Objects.requireNonNull(pm);
        this.tableModel = Objects.requireNonNull(tableModel);

        super.setModel(this.tableModel);
        super.setFont(MEMORY_CELLS_FONT);
        super.setCellSelectionEnabled(true);
        super.setFocusCycleRoot(true);
        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.getTableHeader().setFont(MEMORY_CELLS_FONT);
        super.setDefaultRenderer(Object.class, new MemCellRenderer());
        setOpaque(true);

        MemoryCellEditor ed = new MemoryCellEditor();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumn col = super.getColumnModel().getColumn(i);
            col.setPreferredWidth(3 * 18);
            col.setCellEditor(ed);
        }
    }

    public MemoryTableModel getTableModel() {
        return tableModel;
    }

    private static class MemRowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

        MemRowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            setBorder(header.getBorder());
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(MEMORY_CELLS_FONT);
            setOpaque(true);
            setDoubleBuffered(true);
            this.setPreferredSize(new Dimension(4 * 18, header.getPreferredSize().height + 3));
        }

        @Override
        public Component getListCellRendererComponent(JList list, String value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText((value == null) ? "" : value);
            return this;
        }
    }

    private class MemCellRenderer extends JLabel implements TableCellRenderer {
        private final JList<String> rowHeader;
        private final String[] addresses;
        private final Color romColor = new Color(0xE8, 0x68, 0x50);
        private int currentPage;

        MemCellRenderer() {
            setOpaque(true);
            setDoubleBuffered(true);
            setBorder(BorderFactory.createEmptyBorder());

            currentPage = tableModel.getPage();
            addresses = new String[tableModel.getRowCount()];
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = RadixUtils.formatWordHexString(tableModel.getColumnCount() * i
                        + tableModel.getColumnCount() * tableModel.getRowCount() * currentPage) + "h";
            }
            rowHeader = new JList<>(addresses);
            this.setFont(MEMORY_CELLS_FONT);

            FontMetrics fm = rowHeader.getFontMetrics(rowHeader.getFont());
            int char_width = 17;
            if (fm != null) {
                char_width = fm.stringWidth("FF");
            }

            rowHeader.setFixedCellWidth(char_width * 4);
            rowHeader.setFixedCellHeight(getRowHeight());
            rowHeader.setCellRenderer(new MemRowHeaderRenderer(TableMemory.this));
            setHorizontalAlignment(CENTER);
            paneMemory.setRowHeaderView(rowHeader);
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

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                this.setBackground(TableMemory.this.getSelectionBackground());
                this.setForeground(TableMemory.this.getSelectionForeground());
            } else {
                if (tableModel.isROMAt(row, column)) {
                    this.setBackground(romColor);
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
    }

    private class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField textField = new JTextField();

        public MemoryCellEditor() {
            FontMetrics fm = getFontMetrics(getFont());
            if (fm != null) {
                textField.setSize(fm.stringWidth("0xFFFFFFFF"), 2 * fm.getHeight());
                textField.setBorder(BorderFactory.createEmptyBorder());
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (!isSelected) {
                return null;
            }
            textField.setText("0x" + String.format("%02X", tableModel.getRawValueAt(row, column)));
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }
    }
}
