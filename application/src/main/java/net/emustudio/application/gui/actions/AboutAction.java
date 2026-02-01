/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.dialogs.AboutDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class AboutAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/favicon16.png";
    private final JFrame parent;

    public AboutAction(JFrame parent) {
        super("About...", loadIcon(ICON_FILE));
        this.parent = Objects.requireNonNull(parent);
        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        new AboutDialog(parent).setVisible(true);
    }
}
