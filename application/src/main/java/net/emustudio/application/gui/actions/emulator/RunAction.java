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

import net.emustudio.application.emulation.EmulationController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class RunAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/go-play.png";

    private final EmulationController emulationController;
    private final JTable debugTable;

    public RunAction(EmulationController emulationController, JTable debugTable) {
        super("Run", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, "Run emulation");
        this.emulationController = emulationController;
        this.debugTable = Objects.requireNonNull(debugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            debugTable.setEnabled(false);
            c.start();
        });
    }
}
