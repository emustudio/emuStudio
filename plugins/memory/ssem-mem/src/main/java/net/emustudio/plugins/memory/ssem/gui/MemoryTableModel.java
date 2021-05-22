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

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class MemoryTableModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryTableModel.class);

    final static int COLUMN_HEX_VALUE = 32;
    final static int COLUMN_RAW_VALUE = 33;

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

    /**
     * Determine if a column index points at a bit which is part of the 3 bits in a memory cell representing a SSEM
     * instruction.
     *
     * @param column column in the memory table
     * @return true if the column represents a SSEM instruction bit
     */
    static boolean isBitInstruction(int column) {
        return column >= 13 && column <= 15;
    }

    /**
     * Determine if a column index points at a bit which is part of the 5 bits in a memory cell representing a "line",
     * or address part of the memory cell.
     *
     * @param column column in the memory table
     * @return true if the column represents a line bit
     */
    static boolean isBitLine(int column) {
        return column >= 0 && column < 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case COLUMN_HEX_VALUE:
                return "Value (hex)";
            case COLUMN_RAW_VALUE:
                return "Raw";
        }
        if (isBitLine(columnIndex)) {
            return "L";
        }
        if (isBitInstruction(columnIndex)) {
            return "I";
        }
        return "";
    }

    private byte[] readLineBits(Byte[] line) {
        byte[] lineBits = new byte[32];

        int j = 0;
        for (byte b : line) {
            for (int i = 7; i >= 0; i--) {
                lineBits[j++] = (byte) ((b >>> i) & 1);
            }
        }
        return lineBits;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            Byte[] row = memory.read(rowIndex * 4, 4);
            int value = NumberUtils.readInt(row, NumberUtils.Strategy.REVERSE_BITS);

            switch (columnIndex) {
                case COLUMN_HEX_VALUE:
                    return RadixUtils.formatDwordHexString(value).toUpperCase();
                case COLUMN_RAW_VALUE:
                    return "" + (char) ((value >>> 24) & 0xFF) + (char) ((value >>> 16) & 0xFF)
                        + (char) ((value >>> 8) & 0xFF) + (char) (value & 0xFF);
                default:
                    byte[] lineBits = readLineBits(row);
                    return lineBits[columnIndex];
            }
        } catch (Exception e) {
            LOGGER.debug("[line={}] Could not read value from memory", rowIndex, e);
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (isCellEditable(rowIndex, columnIndex)) {
            try {
                Byte[] row = memory.read(rowIndex * 4, 4);
                String str = String.valueOf(aValue);

                if (columnIndex == COLUMN_HEX_VALUE) {
                    writeHex(str, row);
                } else if (columnIndex == COLUMN_RAW_VALUE) {
                    writeChar((String) aValue, row);
                } else if (columnIndex >= 0 && columnIndex < 33) {
                    writeBit(str, columnIndex, row);
                }
                memory.write(rowIndex * 4, row);

                fireTableCellUpdated(rowIndex, columnIndex);
            } catch (Exception e) {
                LOGGER.debug("[line={}, value={}] Could not set value to memory", rowIndex, aValue, e);
            }
        }
    }

    private void writeHex(String aValue, Byte[] row) {
        int value = Integer.decode(aValue);
        NumberUtils.writeInt(value, row, NumberUtils.Strategy.REVERSE_BITS);
    }

    private void writeChar(String aValue, Byte[] row) {
        int i = 3;
        int value = 0;

        for (char c : aValue.toCharArray()) {
            value |= ((c & 0xFF) << (i * 8));
            i -= 1;
            if (i < 0) {
                break;
            }
        }
        NumberUtils.writeInt(value, row, NumberUtils.Strategy.REVERSE_BITS);
    }

    private void writeBit(String aValue, int columnIndex, Byte[] row) {
        int value;
        value = Integer.parseInt("0" + aValue, 2);

        int byteIndex = columnIndex / 8;
        int bitIndex = 7 - columnIndex % 8;

        int bitMask = ~(1 << bitIndex);
        int bitValue = (value << bitIndex);

        if ((value & 1) != value) {
            LOGGER.error("[line={}, bit={}, value={}] Could not set bit value. Expected 0 or 1", byteIndex, bitIndex, value);
        } else {
            row[byteIndex] = (byte) (row[byteIndex] & bitMask | bitValue);
        }
    }


    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex >= 0 && columnIndex <= 33;
    }

    void dataChangedAt(int address) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            fireTableCellUpdated(address, i);
        }
    }

    void clear() {
        memory.clear();
        fireTableDataChanged();
    }
}
