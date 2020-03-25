package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

public class PauseAction extends AbstractAction {

    private final EmulationController emulationController;
    private final Runnable updateStatus;

    public PauseAction(EmulationController emulationController, Runnable updateStatus) {
        super("Pause", new ImageIcon(PauseAction.class.getResource("/net/emustudio/application/gui/dialogs/go-pause.png")));
        this.emulationController = emulationController;
        this.updateStatus = Objects.requireNonNull(updateStatus);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(e -> {
            boolean timedRunning = e.isTimedRunning();
            e.pause();
            if (timedRunning) {
                updateStatus.run();
            }
        });
    }
}
