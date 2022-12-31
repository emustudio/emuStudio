/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

public class ShowMemoryAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;

    public ShowMemoryAction(JFrame parent, VirtualComputer computer, Dialogs dialogs) {
        super("Show memory...", new ImageIcon(ShowMemoryAction.class.getResource("/net/emustudio/application/gui/dialogs/grid_memory.gif")));

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
