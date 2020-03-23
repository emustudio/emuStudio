package net.emustudio.application.gui.actions;

import org.fife.rsta.ui.search.FindDialog;

import javax.swing.*;
import javax.swing.text.TextAction;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import org.fife.rsta.ui.search.ReplaceDialog;

public class ShowFindDialogAction extends TextAction {
    private final FindDialog findDialog;
    private final ReplaceDialog replaceDialog;

    public ShowFindDialogAction(FindDialog findDialog, ReplaceDialog replaceDialog) {
        super("Find...");
        this.findDialog = Objects.requireNonNull(findDialog);
        this.replaceDialog = Objects.requireNonNull(replaceDialog);

        putValue(SHORT_DESCRIPTION, "Find text...");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (replaceDialog.isVisible()) {
            replaceDialog.setVisible(false);
        }
        findDialog.setVisible(true);
    }
}
