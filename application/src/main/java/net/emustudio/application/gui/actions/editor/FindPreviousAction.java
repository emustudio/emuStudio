package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class FindPreviousAction extends AbstractAction  {

    private final Editor editor;
    private final Dialogs dialogs;
    private final Action findAction;

    public FindPreviousAction(Editor editor, Dialogs dialogs, Action findAction) {
        super("Find previous");
        this.editor = Objects.requireNonNull(editor);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.findAction = Objects.requireNonNull(findAction);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        editor.findPrevious().ifPresentOrElse(found -> {
            if (!found) {
                dialogs.showInfo("Text was not found", "Find previous");
            }
        }, () -> findAction.actionPerformed(actionEvent));
    }
}
