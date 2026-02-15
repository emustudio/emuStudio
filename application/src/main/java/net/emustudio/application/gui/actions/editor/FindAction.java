/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_FIND;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class FindAction extends AbstractAction {
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public FindAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Find...", loadIcon(ICON_FIND));
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SHORT_DESCRIPTION, "Find text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (replaceDialog.isVisible()) {
            replaceDialog.setVisible(false);
        }
        findDialog.setVisible(true);
    }
}
