/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class EraseMemoryAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/edit-clear.png";
    private final MemoryTableModel tableModel;
    private final MemoryContextImpl context;

    public EraseMemoryAction(MemoryTableModel tableModel, MemoryContextImpl context) {
        super("Erase memory", loadIcon(ICON_FILE));
        this.tableModel = Objects.requireNonNull(tableModel);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.clear();
        tableModel.fireTableDataChanged();
    }
}
