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
package net.sf.emustudio.ssem.memory.gui;

import java.awt.Component;
import java.awt.FontMetrics;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import static net.sf.emustudio.ssem.memory.gui.Constants.CHAR_HEIGHT;
import static net.sf.emustudio.ssem.memory.gui.Constants.COLUMN_WIDTH;
import static net.sf.emustudio.ssem.memory.gui.Constants.DEFAULT_FONT;
import static net.sf.emustudio.ssem.memory.gui.MemoryTableModel.COLUMN_HEX_VALUE;
import static net.sf.emustudio.ssem.memory.gui.MemoryTableModel.COLUMN_RAW_VALUE;

class CellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JTextField editComponent = new JTextField();
    private FontMetrics fontMetrics;
    
    private void setComponentSize(int columnIndex) {
        if (fontMetrics != null) {
            editComponent.setSize(COLUMN_WIDTH[columnIndex], fontMetrics.getHeight() + CHAR_HEIGHT);
            editComponent.setBorder(null);
        }
    }
    
    public void setup(JTable table) {
        fontMetrics = table.getFontMetrics(DEFAULT_FONT);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int columnIndex) {
        if (!isSelected) {
            return null;
        }
        setComponentSize(columnIndex);
        switch (columnIndex) {
            case COLUMN_RAW_VALUE:
                editComponent.setText("");
                break;
            case COLUMN_HEX_VALUE:
                editComponent.setText("0x"+ String.valueOf(value));
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
