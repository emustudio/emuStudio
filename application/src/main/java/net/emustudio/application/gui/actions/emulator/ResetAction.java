package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class ResetAction extends AbstractAction {

    private final EmulationController emulationController;

    public ResetAction(EmulationController emulationController) {
        super("Reset", new ImageIcon(ResetAction.class.getResource("/net/emustudio/application/gui/dialogs/reset.png")));

        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::reset);
    }
}
