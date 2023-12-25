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
package net.emustudio.plugins.memory.ssem.gui.table;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.FONT_MONOSPACED;
import static net.emustudio.plugins.memory.ssem.gui.Constants.COLUMN_WIDTH;

public class MemoryTable extends JTable {

    public MemoryTable(MemoryTableModel tableModel, JScrollPane pm) {
        setModel(tableModel);
        setFont(FONT_MONOSPACED);
        setCellSelectionEnabled(true);
        setFocusCycleRoot(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setFont(FONT_MONOSPACED);
        setDefaultRenderer(Object.class, new MemoryCellRenderer(getTableHeader(), tableModel, pm, getRowHeight()));
        setOpaque(true);

        MemoryCellEditor ed = new MemoryCellEditor();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            TableColumn col = super.getColumnModel().getColumn(i);
            col.setPreferredWidth(COLUMN_WIDTH[i]);
            col.setCellEditor(ed);
        }

        InputMap im = getInputMap(JTable.WHEN_FOCUSED);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent listener) {
                int row = getSelectedRow();
                int col = getSelectedColumn();

                if (row != -1 && col != -1) {
                    tableModel.setValueAt("0", row, col);
                }
            }
        });
    }
}

