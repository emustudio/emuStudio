/*
 * This file is part of emuStudio.
 *
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
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMLabel;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

class RAMTableModel extends AbstractTableModel {
    private final RAMMemoryContext memory;

    RAMTableModel(RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return memory.getSize();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.valueOf(rowIndex);
            case 1:
                return memory.getLabel(rowIndex).map(RAMLabel::getLabel).orElse("");
            case 2:
                RAMInstruction i = memory.read(rowIndex);
                return i.getOpcode() + " " + i.getOperand().map(RAMValue::getStringRepresentation).orElse("");
        }
        return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Address";
            case 1:
                return "Label";
            case 2:
                return "Instruction";
        }
        return "";
    }

}
