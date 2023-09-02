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

import net.emustudio.application.gui.schema.SchemaPreviewPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.application.gui.GuiUtils.loadIcon;

public class SaveSchemaAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/document-save.png";
    private final SchemaPreviewPanel preview;

    public SaveSchemaAction(SchemaPreviewPanel preview) {
        super("Save schema image...", loadIcon(ICON_FILE));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));
        this.preview = Objects.requireNonNull(preview);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        preview.saveSchemaImage();
    }
}
