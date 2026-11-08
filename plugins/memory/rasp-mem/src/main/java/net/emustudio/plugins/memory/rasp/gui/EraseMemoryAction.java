/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ExecutionState;

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
    private final ExecutionState execution;

    public EraseMemoryAction(AbstractTableModel tableModel, MemoryContext<?> context, Icon icon, ExecutionState execution) {
        super("Erase memory", icon);
        this.tableModel = Objects.requireNonNull(tableModel);
        this.context = Objects.requireNonNull(context);
        this.execution = Objects.requireNonNull(execution);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execution.mutate(context::clear).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
            tableModel.fireTableDataChanged();
            if (failure != null) JOptionPane.showMessageDialog(null, failure.getMessage(), "Erase memory", JOptionPane.ERROR_MESSAGE);
        }));
    }
}
