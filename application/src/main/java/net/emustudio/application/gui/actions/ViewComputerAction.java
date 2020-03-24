package net.emustudio.application.gui.actions;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.gui.dialogs.ViewComputerDialog;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class ViewComputerAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final ApplicationConfig applicationConfig;

    public ViewComputerAction(JFrame parent, VirtualComputer computer, Dialogs dialogs, ApplicationConfig applicationConfig) {
        super("View computer...");

        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);

        putValue(MNEMONIC_KEY, KeyEvent.VK_V);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        new ViewComputerDialog(parent, computer, applicationConfig, dialogs).setVisible(true);
    }
}
