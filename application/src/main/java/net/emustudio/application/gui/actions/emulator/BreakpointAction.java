/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class BreakpointAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/breakpoints.png";

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Runnable refreshDebugTable;

    public BreakpointAction(JFrame parent, VirtualComputer computer, Dialogs dialogs, Runnable refreshDebugTable) {
        super("Set/unset breakpoint...", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Set/unset breakpoint to address...");

        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            BreakpointDialog dialog = new BreakpointDialog(parent, dialogs);
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
