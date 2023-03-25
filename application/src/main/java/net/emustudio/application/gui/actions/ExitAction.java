/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
