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
