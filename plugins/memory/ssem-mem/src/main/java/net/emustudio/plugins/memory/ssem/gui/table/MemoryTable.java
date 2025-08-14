/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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

