/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.brainduck.gui;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class MemoryTableModel extends AbstractTableModel {
    private final Byte[] memory;
    private volatile int P;

    MemoryTableModel(Byte[] memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        int index = P + (columnIndex - 2);
        if (index >= 0) {
            return String.format("%02Xh", index);
        }
        return "N/A";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int index = P + (columnIndex - 2);
        if (index >= 0) {
            return String.format("%02Xh", memory[index]);
        }
        return "";
    }

    void setP(int P) {
        this.P = P;
        fireTableDataChanged();
    }
}
