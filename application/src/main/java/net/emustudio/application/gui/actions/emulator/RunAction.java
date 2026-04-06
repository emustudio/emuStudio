/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_RUN;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class RunAction extends AbstractAction {

    private final EmulationController emulationController;
    private final JTable debugTable;

    public RunAction(EmulationController emulationController, JTable debugTable) {
        super("Run", loadIcon(ICON_RUN));
        putValue(SHORT_DESCRIPTION, "Run emulation");
        this.emulationController = emulationController;
        this.debugTable = Objects.requireNonNull(debugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            debugTable.setEnabled(false);
            c.start();
        });
    }
}
