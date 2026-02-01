/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class ResetAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/reset.png";

    private final EmulationController emulationController;

    public ResetAction(EmulationController emulationController) {
        super("Reset", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Reset emulation");
        this.emulationController = emulationController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::reset);
    }
}
