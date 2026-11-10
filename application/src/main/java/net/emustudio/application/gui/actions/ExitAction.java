/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Window;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ExitAction extends AbstractAction {

    private final Supplier<Boolean> confirmSave;
    private final EmulationController emulationController;
    private final VirtualComputer computer;
    private final Runnable dispose;
    private final Runnable exit;
    private final Window window;

    public ExitAction(Window window, Supplier<Boolean> confirmSave, EmulationController emulationController, VirtualComputer computer,
                      Runnable dispose) {
        this(window, confirmSave, emulationController, computer, dispose, () -> System.exit(0));
    }

    ExitAction(Window window, Supplier<Boolean> confirmSave, EmulationController emulationController, VirtualComputer computer,
               Runnable dispose, Runnable exit) {
        super("Exit");

        this.confirmSave = Objects.requireNonNull(confirmSave);
        this.emulationController = emulationController;
        this.computer = Objects.requireNonNull(computer);
        this.dispose = Objects.requireNonNull(dispose);
        this.exit = Objects.requireNonNull(exit);
        this.window = Objects.requireNonNull(window);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_E);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (!isEnabled()) return;
        if (confirmSave.get()) {
            setEnabled(false);
            window.setEnabled(false);
            CompletableFuture<Void> stopped = emulationController == null
                    ? CompletableFuture.completedFuture(null) : emulationController.closeAsync();
            // Detach views and queued UI callbacks before plugin-owned memory is destroyed.
            stopped.thenRunAsync(dispose, SwingUtilities::invokeLater)
                    .thenRunAsync(computer::close).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                if (failure != null) {
                    setEnabled(true);
                    window.setEnabled(true);
                    JOptionPane.showMessageDialog(window, "Could not close virtual computer: " + failure.getMessage(),
                            "Exit", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                exit.run();
            }));
        }
    }
}
