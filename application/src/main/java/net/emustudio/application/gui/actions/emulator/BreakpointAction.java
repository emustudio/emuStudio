/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_BREAKPOINTS;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;


public class BreakpointAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final GUI gui;
    private final Runnable refreshDebugTable;

    public BreakpointAction(JFrame parent, VirtualComputer computer, Dialogs dialogs, Runnable refreshDebugTable, GUI gui) {
        super("Set/unset breakpoint...", loadIcon(ICON_BREAKPOINTS));
        putValue(SHORT_DESCRIPTION, "Set/unset breakpoint to address...");

        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            BreakpointDialog dialog = new BreakpointDialog(parent, dialogs, gui);
            dialog.setVisible(true);
            int address = dialog.getAddress();

            if ((address != -1) && cpu.isBreakpointSupported()) {
                if (dialog.isSet()) {
                    cpu.setBreakpoint(address);
                } else {
                    cpu.unsetBreakpoint(address);
                }
            }
            refreshDebugTable.run();
        });
    }
}
