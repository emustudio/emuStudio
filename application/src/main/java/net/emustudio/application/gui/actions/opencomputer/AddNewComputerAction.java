/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.helpers.Unchecked;
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
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class AddNewComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(AddNewComputerAction.class);
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/list-add.png";

    private final Dialogs dialogs;
    private final AppSettings appSettings;
    private final Runnable update;
    private final JDialog parent;

    public AddNewComputerAction(Dialogs dialogs, AppSettings appSettings, Runnable update, JDialog parent) {
        super("Create new computer...", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));

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
