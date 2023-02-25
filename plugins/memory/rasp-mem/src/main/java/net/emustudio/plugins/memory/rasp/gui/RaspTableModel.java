/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2006-2023  Peter Jakubčo
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

package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import javax.swing.table.AbstractTableModel;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;

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
    public RaspTableModel(final RaspMemoryContext memory) {
        this.memory = memory;
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
        return 3;
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
     * Returns string contained in the mnemonic column (column No. 2)
     *
     * @param rowIndex the index of the row in the mnemonic column
     * @return string contained in the mnemonic column
     */
    private String getMnemonicColumnValue(int rowIndex) {
        int item = memory.read(rowIndex);
        if (Disassembler.isInstruction(item)) {
            return Disassembler.disassembleMnemo(item).orElse("unknown");
        }
        if (rowIndex != 0) {
            int previousItem = memory.read(rowIndex - 1);
            if (Disassembler.isInstruction(previousItem)) {
                switch (previousItem) {
                    case JMP:
                    case JZ:
                    case JGTZ:
                        return memory
                                .getLabel(item)
                                .map(RaspLabel::getLabel)
                                .orElse(String.valueOf(item));
                }
            }
        }
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
            default:
                return getMnemonicColumnValue(rowIndex);
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Address";
            case 1:
                return "Numeric Value";
            case 2:
                return "Mnemonic";
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        try {
            int numberValue = Integer.decode((String) value);
            memory.write(rowIndex, numberValue);
        } catch (NumberFormatException e) {
            //do nothing, invalid value was inserted
        }
    }
}
