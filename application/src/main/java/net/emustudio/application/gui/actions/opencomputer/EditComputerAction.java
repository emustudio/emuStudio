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

import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class EditComputerAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/computer.png";

    private final Dialogs dialogs;
    private final AppSettings appSettings;
    private final Runnable update;
    private final JDialog parent;
    private final JList<ComputerConfig> lstConfig;

    public EditComputerAction(Dialogs dialogs, AppSettings appSettings,
                              Runnable update, JDialog parent, JList<ComputerConfig> lstConfig) {
        super("Edit computer...", loadIcon(ICON_FILE));
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
