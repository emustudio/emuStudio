/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.debugger.Debugging;

import javax.swing.*;
import java.awt.Component;

final class WatchHelp {
    private WatchHelp() {}

    static void show(Component parent, EmuStudioGui gui, Debugging debugging) {
        String reference = debugging == null
                ? "This CPU does not support watch expressions."
                : debugging.getExpressionsHelp();
        JTextArea help = new JTextArea(reference, 20, 52);
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setFont(EmuStudioGui.monospaced(help));
        help.setCaretPosition(0);
        help.getAccessibleContext().setAccessibleName("Watch expression reference");
        JOptionPane.showMessageDialog(parent, gui.scrollPane(help), "Watch expression help", JOptionPane.PLAIN_MESSAGE);
    }
}
