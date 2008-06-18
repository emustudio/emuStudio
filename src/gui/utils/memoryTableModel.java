/*
 * memoryTableModel.java
 *
 * Created on Streda, 2007, marec 21, 9:38
 *
 * KEEP IT SIMPLE STUPID
 *
 * YOU AREN'T GONNA NEED IT
 *
 * tabulka ma 16 riadkov a 16 stlpcov (od 0 po FF)
 * celkovo tak zobrazuje 16*16 = 256 hodnot na stranku
 *
 */

package gui.utils;

import interfaces.SMemoryContext;
import javax.swing.table.AbstractTableModel;


/**
 * model pre tabulku operacnej pamate
 * podporuje strankovanie (zobrazuje len urcity pocet riadkov na stranku) - kvoli zvyseniu
 * rychlosti
 *
 * @author vbmacher
 */
public class memoryTableModel extends AbstractTableModel {
    private SMemoryContext mem;
    private int currentPage;
    private final int ROW_COUNT = 16;
    private final int COLUMN_COUNT = 16;

    /** Creates a new instance of memoryTableModel */
    public memoryTableModel(SMemoryContext mem) {
        this.mem = mem;
        currentPage = 0;
    }

    // row count
    public int getRowCount() { return ROW_COUNT; }

    // od 00-FF
    public int getColumnCount() { return COLUMN_COUNT; }

    public String getColumnName(int col) {
        return String.format("%1$02X", col);
    }
    
    public boolean isROMAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        return mem.isRom(pos);
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        if (pos >= mem.getSize()) return ".";
        return String.format("%1$02X", mem.read(pos));
    }
    
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int pos = ROW_COUNT * COLUMN_COUNT * currentPage + rowIndex * COLUMN_COUNT + columnIndex;
        try {
            mem.write(pos,Short.decode(String.valueOf(aValue)));
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {}
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
    
    /* strankovanie */
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
}
