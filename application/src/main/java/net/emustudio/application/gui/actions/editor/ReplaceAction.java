/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class ReplaceAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/edit-find-replace.png";

    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public ReplaceAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Replace...", loadIcon(ICON_FILE));
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SHORT_DESCRIPTION, "Find/replace text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (findDialog.isVisible()) {
            findDialog.setVisible(false);
        }
        replaceDialog.setVisible(true);
    }
}
