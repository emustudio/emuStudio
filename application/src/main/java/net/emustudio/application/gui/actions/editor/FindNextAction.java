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
