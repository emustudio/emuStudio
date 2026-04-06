/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Supplier;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_OPEN_FILE;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class OpenFileAction extends AbstractAction {
    private final Supplier<Boolean> confirmSave;
    private final Editor editor;
    private final JTextArea compilerOutput;
    private final Runnable updateTitle;

    public OpenFileAction(Supplier<Boolean> confirmSave, Editor editor, JTextArea compilerOutput, Runnable updateTitle) {
        super("Open...", loadIcon(ICON_OPEN_FILE));

        this.confirmSave = Objects.requireNonNull(confirmSave);
        this.editor = Objects.requireNonNull(editor);
        this.compilerOutput = Objects.requireNonNull(compilerOutput);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Open file...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (confirmSave.get() && editor.openFile()) {
            compilerOutput.setText("");
            updateTitle.run();
        }
    }
}
