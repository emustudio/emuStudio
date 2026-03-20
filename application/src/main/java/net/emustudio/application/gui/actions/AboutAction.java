/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.dialogs.AboutDialog;
import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_FAVICON;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class AboutAction extends AbstractAction {
    private final JFrame parent;
    private final GUI gui;

    public AboutAction(JFrame parent, GUI gui) {
        super("About...", loadIcon(ICON_FAVICON));
        this.parent = Objects.requireNonNull(parent);
        this.gui = Objects.requireNonNull(gui);
        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        new AboutDialog(parent, gui).setVisible(true);
    }
}
