/*
 * Copyright (C) 2016 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.ssem.memory.gui;

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

public class MemoryTable extends JTable {
    private final static int CHAR_WIDTH = 17;
    private final static int[] COLUMN_WIDTH = new int[] {
        3 * CHAR_WIDTH, 17 * CHAR_WIDTH, 5 * CHAR_WIDTH
    };
    private final static Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 12);
    
    private final MemoryTableModel model;
    private final JScrollPane scrollPane;

    public MemoryTable(MemoryTableModel model, JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        this.model = model;
        this.setModel(this.model);
        this.setFont(DEFAULT_FONT);
        this.setCellSelectionEnabled(true);
        this.setFocusCycleRoot(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getTableHeader().setFont(DEFAULT_FONT);
        this.setDefaultRenderer(Object.class, new MemoryCellRenderer());

        MemoryCellEditor ed = new MemoryCellEditor();

        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = getColumnModel().getColumn(i);
            col.setPreferredWidth(COLUMN_WIDTH[i]);
            col.setCellEditor(ed);
        }
    }

    public class MemoryRowHeaderRenderer extends JLabel implements ListCellRenderer {
        private final int height;

        public MemoryRowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            this.height = header.getPreferredSize().height;
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(DEFAULT_FONT);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            setPreferredSize(new Dimension(COLUMN_WIDTH[index], height));
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    public class MemoryCellRenderer extends JLabel implements TableCellRenderer {
        private final JList rowHeader;
        private final String columnNames[];

        public MemoryCellRenderer() {
            columnNames = new String[model.getColumnCount()];
            for (int i = 0; i < columnNames.length; i++) {
                columnNames[i] = model.getColumnName(0);
            }
            this.setOpaque(true);
            rowHeader = new JList(columnNames);
            this.setFont(DEFAULT_FONT);

            rowHeader.setFixedCellHeight(getRowHeight());
            rowHeader.setCellRenderer(new MemoryRowHeaderRenderer(MemoryTable.this));
            setHorizontalAlignment(CENTER);
            scrollPane.setRowHeaderView(rowHeader);
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
            setText(value.toString());
            return this;
        }
    }

    private class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField component = new JTextField();

        private void setComponentSize(int columnIndex) {
            FontMetrics fm = getFontMetrics(getFont());
            if (fm != null) {
                component.setSize(COLUMN_WIDTH[columnIndex], fm.getHeight() + 10);
                component.setBorder(null);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int rowIndex, int columnIndex) {
            if (!isSelected) {
                return null;
            }
            setComponentSize(columnIndex);
            component.setText((String) value);
            return component;
        }

        @Override
        public Object getCellEditorValue() {
            return component.getText();
        }
    }    
}
