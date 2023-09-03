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
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.settings.ConfigFiles.renameConfiguration;
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class RenameComputerAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/rename-computer.png";
    private final static Logger LOGGER = LoggerFactory.getLogger(RenameComputerAction.class);

    private final Dialogs dialogs;
    private final Runnable update;
    private final JList<ComputerConfig> lstConfig;

    public RenameComputerAction(Dialogs dialogs, Runnable update,
                                JList<ComputerConfig> lstConfig) {
        super("Rename computer...", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));
        this.dialogs = Objects.requireNonNull(dialogs);
        this.update = Objects.requireNonNull(update);
        this.lstConfig = Objects.requireNonNull(lstConfig);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
                .ofNullable(lstConfig.getSelectedValue())
                .ifPresentOrElse(computer -> dialogs
                        .readString("Enter new computer name:", "Rename computer")
                        .ifPresent(newName -> {
                            if (newName.trim().isEmpty()) {
                                dialogs.showError("Computer name must be non-empty", "Rename computer");
                            } else {
                                try {
                                    renameConfiguration(computer, newName);
                                    update.run();
                                } catch (CannotUpdateSettingException | IOException ex) {
                                    LOGGER.error("Could not rename computer", ex);
                                    dialogs.showError("Computer could not be renamed. Please see log file for details.");
                                }
                            }
                        }), () -> dialogs.showError("A computer has to be selected!", "Rename computer"));
    }
}
