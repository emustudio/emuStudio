package net.emustudio.application.gui.actions.editor;

import org.fife.rsta.ui.search.FindDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import org.fife.rsta.ui.search.ReplaceDialog;

public class FindAction extends AbstractAction {
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public FindAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Find...", new ImageIcon(FindAction.class.getResource("/net/emustudio/application/gui/dialogs/edit-find.png")));
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SHORT_DESCRIPTION, "Find text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (replaceDialog.isVisible()) {
            replaceDialog.setVisible(false);
        }
        findDialog.setVisible(true);
    }
}
