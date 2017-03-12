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
package net.sf.emustudio.memory.standard.gui.model;

import net.sf.emustudio.memory.standard.impl.MemoryContextImpl;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

/**
 * tabulka ma 16 riadkov a 16 stlpcov (od 0 po FF)
 * celkovo tak zobrazuje 16*16 = 256 hodnot na stranku
 * ----------------------------------------------------------------------------
 * model pre tabulku operacnej pamate podporuje strankovanie (zobrazuje len
 * urcity pocet riadkov na stranku) - kvoli zvyseniu rychlosti + pridana podpora
 * "bankovania" pamate = pamat od adresy 0 po COMMON sa da prepinat = banky,
 * zvysna pamat je rovnaka pre vsetky banky
 */
public class MemoryTableModel extends AbstractTableModel {
    private static final int ROW_COUNT = 16;
    private static final int COLUMN_COUNT = 16;

    private final MemoryContextImpl mem;
    private int currentPage = 0;
    private int currentBank = 0;

    public MemoryTableModel(MemoryContextImpl mem) {
        this.mem = Objects.requireNonNull(mem);
        currentPage = 0;
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
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        return mem.isROM(pos);
    }

    boolean isAtBANK(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        return pos < mem.getCommonBoundary();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        if (pos >= mem.getSize()) {
            return ".";
        }
        return String.format("%1$02X", mem.read(pos, currentBank));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        try {
            mem.write(pos, Short.decode(String.valueOf(aValue)), currentBank);
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

    public int findSequence(byte[] sequence, int from) {
        final int size = mem.getSize();
        int offset = 0;
        int foundAddress = -1;
        for (int currentAddr = from; currentAddr < size && offset < sequence.length; currentAddr++) {
            if ((mem.read(currentAddr, currentBank)).byteValue() == sequence[offset]) {
                if (offset == 0) {
                    foundAddress = currentAddr;
                }
                offset++;
            } else {
                offset = 0;
                foundAddress = -1;
            }
        }

        return foundAddress;
    }

    public int getPage() {
        return currentPage;
    }

    public int getPageCount() {
        return mem.getSize() / (ROW_COUNT * COLUMN_COUNT);
    }

    public void setCurrentBank(int bank) {
        if (bank >= mem.getBanksCount() || bank < 0) {
            throw new IndexOutOfBoundsException();
        }
        currentBank = bank;
        fireTableDataChanged();
    }

    public int getCurrentBank() {
        return currentBank;
    }
}
