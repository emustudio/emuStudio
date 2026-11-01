/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.StandardIcons.ICON_BREAKPOINTS;
import static net.emustudio.application.gui.framework.EmuStudioGui.loadIcon;


public class BreakpointAction extends AbstractAction {

    private final Runnable showDialog;

    public BreakpointAction(Runnable showDialog) {
        super("Set breakpoint...", loadIcon(ICON_BREAKPOINTS));
        putValue(SHORT_DESCRIPTION, "Set breakpoint / inspect address...");

        this.showDialog = Objects.requireNonNull(showDialog);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        showDialog.run();
    }
}
