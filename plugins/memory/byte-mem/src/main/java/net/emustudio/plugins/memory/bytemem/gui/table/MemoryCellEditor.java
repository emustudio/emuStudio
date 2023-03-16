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
