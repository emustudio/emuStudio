/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;
import java.util.Optional;

public class MemoryTableModel extends AbstractTableModel {
    private static final int ROW_COUNT = 16;
    private static final int COLUMN_COUNT = 16;

    private final MemoryContextImpl memory;
    private int currentPage = 0;
    private int currentBank = 0;
    private volatile boolean asciiMode;

    public MemoryTableModel(MemoryContextImpl memory) {
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
    public String getColumnName(int col) {
        return String.format("%1$02X", col);
    }

    boolean isROMAt(int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        return memory.isReadOnly(address);
    }

    boolean isAtBANK(int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        return address < memory.getCommonBoundary();
    }

    public void setAsciiMode(boolean asciiMode) {
        this.asciiMode = asciiMode;
        fireTableDataChanged();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int value = getRawValueAt(rowIndex, columnIndex);
        return asciiMode ? (char)value : String.format("%02X", value);
    }

    public int getRawValueAt(int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        if (address >= memory.getSize()) {
            return 0;
        }
        return memory.readBank(address, currentBank) & 0xFF;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        try {
            memory.writeBank(address, Byte.decode(String.valueOf(aValue)), currentBank);
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {
            // ignored
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public void setPage(int page) throws IndexOutOfBoundsException {
        if (page >= getPageCount() || page < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentPage = page;
        fireTableDataChanged();
    }

    public Optional<Integer> findSequence(byte[] sequence, int from) {
        final int size = memory.getSize();
        int offset = 0;
        int foundAddress = -1;
        for (int currentAddr = from; currentAddr < size && offset < sequence.length; currentAddr++) {
            if (memory.readBank(currentAddr, currentBank) == sequence[offset]) {
                if (offset == 0) {
                    foundAddress = currentAddr;
                }
                offset++;
            } else {
                offset = 0;
                foundAddress = -1;
            }
        }

        if (foundAddress == -1) {
            return Optional.empty();
        } else {
            return Optional.of(foundAddress);
        }
    }

    public int getPage() {
        return currentPage;
    }

    public int getPageCount() {
        return memory.getSize() / (ROW_COUNT * COLUMN_COUNT);
    }

    public void setCurrentBank(int bank) {
        if (bank >= memory.getBanksCount() || bank < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentBank = bank;
        fireTableDataChanged();
    }

    public int getCurrentBank() {
        return currentBank;
    }

    public int toAddress(int row, int column) {
        return ROW_COUNT * COLUMN_COUNT * currentPage + row * COLUMN_COUNT + column;
    }
}
