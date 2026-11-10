/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.gui.debugger.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.ui.StandardIcons.ICON_STEP_BACK;
import static net.emustudio.application.gui.framework.EmuStudioGui.loadIcon;

public class StepBackAction extends AbstractAction {

    private final VirtualComputer computer;
    private final DebugTableModel debugTableModel;
    private final Runnable refreshDebugTable;

    public StepBackAction(VirtualComputer computer, DebugTableModel debugTableModel, Runnable refreshDebugTable) {
        super("Move PC Back", loadIcon(ICON_STEP_BACK));
        putValue(SHORT_DESCRIPTION, "Move program counter backward (registers and memory remain unchanged)");
        this.computer = Objects.requireNonNull(computer);
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            int pc = computer.getExecution().orElseThrow().cpuContext().getInstructionLocation();
            if (pc > 0) {
                int address = debugTableModel.guessPreviousInstructionLocation();
                computer.getExecution().orElseThrow().mutate(() -> cpu.setInstructionLocation(address))
                        .thenRun(() -> SwingUtilities.invokeLater(refreshDebugTable));
            }
        });
    }
}
