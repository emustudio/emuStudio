/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_GRID;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class ShowMemoryAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;

    public ShowMemoryAction(JFrame parent, VirtualComputer computer, Dialogs dialogs) {
        super("Show memory...", loadIcon(ICON_GRID));
        putValue(SHORT_DESCRIPTION, "Show operating memory");
        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getMemory()
                .filter(Memory::isShowSettingsSupported)
                .ifPresentOrElse(
                        p -> p.showSettings(parent),
                        () -> dialogs.showInfo("Memory GUI is not supported", "Show Memory")
                );
    }
}
