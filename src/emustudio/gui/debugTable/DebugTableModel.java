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

package emustudio.gui.debugTable;

import javax.swing.table.*;

import emuLib8.plugins.memory.IMemory;
import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.IDebugColumn;
import emuLib8.plugins.cpu.IDisassembler;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTableModel extends AbstractTableModel {
    private static final int MAX_ROW_COUNT = 15;
    private IDebugColumn[] columns;
    private ICPU cpu;
    private IMemory mem;

    private int page; // The page of the debug table

    /**
     * This indicates a number of instructions that will be showed before
     * current instruction and hopefully after current instruction. It is
     * dependent on MAX_ROW_COUNT.
     */
    private int gapInstr;

    /** Creates a new instance of DebugTableModel */
    public DebugTableModel(ICPU cpu, IMemory mem) {
        this.cpu = cpu;
        this.mem = mem;
        page = 0;

        IDisassembler dis = cpu.getDisassembler();

        if (cpu.isBreakpointSupported()) {
            columns = new IDebugColumn[4];
            columns[0] = new ColumnBreakpoint(cpu);
            columns[1] = new ColumnAddress();
            columns[2] = new ColumnMnemo(dis);
            columns[3] = new ColumnOpcode(dis);
        } else {
            columns = new IDebugColumn[3];
            columns[0] = new ColumnAddress();
            columns[1] = new ColumnMnemo(dis);
            columns[2] = new ColumnOpcode(dis);
        }
        gapInstr = MAX_ROW_COUNT / 2;
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
        // TODO: test for the first page
        page -= 1;
        fireTableDataChanged();
    }
    
    /**
     * Got to nextPage page
     */
    public void nextPage() {
        // TODO: test for the last page
        page += 1;
        fireTableDataChanged();
    }

    /**
     * Sets the current page
     */
    public void gotoPC() {
        page = 0;
        fireTableDataChanged();
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
            return columns[columnIndex].getDebugValue(rowToLocation(rowIndex));
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
        columns[columnIndex].setDebugValue(rowToLocation(rowIndex), aValue);
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
     * Determine if the instruction at rowIndex is current
     * instruction.
     * 
     * @param rowIndex The row in the debug table
     * @return true if the row represents current instruction
     */
    public boolean isCurrent(int rowIndex) {
        return (cpu.getInstrPosition() == rowToLocation(rowIndex));
    }


    /**
     * This method converts debug table row (in emuStudio) into memory location.
     * Pages are taken into account.
     *
     * @param row row index in the debug table
     * @return memory location that the row is pointing at
     * @throws IndexOutOfBoundsException when debug row corresponds to memory
     * location that exceeds boundaries
     */
    private int rowToLocation(int row) throws IndexOutOfBoundsException {
        // the row of current instruction is always gapInstr+1
        int location = cpu.getInstrPosition();
        int tmp;
        IDisassembler dis = cpu.getDisassembler();

        int rowCurrent = gapInstr + page * MAX_ROW_COUNT;
        int rowWanted = row + MAX_ROW_COUNT * page;

        while (rowWanted < rowCurrent) {
            // up
            tmp = dis.getPreviousInstructionLocation(location);
            if (tmp >= 0) {
                rowCurrent--;
                location = tmp;
            }
            else
                break;
        }
        while (rowWanted > rowCurrent) {
            // down
            try {
                tmp = dis.getNextInstructionLocation(location);
                rowCurrent++;
                location = tmp;
            } catch(IndexOutOfBoundsException e) {
                break;
            }
        }
        return location;
    }

}
