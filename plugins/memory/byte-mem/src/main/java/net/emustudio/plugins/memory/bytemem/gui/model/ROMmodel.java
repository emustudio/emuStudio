/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext.AddressRange;

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
