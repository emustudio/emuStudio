/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class StepBackAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/go-previous.png";

    private final VirtualComputer computer;
    private final DebugTableModel debugTableModel;
    private final Runnable refreshDebugTable;

    public StepBackAction(VirtualComputer computer, DebugTableModel debugTableModel, Runnable refreshDebugTable) {
        super("Step Back", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Step back");
        this.computer = Objects.requireNonNull(computer);
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            int pc = cpu.getInstructionLocation();
            if (pc > 0) {
                cpu.setInstructionLocation(debugTableModel.guessPreviousInstructionLocation());
                refreshDebugTable.run();
            }
        });
    }
}
