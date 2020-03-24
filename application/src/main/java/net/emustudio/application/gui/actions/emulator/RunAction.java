package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

public class RunAction extends AbstractAction {

    private final EmulationController emulationController;
    private final JTable debugTable;

    public RunAction(EmulationController emulationController, JTable debugTable) {
        super("Run", new ImageIcon(RunAction.class.getResource("/net/emustudio/application/gui/dialogs/go-play.png")));

        this.emulationController = emulationController;
        this.debugTable = Objects.requireNonNull(debugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            debugTable.setEnabled(false);
            c.start();
        });
    }
}
