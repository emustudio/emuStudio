/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.emulib.runtime.interaction.GuiConstants;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.Objects;


public class MemoryTable extends JTable {
    private final MemoryTableModel tableModel;

    public MemoryTable(MemoryTableModel tableModel, JScrollPane pm) {
        this.tableModel = Objects.requireNonNull(tableModel);

        setModel(this.tableModel);
        setFont(GuiConstants.FONT_MONOSPACED);
        setCellSelectionEnabled(true);
        setFocusCycleRoot(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setFont(GuiConstants.FONT_MONOSPACED);
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
