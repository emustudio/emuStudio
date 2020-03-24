package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.dialogs.AboutDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class AboutAction extends AbstractAction {
    private final JFrame parent;

    public AboutAction(JFrame parent) {
        super("About...");

        this.parent = Objects.requireNonNull(parent);

        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        new AboutDialog(parent).setVisible(true);
    }
}
