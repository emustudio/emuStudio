/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

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
