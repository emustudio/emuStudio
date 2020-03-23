package net.emustudio.application.gui.actions;

import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class ShowReplaceDialogAction extends AbstractAction {
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public ShowReplaceDialogAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Replace...");
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SMALL_ICON, null);
        putValue(SHORT_DESCRIPTION, "Replace text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, "Replace");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (findDialog.isVisible()) {
            findDialog.setVisible(false);
        }
        replaceDialog.setVisible(true);
    }
}
