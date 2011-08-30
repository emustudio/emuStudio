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

import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.IDebugColumn;
import emuLib8.plugins.cpu.IDisassembler;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTableModel extends AbstractTableModel {
    private static final int MAX_ROW_COUNT = 13;
    private IDebugColumn[] columns;
    private ICPU cpu;

    private int page; // The page of the debug table

    /**
     * This indicates a number of instructions that will be showed before
     * current instruction and hopefully after current instruction. It is
     * dependent on MAX_ROW_COUNT.
     */
    private int gapInstr;

    /** Creates a new instance of DebugTableModel */
    public DebugTableModel(ICPU cpu) {
        this.cpu = cpu;
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
        page -= 1;
        try {
            int loc = rowToLocation(MAX_ROW_COUNT-1);
            if (loc < 0)
                page++;
        } catch(IndexOutOfBoundsException e) {
            page++; // the first page
        }
        fireTableDataChanged();
    }

    /**
     * Go to the first page in the debug table
     */
    public void firstPage() {
        page = locationToPage(0);
        fireTableDataChanged();
    }
    
    /**
     * Got to next page
     */
    public void nextPage() {
        page += 1;
        try {
            int loc = rowToLocation(0);
        } catch(IndexOutOfBoundsException e) {
            page--; // the last page
        }
        fireTableDataChanged();
    }

    /**
     * Got to the last page
     *
     * Fast version.
     */
    public void lastPage() {
        int gap = 100; // empiric value
        int llocation;
        do {
            try {
                do {
                    page += gap;
                    llocation = rowToLocation(0, page);
                } while (true);
            } catch (IndexOutOfBoundsException e) {
                if (gap > 1) {
                    page -= gap;
                    gap /= 2;
                    continue;
                }
            }
            if (gap > 1) {
                page -= gap;
                gap /= 2;
            } else
                break;
        } while (true);
        page--;
        fireTableDataChanged();
    }

    /**
     * Sets the current page
     */
    public void currentPage() {
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
        try {
            columns[columnIndex].setDebugValue(rowToLocation(rowIndex), aValue);
        } catch(IndexOutOfBoundsException e) {}
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
        try {
            return (cpu.getInstrPosition() == rowToLocation(rowIndex));
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Return row index for current instruction.
     *
     * The number is not fixed but depends on the fact if the current
     * instruction number is less than on gapInstr number. If yes, the row is
     * equal to the current instruction number. Otherwise the row is equal to
     * gapInstr number.
     *
     * @return row index for current instruction
     */
    private int computeCurrentRow() {
        int loc = cpu.getInstrPosition();

        if (loc == 0)
            return 0;

        int row = 0;

        try {
            IDisassembler dis = cpu.getDisassembler();
            do {
                row++;
            } while (((loc = dis.getPreviousInstructionLocation(loc)) > 0)
                    && (row < gapInstr));
        } catch (NullPointerException x) {
            row = 0;
        }
        return row;
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
    private int rowToLocation(int row, int ppage) throws IndexOutOfBoundsException {
        // the row of current instruction is always gapInstr+1
        int location = cpu.getInstrPosition();
        int tmp;
        IDisassembler dis = cpu.getDisassembler();

        int rowCurrent = computeCurrentRow();
        int rowWanted = row + MAX_ROW_COUNT * ppage;

        try {
            while (rowWanted < rowCurrent) {
                // up
                tmp = dis.getPreviousInstructionLocation(location);
                rowCurrent--;
                location = tmp;
                if (location < 0) {
                    throw new IndexOutOfBoundsException();
                }
            }
            while (rowWanted > rowCurrent) {
                // down
                tmp = dis.getNextInstructionLocation(location);
                rowCurrent++;
                location = tmp;
            }
        } catch (NullPointerException x) {
            location = cpu.getInstrPosition();
        }
        return location;
    }

    private int rowToLocation(int row) throws IndexOutOfBoundsException {
        return rowToLocation(row, page);
    }


    /**
     * Return the page in the debug table that shows the location
     *
     * Fast version.
     *
     * @param location the memory location
     * @return page that shows the instruction on the memory location
     */
    public int locationToPage(int location) {
        int ppage = page;
        int llocation;
        int gap = 100;

        try {
            llocation = rowToLocation(0);
        } catch (IndexOutOfBoundsException e) {
            llocation = cpu.getInstrPosition();
        }

        if (llocation < location) {
            do {
                try {
                    do {
                        ppage += gap;
                        llocation = rowToLocation(0,ppage);
                    } while(llocation <= location);
                } catch(IndexOutOfBoundsException e) {
                    if (gap > 1) {
                        ppage -= gap;
                        gap /= 2;
                        continue;
                    }
                }
                if ((llocation > location) && (gap > 1)) {
                    ppage -= gap;
                    gap /=2;
                } else
                    break;
            } while (true);
            ppage--;
        } else if (llocation > location) {
            do {
                try {
                    do {
                        ppage -= gap;
                        llocation = rowToLocation(0,ppage);
                    } while(llocation >= location);
                } catch(IndexOutOfBoundsException e) {
                    if (gap > 1) {
                        ppage += gap;
                        gap /= 2;
                        continue;
                    }
                }
                if ((llocation < location) && (gap > 1)) {
                    ppage += gap;
                    gap /=2;
                } else
                    break;
            } while(true);
            ppage++;
        }
        return ppage;
    }

}
