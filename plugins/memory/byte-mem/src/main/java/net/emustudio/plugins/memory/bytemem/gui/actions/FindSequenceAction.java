/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.application.ui.GUI.loadIcon;

public class FindSequenceAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/icons/find.png";
    private final Runnable showSearch;

    public FindSequenceAction(Runnable showSearch) {
        super("Find sequence", loadIcon(ICON_FILE, FindSequenceAction.class));
        this.showSearch = Objects.requireNonNull(showSearch);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Find sequence (Ctrl+F)");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        showSearch.run();
    }
}
