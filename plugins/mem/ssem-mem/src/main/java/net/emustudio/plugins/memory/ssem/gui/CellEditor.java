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
package net.emustudio.plugins.memory.ssem.gui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

class CellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JTextField editComponent = new JTextField();
    private FontMetrics fontMetrics;

    private void setComponentSize(int columnIndex) {
        if (fontMetrics != null) {
            editComponent.setSize(Constants.COLUMN_WIDTH[columnIndex], fontMetrics.getHeight() + Constants.CHAR_HEIGHT);
            editComponent.setBorder(null);
        }
    }

    public void setup(JTable table) {
        fontMetrics = table.getFontMetrics(Constants.DEFAULT_FONT);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int columnIndex) {
        if (!isSelected) {
            return null;
        }
        setComponentSize(columnIndex);
        switch (columnIndex) {
            case MemoryTableModel.COLUMN_RAW_VALUE:
                editComponent.setText("");
                break;
            case MemoryTableModel.COLUMN_HEX_VALUE:
                editComponent.setText("0x" + String.valueOf(value));
                break;
            default:
                editComponent.setText(String.valueOf(value));
                break;
        }
        return editComponent;
    }

    @Override
    public Object getCellEditorValue() {
        return editComponent.getText();
    }

}
