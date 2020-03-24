package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class StopAction extends AbstractAction {

    private final EmulationController emulationController;

    public StopAction(EmulationController emulationController) {
        super("Stop", new ImageIcon(StopAction.class.getResource("/net/emustudio/application/gui/dialogs/go-stop.png")));
        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::stop);
    }
}
