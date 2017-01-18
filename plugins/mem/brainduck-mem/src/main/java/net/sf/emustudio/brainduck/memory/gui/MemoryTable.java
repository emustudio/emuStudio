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
package net.sf.emustudio.brainduck.memory.gui;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import static emulib.runtime.RadixUtils.formatWordHexString;

class MemoryTable extends JTable {
    private final static int CHAR_WIDTH = 17;
    
    private final MemoryTableModel model;
    private final JScrollPane scrollPane;

    MemoryTable(MemoryTableModel model, JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        this.model = model;
        this.setModel(this.model);
        this.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.setCellSelectionEnabled(true);
        this.setFocusCycleRoot(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getTableHeader().setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.setDefaultRenderer(Object.class, new MemCellRenderer());

        MemoryCellEditor ed = new MemoryCellEditor();
        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = this.getColumnModel().getColumn(i);
            col.setPreferredWidth(3 * CHAR_WIDTH);
            col.setCellEditor(ed);
        }
    }

    private class MemRowHeaderRenderer extends JLabel implements ListCellRenderer {

        MemRowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(new Font("Monospaced", Font.PLAIN, 11));
            setPreferredSize(new Dimension(4 * CHAR_WIDTH, header.getPreferredSize().height));
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class MemCellRenderer extends JLabel implements TableCellRenderer {
        private final JList rowHeader;
        private final String adresses[];
        private int currentPage;

        MemCellRenderer() {
            currentPage = model.getPage();
            adresses = new String[model.getRowCount()];
            for (int i = 0; i < adresses.length; i++) {
                adresses[i] = formatWordHexString(
                        model.getColumnCount() * i + model.getColumnCount() * model.getRowCount() * currentPage
                ) + "h";
            }
            this.setOpaque(true);
            rowHeader = new JList(adresses);
            this.setFont(new Font("Monospaced", Font.PLAIN, 11));

            FontMetrics fm = rowHeader.getFontMetrics(rowHeader.getFont());
            int char_width = CHAR_WIDTH;
            if (fm != null) {
                char_width = fm.stringWidth("FF");
            }

            rowHeader.setFixedCellWidth(char_width * 4);
            rowHeader.setFixedCellHeight(getRowHeight());
            rowHeader.setCellRenderer(new MemRowHeaderRenderer(MemoryTable.this));
            setHorizontalAlignment(CENTER);
            scrollPane.setRowHeaderView(rowHeader);
        }

        private void remakeAdresses() {
            if (currentPage == model.getPage()) {
                return;
            }
            currentPage = model.getPage();
            for (int i = 0; i < adresses.length; i++) {
                adresses[i] = String.format("%1$04Xh",
                        model.getColumnCount() * i + model.getColumnCount() * model.getRowCount() * currentPage);
            }
            rowHeader.setListData(adresses);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(MemoryTable.this.getSelectionBackground());
                setForeground(MemoryTable.this.getSelectionForeground());
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            remakeAdresses();
            setText(value.toString());
            return this;
        }
    }

    private class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField component = new JTextField();

        MemoryCellEditor() {
            FontMetrics fm = getFontMetrics(getFont());
            if (fm != null) {
                component.setSize(fm.stringWidth("0xFFFFFF"), fm.getHeight() + 10);
                component.setBorder(null);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int rowIndex, int vColIndex) {
            if (isSelected == false) {
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
