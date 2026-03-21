/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class SettingsDialog extends DialogBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final TapeGui tapeGui;
    private final JCheckBox chkAlwaysOnTop = new JCheckBox("Always on top");
    private final JCheckBox chkShowAtStartup = new JCheckBox("Show GUI at startup");

    public SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, TapeGui tapeGui, String title, GUI gui) {
        super(parent, title + " settings", true);
        this.gui = Objects.requireNonNull(gui);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.tapeGui = tapeGui;

        setResizable(false);

        chkAlwaysOnTop.setSelected(settings.getBoolean("alwaysOnTop", false));
        chkShowAtStartup.setSelected(settings.getBoolean("showAtStartup", false));

        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        chkAlwaysOnTop.addActionListener(e -> {
            if (tapeGui != null) {
                tapeGui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
            }
        });

        JButton btnSave = new JButton("Save");
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.addActionListener(e -> {
            try {
                settings.setBoolean("alwaysOnTop", chkAlwaysOnTop.isSelected());
                settings.setBoolean("showAtStartup", chkShowAtStartup.isSelected());
            } catch (CannotUpdateSettingException ex) {
                LOGGER.error("Could not save abstract tape settings", ex);
                dialogs.showError("Could not save abstract tape settings. Please see log file for more details.", "Save settings");
            }
            dispose();
        });

        JPanel content = gui.panel("insets dialog", "[grow]", "[][][grow][]");
        content.add(chkAlwaysOnTop, "wrap");
        content.add(chkShowAtStartup, "wrap");
        content.add(new JPanel(), "grow, wrap");
        content.add(btnSave, "align right");

        return content;
    }
}
