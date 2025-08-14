/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
