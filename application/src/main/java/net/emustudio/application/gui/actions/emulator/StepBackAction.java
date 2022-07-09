/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class StepBackAction extends AbstractAction {

    private final VirtualComputer computer;
    private final DebugTableModel debugTableModel;
    private final Runnable refreshDebugTable;

    public StepBackAction(VirtualComputer computer, DebugTableModel debugTableModel, Runnable refreshDebugTable) {
        super("Step Back", new ImageIcon(StepBackAction.class.getResource("/net/emustudio/application/gui/dialogs/go-previous.png")));

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
