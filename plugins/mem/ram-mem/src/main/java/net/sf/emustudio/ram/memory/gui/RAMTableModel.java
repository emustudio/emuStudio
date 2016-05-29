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
package net.sf.emustudio.ram.memory.gui;

import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

import javax.swing.table.AbstractTableModel;

class RAMTableModel extends AbstractTableModel {
    private final RAMMemoryContext memory;

    RAMTableModel(final RAMMemoryContext memory) {
        this.memory = memory;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return memory.getSize();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            String label = memory.getLabel(rowIndex);
            if (label != null) {
                return String.valueOf(rowIndex) + " (" + label + ")";
            } else {
                return String.valueOf(rowIndex);
            }
        } else {
            RAMInstruction i = (RAMInstruction) memory.read(rowIndex);
            return i.getCodeStr() + " " + i.getOperandStr();
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int col) {
        return (col == 0) ? "Addr" : "Instruction";
    }
    
}
