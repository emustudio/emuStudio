/*
 * DebugTableModel.java
 *
 * Created on Pondelok, 2007, marec 26, 16:29
 *
 * KEEP IT SIMPLY STUPID
 * YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2011 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui.utils;

import javax.swing.table.*;

import emuLib8.plugins.memory.IMemory;
import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.IDebugColumn;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTableModel extends AbstractTableModel {
    private int page; // The page of the debug table
    private int lastPage; // The last page is determined at runtime
    
    private static final int MAX_ROW_COUNT = 15;
    private IDebugColumn[] columns;
    
    /** Creates a new instance of DebugTableModel */
    public DebugTableModel(ICPU cpu, IMemory mem) {
        page = 0;
        lastPage = mem.getSize()/MAX_ROW_COUNT;
        columns = cpu.getDebugColumns();
    }

    /**
     * Get the number of rows in the debug table.
     * @return rows count
     */
    @Override
    public int getRowCount() {
        return MAX_ROW_COUNT;
    }

    /**
     * Get the number of columns in the debug table.
     *
     * @return columns count
     */
    @Override
    public int getColumnCount() {
        try {  return columns.length; }
        catch(NullPointerException e) {
            return 0;
        }
    }

    /**
     * Gets the title of the debug column.
     *
     * @param col
     *   Column index
     * @return the column title
     */
    @Override
    public String getColumnName(int col) {
        return columns[col].getTitle();
    }

    /**
     * Gets the type of the column.
     *
     * @param columnIndex column index
     * @return class representing the column type
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns[columnIndex].getClassType();
    }
    
    /**
     * Go to previous page
     */
    public void previousPage() {
        if (page == 0)
            return;

        page -= 1;
    }
    
    /**
     * Got to nextPage page
     */
    public void nextPage() {
        if (page < lastPage)
            page += 1;
    }

    /**
     * Sets the current page
     */
    public void gotoPC() {
        if (columns.length > 0)

        page = columns[0].getCurrentDebugRow() / MAX_ROW_COUNT;
    }

    /**
     * Returns a value at specified location in the debug table.
     *
     * @param rowIndex
     *   The row index
     * @param columnIndex
     *   The column index
     * @return value at (rowIndex, columnIndex)
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return columns[columnIndex].getDebugValue(rowIndex
                    + page * MAX_ROW_COUNT);
            
        } catch(Exception x) {
            return null;
        }
    }

    /**
     * Sets the value at specified location into the debug table.
     *
     * @param aValue The value
     * @param rowIndex the index of the row
     * @param columnIndex the index of the column
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        columns[columnIndex].setDebugValue(rowIndex + page * MAX_ROW_COUNT,
                aValue);
    }

    /**
     * Determine whether the column in the debug table is editable.
     *
     * @param rowIndex row index. It is irrelevant.
     * @param columnIndex column index
     * @return true if column is editable, false otherwise
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns[columnIndex].isEditable();
    }
    
    /**
     * Determine if the instruction at rowIndex,columnIndex is current
     * instruction.
     * 
     * @param rowIndex The row in the debug table
     * @param columnIndex
     * @return
     */
    public boolean isCurrent(int rowIndex, int columnIndex) {
        return columns[columnIndex].isCurrent(rowIndex + page * MAX_ROW_COUNT);
    }
}
