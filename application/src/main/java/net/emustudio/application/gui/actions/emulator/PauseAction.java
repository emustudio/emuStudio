package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class PauseAction extends AbstractAction {

    private final EmulationController emulationController;

    public PauseAction(EmulationController emulationController) {
        super("Pause", new ImageIcon(PauseAction.class.getResource("/net/emustudio/application/gui/dialogs/go-pause.png")));
        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::pause);
    }
}
