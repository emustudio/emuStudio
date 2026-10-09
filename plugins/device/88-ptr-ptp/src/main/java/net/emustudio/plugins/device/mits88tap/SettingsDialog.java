/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;

final class SettingsDialog extends DialogBase {
    static final String SHOW_GUI_AT_STARTUP = "showGuiAtStartup";
    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final JCheckBox showGuiAtStartup = new JCheckBox("Show GUI at startup");

    SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "Altair PTR/PTP - Settings", true);
        this.settings = settings;
        this.dialogs = dialogs;
        this.gui = gui;
        showGuiAtStartup.setSelected(settings.getBoolean(SHOW_GUI_AT_STARTUP, false));
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(showGuiAtStartup, "wrap");
        content.add(gui.label("CPU ports: 12h status, 13h data."), "gaptop 12, wrap");
        JButton save = gui.button("Save");
        gui.buttonMakePrimary(save);
        getRootPane().setDefaultButton(save);
        save.addActionListener(event -> {
            try {
                settings.setBoolean(SHOW_GUI_AT_STARTUP, showGuiAtStartup.isSelected());
                dispose();
            } catch (CannotUpdateSettingException e) {
                dialogs.showError(e.getMessage(), "Save settings");
            }
        });
        content.add(save, "align right, gaptop 12");
        return content;
    }
}
