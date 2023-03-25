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
        putValue(SHORT_DESCRIPTION, "Run \"timed\" emulation");
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
