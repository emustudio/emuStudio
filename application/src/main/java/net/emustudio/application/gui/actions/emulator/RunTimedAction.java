/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class RunTimedAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/go-play-time.png";

    private final EmulationController emulationController;
    private final Dialogs dialogs;

    public RunTimedAction(EmulationController emulationController, Dialogs dialogs) {
        super("Run timed...", loadIcon(ICON_FILE));
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
