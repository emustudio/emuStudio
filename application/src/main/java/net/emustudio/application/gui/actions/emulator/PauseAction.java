/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class PauseAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/go-pause.png";

    private final EmulationController emulationController;
    private final Runnable updateStatus;

    public PauseAction(EmulationController emulationController, Runnable updateStatus) {
        super("Pause", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Pause emulation");
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
