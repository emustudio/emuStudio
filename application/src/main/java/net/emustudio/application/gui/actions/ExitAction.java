package net.emustudio.application.gui.actions;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ExitAction extends AbstractAction {

    private final Supplier<Boolean> confirmSave;
    private final EmulationController emulationController;
    private final VirtualComputer computer;
    private final Runnable dispose;

    public ExitAction(Supplier<Boolean> confirmSave, EmulationController emulationController, VirtualComputer computer,
                      Runnable dispose) {
        super("Exit");

        this.confirmSave = Objects.requireNonNull(confirmSave);
        this.emulationController = emulationController;
        this.computer = Objects.requireNonNull(computer);
        this.dispose = Objects.requireNonNull(dispose);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_E);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (confirmSave.get()) {
            Optional.ofNullable(emulationController).ifPresent(EmulationController::close);
            computer.close();
            dispose.run();

            System.exit(0);
        }
    }
}
