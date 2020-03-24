package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class StepAction extends AbstractAction {

    private final EmulationController emulationController;

    public StepAction(EmulationController emulationController) {
        super("Step", new ImageIcon(StepAction.class.getResource("/net/emustudio/application/gui/dialogs/go-next.png")));

        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::step);
    }
}
