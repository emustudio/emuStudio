/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class SaveFileAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/document-save.png";

    private final Editor editor;
    private final Runnable updateTitle;

    public SaveFileAction(Editor editor, Runnable updateTitle) {
        super("Save", loadIcon(ICON_FILE));

        this.editor = Objects.requireNonNull(editor);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Save file");
        putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (editor.saveFile()) {
            updateTitle.run();
        }
    }
}
