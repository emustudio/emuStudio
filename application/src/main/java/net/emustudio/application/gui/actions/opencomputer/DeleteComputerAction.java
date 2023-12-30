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
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.settings.ConfigFiles.removeConfiguration;
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class DeleteComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeleteComputerAction.class);
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/list-remove.png";

    private final Dialogs dialogs;
    private final Runnable update;
    private final JList<ComputerConfig> lstConfig;

    public DeleteComputerAction(Dialogs dialogs, Runnable update, JList<ComputerConfig> lstConfig) {
        super("Delete computer", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));
        this.dialogs = Objects.requireNonNull(dialogs);
        this.update = Objects.requireNonNull(update);
        this.lstConfig = Objects.requireNonNull(lstConfig);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
                .ofNullable(lstConfig.getSelectedValue())
                .ifPresentOrElse(computer -> {
                    Dialogs.DialogAnswer answer = dialogs.ask("Do you really want to delete selected computer?", "Delete computer");
                    if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                        try {
                            removeConfiguration(computer.getName());
                            update.run();
                        } catch (IOException ex) {
                            LOGGER.error("Could not remove computer configuration", ex);
                            dialogs.showError("Computer could not be deleted. Please consult log for details.");
                        }
                    }
                }, () -> dialogs.showError("A computer has to be selected!", "Delete computer"));
    }
}
