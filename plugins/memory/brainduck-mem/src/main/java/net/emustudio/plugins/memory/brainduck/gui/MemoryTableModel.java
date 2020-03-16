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
package net.emustudio.plugins.memory.brainduck.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class MemoryTableModel extends AbstractTableModel {
    private final MemoryContext<Short> mem;
    private int currentPage = 0;
    private final int ROW_COUNT = 16;
    private final int COLUMN_COUNT = 16;

    MemoryTableModel(MemoryContext<Short> memory) {
        this.mem = Objects.requireNonNull(memory);
    }

    @Override
    public int getRowCount() {
        return ROW_COUNT;
    }

    // from 00-FF
    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int col) {
        return String.format("%1$02X", col);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        if (pos >= mem.getSize()) {
            return ".";
        }
        return String.format("%1$02X", mem.read(pos));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        try {
            mem.write(pos, Short.decode(String.valueOf(aValue)));
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    void dataChangedAt(int address) {
        int page = address / (ROW_COUNT * COLUMN_COUNT);
        if (page == this.currentPage) {
            int positionInPage = address % (ROW_COUNT * COLUMN_COUNT);
            int row = positionInPage / COLUMN_COUNT;
            int col = positionInPage % COLUMN_COUNT;
            fireTableCellUpdated(row, col);
        }
    }

    void setPage(int page) throws IndexOutOfBoundsException {
        if (page >= getPageCount() || page < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentPage = page;
        fireTableDataChanged();
    }

    int getPage() {
        return currentPage;
    }

    int getPageCount() {
        return mem.getSize() / (ROW_COUNT * COLUMN_COUNT);
    }

}
