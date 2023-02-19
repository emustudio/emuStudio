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

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.SettingsDialog;
import net.emustudio.plugins.memory.bytemem.gui.model.TableMemory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class SettingsAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/preferences-system.png";
    private final Dialogs dialogs;
    private final JDialog parent;
    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final TableMemory table;
    private final PluginSettings settings;

    public SettingsAction(Dialogs dialogs, JDialog parent, MemoryImpl memory, MemoryContextImpl context,
                          TableMemory table, PluginSettings settings) {
        super("Erase memory", new ImageIcon(SettingsAction.class.getResource(ICON_FILE)));
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
        this.table = Objects.requireNonNull(table);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SettingsDialog(parent, memory, context, table, settings, dialogs).setVisible(true);
    }
}
