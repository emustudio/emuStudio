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
package net.sf.emustudio.brainduck.memory.gui;

import emulib.plugins.memory.MemoryContext;
import javax.swing.table.AbstractTableModel;

public class MemoryTableModel extends AbstractTableModel {
    private final MemoryContext mem;
    private int currentPage = 0;
    private final int ROW_COUNT = 16;
    private final int COLUMN_COUNT = 16;

    MemoryTableModel(MemoryContext memory) {
        this.mem = memory;
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
        } catch (NumberFormatException e) {
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
