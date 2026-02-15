/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.gui.schema.SchemaPreviewPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_SAVE;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class SaveSchemaAction extends AbstractAction {
    private final SchemaPreviewPanel preview;

    public SaveSchemaAction(SchemaPreviewPanel preview) {
        super("Save schema image...", loadIcon(ICON_SAVE));
        putValue(SHORT_DESCRIPTION, getValue(Action.NAME));
        this.preview = Objects.requireNonNull(preview);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        preview.saveSchemaImage();
    }
}
