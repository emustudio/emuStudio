/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_RENAME;
import static net.emustudio.application.settings.ConfigFiles.renameConfiguration;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class RenameComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(RenameComputerAction.class);

    private final Dialogs dialogs;
    private final Runnable update;
    private final JList<ComputerConfig> lstConfig;

    public RenameComputerAction(Dialogs dialogs, Runnable update,
                                JList<ComputerConfig> lstConfig) {
        super("Rename computer...", loadIcon(ICON_RENAME));
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
