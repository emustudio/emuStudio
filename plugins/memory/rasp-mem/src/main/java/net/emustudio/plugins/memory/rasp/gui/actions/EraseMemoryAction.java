/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.memory.rasp.gui.RaspTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class EraseMemoryAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/rasp/gui/clear.png";
    private final RaspTableModel tableModel;
    private final MemoryContext<Integer> context;

    public EraseMemoryAction(RaspTableModel tableModel, MemoryContext<Integer> context) {
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
