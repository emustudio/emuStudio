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
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class OpenComputerAction extends AbstractAction {
    private final Dialogs dialogs;
    private final JDialog parent;
    private final JList<ComputerConfig> lstConfig;
    private final Consumer<ComputerConfig> selectComputer;

    public OpenComputerAction(Dialogs dialogs, JDialog parent, JList<ComputerConfig> lstConfig,
                              Consumer<ComputerConfig> selectComputer) {
        super(
            "Create new computer...", new ImageIcon(OpenComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/list-add.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.parent = Objects.requireNonNull(parent);
        this.lstConfig = Objects.requireNonNull(lstConfig);
        this.selectComputer = Objects.requireNonNull(selectComputer);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
            .ofNullable(lstConfig.getSelectedValue())
            .ifPresentOrElse(computer -> {
                selectComputer.accept(computer);
                parent.dispose();
            }, () -> dialogs.showError("A computer has to be selected!", "Open computer"));
    }
}
