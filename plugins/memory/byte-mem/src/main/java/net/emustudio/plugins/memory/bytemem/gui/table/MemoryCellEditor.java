/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Objects;

class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final MemoryTableModel tableModel;
    private final JTextField textField = new JTextField();

    public MemoryCellEditor(MemoryTableModel tableModel) {
        this.tableModel = Objects.requireNonNull(tableModel);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        if (!isSelected) {
            return null;
        }
        FontMetrics fm = table.getFontMetrics(table.getFont());
        if (fm != null) {
            textField.setSize(fm.stringWidth("0xFFFFFFFF"), 2 * fm.getHeight());
            textField.setBorder(BorderFactory.createEmptyBorder());
        }

        textField.setText("0x" + String.format("%02X", tableModel.getRawValueAt(row, column)));
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }
}
