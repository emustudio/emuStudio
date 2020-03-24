package net.emustudio.application.gui.actions.editor;

import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class ReplaceAction extends AbstractAction {
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public ReplaceAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Replace...", new ImageIcon(ReplaceAction.class.getResource("/net/emustudio/application/gui/dialogs/edit-find-replace.png")));
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SHORT_DESCRIPTION, "Replace text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (findDialog.isVisible()) {
            findDialog.setVisible(false);
        }
        replaceDialog.setVisible(true);
    }
}
