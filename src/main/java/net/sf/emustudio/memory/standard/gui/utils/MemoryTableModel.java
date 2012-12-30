/*
 * MemoryTableModel.java
 *
 * Created on Streda, 2007, marec 21, 9:38
 *
 * KEEP IT SIMPLE STUPID
 * YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
 *
 * ----------------------------------------------------------------------------
 * tabulka ma 16 riadkov a 16 stlpcov (od 0 po FF)
 * celkovo tak zobrazuje 16*16 = 256 hodnot na stranku
 *
 */

package net.sf.emustudio.memory.standard.gui.utils;

import javax.swing.table.AbstractTableModel;
import net.sf.emustudio.memory.standard.impl.MemoryContextImpl;


/**
 * model pre tabulku operacnej pamate
 * podporuje strankovanie (zobrazuje len urcity pocet riadkov na stranku) - kvoli zvyseniu
 * rychlosti
 * + pridana podpora "bankovania" pamate = pamat od adresy 0 po COMMON sa 
 * da prepinat = banky, zvysna pamat je rovnaka pre vsetky banky
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class MemoryTableModel extends AbstractTableModel {
    private MemoryContextImpl mem;
    private int currentPage=0;
    private int currentBank =0;
    private final int ROW_COUNT = 16;
    private final int COLUMN_COUNT = 16;

    /** Creates a new instance of memoryTableModel */
    public MemoryTableModel(MemoryContextImpl mem) {
        this.mem = mem;
        currentPage = 0;
    }
    
    // row count
    public int getRowCount() { return ROW_COUNT; }

    // od 00-FF
    public int getColumnCount() { return COLUMN_COUNT; }

    @Override
    public String getColumnName(int col) {
        return String.format("%1$02X", col);
    }
    
    public boolean isROMAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        return mem.isROM(pos);
    }
    
    public boolean isAtBANK(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        if (pos < mem.getCommonBoundary()) return true;
        return false;
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        if (pos >= mem.getSize()) return ".";
        return String.format("%1$02X", mem.read(pos,currentBank));
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        try {
            mem.write(pos,Short.decode(String.valueOf(aValue)),currentBank);
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {}
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
    
    /* strankovanie a bankovanie */
    public void setPage(int page) throws IndexOutOfBoundsException {
        if (page >= getPageCount() || page < 0) throw new IndexOutOfBoundsException();
        currentPage = page;
        fireTableDataChanged();
    }
    
    public int getPage() {
        return currentPage;
    }
    
    public int getPageCount() {
        return mem.getSize()/(ROW_COUNT * COLUMN_COUNT);
    }
    
    public void setCurrentBank(int bank) {
        if (bank >= mem.getBanksCount() || bank < 0) throw new IndexOutOfBoundsException();
        currentBank = bank; 
        fireTableDataChanged();
    }
    public int getCurrentBank() { return currentBank; }

}
