/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.memory.standard.gui.model;

import net.emustudio.plugins.memory.standard.StandardMemoryContext.AddressRange;
import net.emustudio.plugins.memory.standard.impl.MemoryContextImpl;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class ROMmodel extends AbstractTableModel {
    private final MemoryContextImpl memory;

    public ROMmodel(MemoryContextImpl memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public int getRowCount() {
        return memory.getReadOnly().size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "From (hex)";
        } else {
            return "To (hex)";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int r, int c) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        AddressRange range = memory.getReadOnly().get(rowIndex);

        if (columnIndex == 0) {
            return String.format("0x%04X", range.getStartAddress());
        } else {
            return String.format("0x%04X", range.getStopAddress());
        }
    }

}
