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
import emulib.runtime.NumberUtils;
import emulib.runtime.NumberUtils.Strategy;
import emulib.runtime.RadixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class MemoryTableModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryTableModel.class);

    final static int COLUMN_HEX_VALUE = 32;
    final static int COLUMN_CHAR_VALUE = 33;

    private final static int ROW_COUNT = 32;
    private final static int COLUMN_COUNT = 2 + 32;

    private final MemoryContext<Byte> memory;

    MemoryTableModel(MemoryContext<Byte> memory) {
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
            case COLUMN_HEX_VALUE:
                return "Value (hex)";
            case COLUMN_CHAR_VALUE:
                return "Raw";
        }
        return "";
    }
    
    private byte[] readLineBits(Byte[] line) {
        byte[] lineBits = new byte[32];

        int j = 0;
        for (byte b : line) {
            for (int i = 7; i >= 0; i--) {
                lineBits[j++] = (byte)((b >>> i) & 1);
            }
        }
        return lineBits;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            Byte[] row = memory.readWord(rowIndex * 4);
            int value = NumberUtils.readInt(row, Strategy.REVERSE_BITS);

            switch (columnIndex) {
                case COLUMN_HEX_VALUE:
                    return RadixUtils.getDwordHexString(value).toUpperCase();
                case COLUMN_CHAR_VALUE:
                    return "" + (char)(value & 0xFF) + (char)((value >>> 8) & 0xFF) +
                        (char)((value >>> 16) & 0xFF) + (char)((value >>> 24) & 0xFF);
                default:
                    byte[] lineBits = readLineBits(row);
                    return lineBits[columnIndex];
            }
        } catch (Exception e) {
            LOGGER.debug("[location={}] Could not read value from memory", rowIndex, e);
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (isCellEditable(rowIndex, columnIndex)) {
            try {
                Byte[] row = memory.readWord(rowIndex * 4);
                
                if (columnIndex == COLUMN_HEX_VALUE) {
                    writeHex(aValue, row);
                } else if (columnIndex == COLUMN_CHAR_VALUE) {
                    writeChar(aValue, row);
                } else if (columnIndex >= 0 && columnIndex < 33) {
                    writeBit(aValue, columnIndex, row);
                }
                memory.writeWord(rowIndex * 4, row);

                fireTableCellUpdated(rowIndex, columnIndex);
            } catch (Exception e) {
                LOGGER.debug("[location={}, value={}] Could not set value to memory", rowIndex, aValue, e);
            }
        }
    }

    private void writeHex(Object aValue, Byte[] row) {
        int value = Integer.parseInt(String.valueOf(aValue), 16);
        NumberUtils.writeInt(value, row, Strategy.REVERSE_BITS);
    }

    private void writeChar(Object aValue, Byte[] row) {
        int i = 3;
        int value = 0;
        for (char c : String.valueOf(aValue).toCharArray()) {
            value |= ((c & 0xFF) << (i*8));
            i -= 1;
            if (i < 0) {
                break;
            }
        }
        NumberUtils.writeInt(value, row, Strategy.REVERSE_BITS);
    }

    private void writeBit(Object aValue, int columnIndex, Byte[] row) {
        int value;
        value = Integer.parseInt(String.valueOf(aValue), 2);

        int byteIndex = columnIndex / 8;
        int bitIndex = 7 - columnIndex % 8;

        int bitMask = ~(1 << bitIndex);
        int bitValue = (value << bitIndex);

        row[byteIndex] = (byte)(row[byteIndex] & bitMask | bitValue);
    }


    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex >= 0 && columnIndex <= 33;
    }
    
    public void dataChangedAt(int address) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            fireTableCellUpdated(address, i);
        }
    }
    
    public void clear() {
        memory.clear();
        fireTableDataChanged();
    }

}
