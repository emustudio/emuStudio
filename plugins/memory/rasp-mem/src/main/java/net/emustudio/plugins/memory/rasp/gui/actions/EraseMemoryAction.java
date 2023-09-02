/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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
package net.emustudio.plugins.memory.rasp.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.memory.rasp.gui.RaspTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.plugins.memory.rasp.gui.Constants.loadIcon;

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
