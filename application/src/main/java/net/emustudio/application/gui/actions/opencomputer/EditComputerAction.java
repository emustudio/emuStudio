/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_COMPUTER;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class EditComputerAction extends AbstractAction {

    private final Dialogs dialogs;
    private final AppSettings appSettings;
    private final Runnable update;
    private final JDialog parent;
    private final JList<ComputerConfig> lstConfig;

    public EditComputerAction(Dialogs dialogs, AppSettings appSettings,
                              Runnable update, JDialog parent, JList<ComputerConfig> lstConfig) {
        super("Edit computer...", loadIcon(ICON_COMPUTER));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));
        this.dialogs = Objects.requireNonNull(dialogs);
        this.appSettings = Objects.requireNonNull(appSettings);
        this.update = Objects.requireNonNull(update);
        this.parent = Objects.requireNonNull(parent);
        this.lstConfig = Objects.requireNonNull(lstConfig);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
                .ofNullable(lstConfig.getSelectedValue())
                .ifPresentOrElse(computer -> {
                    Schema schema = new Schema(computer, appSettings);
                    new SchemaEditorDialog(parent, schema, dialogs).setVisible(true);
                    update.run();
                }, () -> dialogs.showError("A computer has to be selected!", "Edit computer"));
    }
}
