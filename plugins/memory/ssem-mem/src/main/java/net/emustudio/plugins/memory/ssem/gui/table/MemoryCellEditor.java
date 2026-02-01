/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

class MemoryCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JTextField textField = new JTextField();

    public MemoryCellEditor() {
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

        textField.setText(String.valueOf(value));
        return textField;
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }
}
