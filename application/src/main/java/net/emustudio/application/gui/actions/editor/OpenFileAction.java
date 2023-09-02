/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Supplier;

import static net.emustudio.application.gui.GuiUtils.loadIcon;

public class OpenFileAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/document-open.png";

    private final Supplier<Boolean> confirmSave;
    private final Editor editor;
    private final JTextArea compilerOutput;
    private final Runnable updateTitle;

    public OpenFileAction(Supplier<Boolean> confirmSave, Editor editor, JTextArea compilerOutput, Runnable updateTitle) {
        super("Open...", loadIcon(ICON_FILE));

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
