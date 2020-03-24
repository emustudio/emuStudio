package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Supplier;

public class NewFileAction extends AbstractAction {

    private final Supplier<Boolean> confirmSave;
    private final Editor editor;
    private final JTextArea compilerOutput;
    private final Runnable updateTitle;

    public NewFileAction(Supplier<Boolean> confirmSave, Editor editor, JTextArea compilerOutput, Runnable updateTitle) {
        super("New", new ImageIcon(NewFileAction.class.getResource("/net/emustudio/application/gui/dialogs/document-new.png")));

        this.confirmSave = Objects.requireNonNull(confirmSave);
        this.editor = Objects.requireNonNull(editor);
        this.compilerOutput = Objects.requireNonNull(compilerOutput);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "New file");
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (confirmSave.get()) {
            editor.newFile();
            compilerOutput.setText("");
            updateTitle.run();
        }
    }
}
