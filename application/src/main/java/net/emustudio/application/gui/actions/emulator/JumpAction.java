/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class JumpAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/go-jump.png";

    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Runnable refreshDebugTable;

    public JumpAction(VirtualComputer computer, Dialogs dialogs, Runnable refreshDebugTable) {
        super("Jump...", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Jump to address");
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
                                dialogs.showError("Invalid memory address (please check memory size)");
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
