/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.memory.bytemem.gui.FindSequenceDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Supplier;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class FindSequenceAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/edit-find.png";
    private final Dialogs dialogs;
    private final MemorySearch search;
    private final Supplier<Integer> getCurrentAddress;
    private final JDialog parent;
    private final GUI gui;

    public FindSequenceAction(Dialogs dialogs, MemorySearch search, Supplier<Integer> getCurrentAddress,
                              JDialog parent, GUI gui) {
        super("Find sequence...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.search = Objects.requireNonNull(search);
        this.getCurrentAddress = Objects.requireNonNull(getCurrentAddress);
        this.parent = Objects.requireNonNull(parent);
        this.gui = gui;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Find sequence...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FindSequenceDialog dialog = new FindSequenceDialog(
                dialogs, parent, getCurrentAddress.get(), search::start, gui
        );
        dialog.setVisible(true);
    }
}
