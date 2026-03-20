/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.dialogs.ViewComputerDialog;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class ViewComputerAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final AppSettings appSettings;
    private final GUI gui;

    public ViewComputerAction(JFrame parent, VirtualComputer computer, Dialogs dialogs, AppSettings appSettings, GUI gui) {
        super("View computer...");

        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.appSettings = Objects.requireNonNull(appSettings);
        this.gui = Objects.requireNonNull(gui);

        putValue(MNEMONIC_KEY, KeyEvent.VK_V);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        new ViewComputerDialog(parent, computer, appSettings, dialogs, gui).setVisible(true);
    }
}
