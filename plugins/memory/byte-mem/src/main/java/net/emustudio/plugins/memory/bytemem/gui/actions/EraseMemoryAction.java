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
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

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
