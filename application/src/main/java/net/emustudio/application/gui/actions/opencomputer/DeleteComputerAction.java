/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.settings.ConfigFiles.removeConfiguration;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

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
