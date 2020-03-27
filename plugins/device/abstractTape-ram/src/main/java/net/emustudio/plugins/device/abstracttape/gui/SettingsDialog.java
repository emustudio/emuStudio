/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class SettingsDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final TapeGui gui;

    public SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, TapeGui gui) {
        super(parent, true);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = gui;

        initComponents();
        setSize(250, this.getHeight());
        setLocationRelativeTo(parent);

        boolean alwaysOnTop = settings.getBoolean("alwaysOnTop", false);
        chkAlwaysOnTop.setSelected(alwaysOnTop);

        boolean showAtStartup = settings.getBoolean("showAtStartup", false);
        chkShowAtStartup.setSelected(showAtStartup);
    }

    private void initComponents() {
        chkAlwaysOnTop = new JCheckBox("Always on top");
        chkShowAtStartup = new JCheckBox("Show GUI at startup");
        JButton btnSave = new JButton("Save");

        setTitle("AbstractTape settings");
        setResizable(false);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        chkAlwaysOnTop.addActionListener(e -> {
            if (gui != null) {
                gui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
            }
        });
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

        Container pane = this.getContentPane();
        GroupLayout layout = new GroupLayout(pane);
        pane.setLayout(layout);

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(chkAlwaysOnTop)
                    .addComponent(chkShowAtStartup)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(-1, Short.MAX_VALUE)
                        .addComponent(btnSave)))
                .addContainerGap());
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkAlwaysOnTop)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkShowAtStartup)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSave)
                .addContainerGap());
        pack();
    }

    private JCheckBox chkAlwaysOnTop;
    private JCheckBox chkShowAtStartup;
}
