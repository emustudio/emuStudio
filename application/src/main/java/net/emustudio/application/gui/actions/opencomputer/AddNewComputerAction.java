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

import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.internal.Unchecked;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.settings.ConfigFiles.createConfiguration;
import static net.emustudio.application.settings.ConfigFiles.loadConfiguration;

public class AddNewComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(AddNewComputerAction.class);

    private final Dialogs dialogs;
    private final AppSettings appSettings;
    private final Runnable update;
    private final JDialog parent;

    public AddNewComputerAction(Dialogs dialogs, AppSettings appSettings, Runnable update, JDialog parent) {
        super(
            "Create new computer...", new ImageIcon(AddNewComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/list-add.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.appSettings = Objects.requireNonNull(appSettings);
        this.update = Objects.requireNonNull(update);
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional<String> computerName = dialogs.readString("Enter computer name:", "Create new computer");
        computerName.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                dialogs.showError("Computer name must be non-empty", "Create new computer");
            } else {
                try {
                    loadConfiguration(name)
                        .ifPresentOrElse(
                            c -> dialogs.showError("Computer '" + name + "' already exists, choose another name."),
                            () -> {
                                ComputerConfig newComputer = Unchecked.call(() -> createConfiguration(name));
                                Schema schema = new Schema(newComputer, appSettings);
                                SchemaEditorDialog di = new SchemaEditorDialog(parent, schema, dialogs);
                                di.setVisible(true);
                                update.run();
                            }
                        );
                } catch (IOException ex) {
                    LOGGER.error("Could not load computer with name '" + name + "'", ex);
                    dialogs.showError("Could not load computer with name '" + name + "'. Please see log file for details.");
                }
            }
        });
    }
}
