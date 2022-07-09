/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class FindNextAction extends AbstractAction {

    private final Editor editor;
    private final Dialogs dialogs;
    private final Action findAction;

    public FindNextAction(Editor editor, Dialogs dialogs, Action findAction) {
        super("Find next");

        this.editor = Objects.requireNonNull(editor);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.findAction = Objects.requireNonNull(findAction);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        editor.findNext().ifPresentOrElse(found -> {
            if (!found) {
                dialogs.showInfo("Text was not found", "Find next");
            }
        }, () -> findAction.actionPerformed(actionEvent));
    }
}
