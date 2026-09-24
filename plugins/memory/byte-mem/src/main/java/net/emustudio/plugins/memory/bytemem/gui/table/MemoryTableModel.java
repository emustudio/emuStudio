/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MemoryTableModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryTableModel.class);
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
        return asciiMode ? (char) value : String.format("%02X", value);
    }

    public int getRawValueAt(int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        if (address >= memory.getSize()) {
            return 0;
        }
        return memory.readBank(address, currentBank) & 0xFF;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int address = toAddress(rowIndex, columnIndex);
        try {
            memory.writeBank(address, (byte) (Integer.decode(String.valueOf(value)) & 0xFF), currentBank);
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {
            LOGGER.error("Could not set memory cell at address 0x{} to value {}", Integer.toHexString(address), value, e);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public Optional<Integer> findSequence(byte[] sequence, int from) {
        return findSequences(sequence).stream().filter(address -> address >= from).findFirst();
    }

    public List<Integer> findSequences(byte[] sequence) {
        Objects.requireNonNull(sequence);
        List<Integer> matches = new ArrayList<>();
        int size = memory.getSize();
        if (sequence.length == 0 || sequence.length > size) {
            return matches;
        }

        for (int address = 0; address <= size - sequence.length; address++) {
            boolean matchesAtAddress = true;
            for (int offset = 0; offset < sequence.length; offset++) {
                if (memory.readBank(address + offset, currentBank) != sequence[offset]) {
                    matchesAtAddress = false;
                    break;
                }
            }
            if (matchesAtAddress) {
                matches.add(address);
            }
        }
        return matches;
    }

    public int getPage() {
        return currentPage;
    }

    public void setPage(int page) throws IndexOutOfBoundsException {
        if (page >= getPageCount() || page < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentPage = page;
        fireTableDataChanged();
    }

    public int getPageCount() {
        return memory.getSize() / (ROW_COUNT * COLUMN_COUNT);
    }

    public int getCurrentBank() {
        return currentBank;
    }

    public void setCurrentBank(int bank) {
        if (bank >= memory.getBanksCount() || bank < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentBank = bank;
        fireTableDataChanged();
    }

    public int toAddress(int row, int column) {
        return ROW_COUNT * COLUMN_COUNT * currentPage + row * COLUMN_COUNT + column;
    }
}
