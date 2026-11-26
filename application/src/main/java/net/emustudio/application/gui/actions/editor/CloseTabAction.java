/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.TabbedEditor;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class CloseTabAction extends AbstractAction {
    public static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(
            RTextArea.isOSX() ? KeyEvent.VK_W : KeyEvent.VK_F4, RTextArea.getDefaultModifier());

    private final TabbedEditor editor;
    private final Runnable updateTitle;

    public CloseTabAction(TabbedEditor editor, Runnable updateTitle) {
        super("Close tab");
        this.editor = Objects.requireNonNull(editor);
        this.updateTitle = Objects.requireNonNull(updateTitle);
        putValue(ACCELERATOR_KEY, SHORTCUT);
        putValue(SHORT_DESCRIPTION, "Close current editor tab");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editor.closeCurrent()) {
            updateTitle.run();
        }
    }
}
