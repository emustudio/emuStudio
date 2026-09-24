/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.TabbedEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class CloseTabAction extends AbstractAction {
    private final TabbedEditor editor;
    private final Runnable updateTitle;

    public CloseTabAction(TabbedEditor editor, Runnable updateTitle) {
        super("Close tab");
        this.editor = Objects.requireNonNull(editor);
        this.updateTitle = Objects.requireNonNull(updateTitle);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Close current editor tab");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editor.closeCurrent()) {
            updateTitle.run();
        }
    }
}
