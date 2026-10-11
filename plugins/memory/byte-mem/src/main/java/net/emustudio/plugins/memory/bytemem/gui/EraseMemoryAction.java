/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * Swing action which erases (clears) the memory context and refreshes its table model.
 */
public class EraseMemoryAction extends AbstractAction {
    private final AbstractTableModel tableModel;
    private final MemoryContext<?> context;

    public EraseMemoryAction(AbstractTableModel tableModel, MemoryContext<?> context, Icon icon) {
        super("Erase memory", icon);
        this.tableModel = Objects.requireNonNull(tableModel);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        context.clear();
        tableModel.fireTableDataChanged();
    }
}
