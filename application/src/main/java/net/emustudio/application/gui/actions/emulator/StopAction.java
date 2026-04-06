/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_STOP;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class StopAction extends AbstractAction {

    private final EmulationController emulationController;

    public StopAction(EmulationController emulationController) {
        super("Stop", loadIcon(ICON_STOP));
        putValue(SHORT_DESCRIPTION, "Stop emulation");
        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::stop);
    }
}
