/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.plugins.memory.MemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class MemoryTableModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryTableModel.class);

    final static int COLUMN_BINARY_VALUE = 1;
    final static int COLUMN_HEX_VALUE = 2;

    private final static int ROW_COUNT = 32;
    private final static int COLUMN_COUNT = 3;

    private final MemoryContext<Integer> memory;

    MemoryTableModel(MemoryContext<Integer> memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public int getRowCount() {
        return ROW_COUNT;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "No.";
            case COLUMN_BINARY_VALUE:
                return "Value (binary)";
            case COLUMN_HEX_VALUE:
                return "Value (hex)";
        }
        return "";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            int value = memory.read(rowIndex);

            switch (columnIndex) {
                case 0:
                    return String.format("%02X", rowIndex);
                case COLUMN_BINARY_VALUE:
                    return String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0');
                case COLUMN_HEX_VALUE:
                    return String.format("%04X", value);
            }
        } catch (Exception e) {
            LOGGER.debug("[location={}] Could not reead value from memory", rowIndex, e);
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (isCellEditable(rowIndex, columnIndex)) {
            try {
                int value;
                if (columnIndex == COLUMN_BINARY_VALUE) {
                    value = Integer.parseInt(String.valueOf(aValue), 2);
                } else {
                    value = Integer.parseInt(String.valueOf(aValue), 16);
                }
                memory.write(rowIndex, value);
                fireTableCellUpdated(rowIndex, COLUMN_BINARY_VALUE);
                fireTableCellUpdated(rowIndex, COLUMN_HEX_VALUE);
            } catch (Exception e) {
                LOGGER.debug("[location={}, value={}] Could not set value to memory", rowIndex, aValue, e);
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COLUMN_BINARY_VALUE || columnIndex == COLUMN_HEX_VALUE;
    }
    
    public void dataChangedAt(int address) {
        fireTableCellUpdated(address, COLUMN_BINARY_VALUE);
        fireTableCellUpdated(address, COLUMN_HEX_VALUE);
    }
    
    public void clear() {
        memory.clear();
        fireTableDataChanged();
    }

}
