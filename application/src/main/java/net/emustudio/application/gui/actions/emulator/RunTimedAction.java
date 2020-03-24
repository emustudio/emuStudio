package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RunTimedAction extends AbstractAction {

    private final EmulationController emulationController;
    private final Dialogs dialogs;

    public RunTimedAction(EmulationController emulationController, Dialogs dialogs) {
        super("Run timed...", new ImageIcon(RunTimedAction.class.getResource("/net/emustudio/application/gui/dialogs/go-play-time.png")));

        this.emulationController = emulationController;
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            try {
                dialogs
                    .readInteger("Enter time slice in milliseconds:", "Timed emulation", 500)
                    .ifPresent(sliceMillis -> c.step(sliceMillis, TimeUnit.MILLISECONDS));
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid number format", "Timed emulation");
            }
        });
    }
}
