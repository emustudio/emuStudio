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

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class JumpAction extends AbstractAction {

    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Runnable refreshDebugTable;

    public JumpAction(VirtualComputer computer, Dialogs dialogs, Runnable refreshDebugTable) {
        super("Jump...", new ImageIcon(JumpAction.class.getResource("/net/emustudio/application/gui/dialogs/go-jump.png")));

        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresentOrElse(cpu -> {
            try {
                dialogs
                        .readInteger("Memory address:", "Jump to address", 0)
                        .ifPresent(address -> {
                            if (!cpu.setInstructionLocation(address)) {
                                int memorySize = computer.getMemory().map(Memory::getSize).orElse(0);
                                String maxSizeMessage = (memorySize == 0) ? "" : "(probably accepts range from 0 to " + memorySize + ")";
                                dialogs.showError("Invalid memory address" + maxSizeMessage);
                            } else {
                                refreshDebugTable.run();
                            }
                        });
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid address format", "Jump to address");
            }
        }, () -> dialogs.showInfo("CPU is not set", "Jump to address"));
    }
}
