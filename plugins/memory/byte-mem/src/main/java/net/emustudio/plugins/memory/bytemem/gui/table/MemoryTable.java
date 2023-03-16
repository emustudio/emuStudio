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
package net.emustudio.plugins.memory.bytemem.gui.table;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.Objects;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.MEMORY_CELLS_FONT;

public class MemoryTable extends JTable {
    private final MemoryTableModel tableModel;

    public MemoryTable(MemoryTableModel tableModel, JScrollPane pm) {
        this.tableModel = Objects.requireNonNull(tableModel);

        setModel(this.tableModel);
        setFont(MEMORY_CELLS_FONT);
        setCellSelectionEnabled(true);
        setFocusCycleRoot(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setFont(MEMORY_CELLS_FONT);
        setDefaultRenderer(Object.class, new MemoryCellRenderer(getTableHeader(), tableModel, pm, getRowHeight()));
        setOpaque(true);

        MemoryCellEditor ed = new MemoryCellEditor(tableModel);
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumn col = super.getColumnModel().getColumn(i);
            col.setPreferredWidth(3 * 18);
            col.setCellEditor(ed);
        }
    }

    public MemoryTableModel getTableModel() {
        return tableModel;
    }
}
