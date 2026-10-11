/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer.gui;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;

/**
 * Settings dialog for configuring tape player options.
 */
public class SettingsDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);
    public static final String SETTINGS_KEY_EVENTS = "automationEvents";
    public static final String SETTINGS_KEY_SHOW_GUI_AT_STARTUP = "showGuiAtStartup";

    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final JCheckBox chkShowGuiAtStartup = new JCheckBox("Show GUI at startup");

    public SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "Audio Tape Player Settings", true);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);

        chkShowGuiAtStartup.setSelected(settings.getBoolean(SETTINGS_KEY_SHOW_GUI_AT_STARTUP, false));
        buildContent();
    }

    private void saveSettings() {
        try {
            settings.setBoolean(SETTINGS_KEY_SHOW_GUI_AT_STARTUP, chkShowGuiAtStartup.isSelected());
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not save settings", e);
            dialogs.showError("Could not save settings. Please see log file for details.", "Save settings");
        }
    }

    @Override
    protected JComponent initializeComponents() {
        JButton btnSave = gui.buttonMakePrimary(gui.button("Save", () -> {
            saveSettings();
            dispose();
        }));
        getRootPane().setDefaultButton(btnSave);

        JPanel content = gui.panel("insets dialog", "[grow]", "[][]");
        content.add(chkShowGuiAtStartup, "wrap");
        content.add(btnSave, "align right, gaptop 12");
        return content;
    }
}
