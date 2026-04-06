/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_JUMP_TO_BEGINNING;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class JumpToBeginningAction extends AbstractAction {

    private final VirtualComputer computer;
    private final Runnable refreshDebugTable;

    public JumpToBeginningAction(VirtualComputer computer, Runnable refreshDebugTable) {
        super("Jump to beginning", loadIcon(ICON_JUMP_TO_BEGINNING));
        putValue(SHORT_DESCRIPTION, "Jump to beginning");
        this.computer = Objects.requireNonNull(computer);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            cpu.setInstructionLocation(0);
            refreshDebugTable.run();
        });
    }
}
