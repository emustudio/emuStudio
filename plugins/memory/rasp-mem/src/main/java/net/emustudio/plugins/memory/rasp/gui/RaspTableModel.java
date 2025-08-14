/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

/**
 * MODEL for the table with memory content.
 */
public class RaspTableModel extends AbstractTableModel {

    private final RaspMemoryContext memory;

    /**
     * Default constructor.
     *
     * @param memory the memory that will hold the content.
     */
    public RaspTableModel(RaspMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        //only numeric value (at column 1) is editable
        return columnIndex == 1;
    }

    /**
     * Get number of items in memory.
     *
     * @return the number of items in memory
     */
    @Override
    public int getRowCount() {
        return memory.getSize();
    }

    /**
     * Get number of columns in memory table; it is 3: |"address"| "numeric value" |"cell value"|
     *
     * @return 3
     */
    @Override
    public int getColumnCount() {
        return 2;
    }

    /**
     * Returns string contained in the address column (column No. 0).
     *
     * @param rowIndex the index of the row in the address column
     * @return string contained in the address column
     */
    private String getAddressColumnText(int rowIndex) {
        return memory.getLabel(rowIndex).map(l -> rowIndex + " " + l.getLabel()).orElse(String.valueOf(rowIndex));
    }

    /**
     * Returns string contained in the numeric value column (column No. 1)
     *
     * @param rowIndex the index of the row in the numeric value column
     * @return string contained in the numeric value column
     */
    private String getNumericValueColumnText(int rowIndex) {
        int item = memory.read(rowIndex);
        return String.valueOf(item);
    }

    /**
     * Returns the string representation of cell in the memory table cell at given position.
     *
     * @param rowIndex    row index
     * @param columnIndex column index
     * @return string representation of memory table cell at given position
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return getAddressColumnText(rowIndex);
            case 1:
                return getNumericValueColumnText(rowIndex);
        }
        return "";
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Address";
            case 1:
                return "Numeric Value";
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        try {
            int numberValue = RadixUtils.getInstance().parseRadix((String) value);
            memory.write(rowIndex, numberValue);
        } catch (NumberFormatException e) {
            //do nothing, invalid value was inserted
        }
    }
}
