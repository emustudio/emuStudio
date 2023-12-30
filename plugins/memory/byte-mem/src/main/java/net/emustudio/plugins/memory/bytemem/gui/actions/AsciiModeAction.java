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

import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class AsciiModeAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/ascii-mode.png";
    private final MemoryTableModel tableModel;
    private final JToggleButton btnAsciiMode;

    public AsciiModeAction(MemoryTableModel tableModel, JToggleButton btnAsciiMode) {
        super("Toggle ASCII mode", loadIcon(ICON_FILE));
        this.tableModel = Objects.requireNonNull(tableModel);
        this.btnAsciiMode = Objects.requireNonNull(btnAsciiMode);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tableModel.setAsciiMode(btnAsciiMode.isSelected());
    }
}
