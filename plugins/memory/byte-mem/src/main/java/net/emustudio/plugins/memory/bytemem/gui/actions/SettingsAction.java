/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.SettingsDialog;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class SettingsAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/preferences-system.png";
    private final Dialogs dialogs;
    private final JDialog parent;
    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final MemoryTable table;
    private final PluginSettings settings;

    public SettingsAction(Dialogs dialogs, JDialog parent, MemoryImpl memory, MemoryContextImpl context,
                          MemoryTable table, PluginSettings settings) {
        super("Erase memory", loadIcon(ICON_FILE));
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
