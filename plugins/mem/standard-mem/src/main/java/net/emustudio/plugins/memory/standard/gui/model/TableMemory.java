/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.memory.standard.gui.model;

import net.emustudio.emulib.runtime.helpers.RadixUtils;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Objects;

public class TableMemory extends JTable {
    private final MemoryTableModel tableModel;
    private final JScrollPane paneMemory;

    public TableMemory(MemoryTableModel tableModel, JScrollPane pm) {
        this.paneMemory = Objects.requireNonNull(pm);
        this.tableModel = Objects.requireNonNull(tableModel);

        super.setModel(this.tableModel);
        super.setFont(new Font("Monospaced", Font.PLAIN, 12));
        super.setCellSelectionEnabled(true);
        super.setFocusCycleRoot(true);
        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.getTableHeader().setFont(new Font("Monospaced", Font.PLAIN, 12));
        super.setDefaultRenderer(Object.class, new MemCellRenderer(this));

        MemoryCellEditor ed = new MemoryCellEditor();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumn col = super.getColumnModel().getColumn(i);
            col.setPreferredWidth(3 * 17);
            col.setCellEditor(ed);
        }
    }

    public MemoryTableModel getTableModel() {
        return tableModel;
    }

    private static class MemRowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

        MemRowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(new Font("Monospaced", Font.PLAIN, 11));
            this.setPreferredSize(new Dimension(4 * 17, header.getPreferredSize().height));
        }

        @Override
        public Component getListCellRendererComponent(JList list, String value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText((value == null) ? "" : value);
            return this;
        }
    }

    private class MemCellRenderer extends JLabel implements TableCellRenderer {

        private JList<String> rowHeader;
        private String[] adresses;
        private int currentPage;
        private TableMemory tm;
        private Color romColor;

        MemCellRenderer(TableMemory tm) {
            this.tm = tm;
            romColor = new Color(0xE8, 0x68, 0x50);
            currentPage = tableModel.getPage();
            adresses = new String[tableModel.getRowCount()];
            for (int i = 0; i < adresses.length; i++) {
                adresses[i] = RadixUtils.formatWordHexString(tableModel.getColumnCount() * i
                    + tableModel.getColumnCount() * tableModel.getRowCount() * currentPage) + "h";
            }
            this.setOpaque(true);
            rowHeader = new JList<>(adresses);
            this.setFont(new Font("Monospaced", Font.PLAIN, 11));

            FontMetrics fm = rowHeader.getFontMetrics(rowHeader.getFont());
            int char_width = 17;
            if (fm != null) {
                char_width = fm.stringWidth("FF");
            }

            rowHeader.setFixedCellWidth(char_width * 4);
            rowHeader.setFixedCellHeight(getRowHeight());
            rowHeader.setCellRenderer(new MemRowHeaderRenderer(this.tm));
            setHorizontalAlignment(CENTER);
            paneMemory.setRowHeaderView(rowHeader);
        }

        private void remakeAdresses() {
            if (currentPage == tableModel.getPage()) {
                return;
            }
            currentPage = tableModel.getPage();
            for (int i = 0; i < adresses.length; i++) {
                adresses[i] = String.format("%1$04Xh",
                    tableModel.getColumnCount() * i + tableModel.getColumnCount()
                        * tableModel.getRowCount() * currentPage);
            }
            rowHeader.setListData(adresses);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                this.setBackground(tm.getSelectionBackground());
                this.setForeground(tm.getSelectionForeground());
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
            remakeAdresses();
            setText(value.toString());
            return this;
        }
    }

    private class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
        // This is the component that will handle the editing of the cell value

        JTextField component;

        public MemoryCellEditor() {
            component = new JTextField();
            FontMetrics fm = getFontMetrics(getFont());
            if (fm != null) {
                component.setSize(fm.stringWidth("0xFFFFFF"), fm.getHeight() + 10);
                component.setBorder(null);
            }
        }

        // This method is called when a cell value is edited by the user.
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int rowIndex, int vColIndex) {
            // 'value' is value contained in the cell located at (rowIndex, vColIndex)
            if (!isSelected) {
                return null;
            }
            component.setText("0x" + value);
            return component;
        }

        @Override
        public Object getCellEditorValue() {
            return component.getText();
        }
    }
}
