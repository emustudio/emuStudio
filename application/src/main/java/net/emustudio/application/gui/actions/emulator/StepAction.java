/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_STEP;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class StepAction extends AbstractAction {

    private final EmulationController emulationController;

    public StepAction(EmulationController emulationController) {
        super("Step", loadIcon(ICON_STEP));
        putValue(SHORT_DESCRIPTION, "Step forward");
        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::step);
    }
}
