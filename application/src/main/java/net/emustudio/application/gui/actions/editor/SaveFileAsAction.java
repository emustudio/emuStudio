package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class SaveFileAsAction extends AbstractAction {
    private final Editor editor;
    private final Runnable updateTitle;

    public SaveFileAsAction(Editor editor, Runnable updateTitle) {
        super("Save As...");

        this.editor = Objects.requireNonNull(editor);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(SHORT_DESCRIPTION, "Save file as...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (editor.saveFileAs()) {
            updateTitle.run();
        }
    }
}
