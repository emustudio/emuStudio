/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
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
    private JCheckBox chkAlwaysOnTop;
    private JCheckBox chkShowAtStartup;

    public SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, TapeGui gui, String title) {
        super(parent, true);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = gui;

        initComponents();
        setSize(250, this.getHeight());
        setLocationRelativeTo(parent);
        setTitle(title + " settings");

        boolean alwaysOnTop = settings.getBoolean("alwaysOnTop", false);
        chkAlwaysOnTop.setSelected(alwaysOnTop);

        boolean showAtStartup = settings.getBoolean("showAtStartup", false);
        chkShowAtStartup.setSelected(showAtStartup);
    }

    private void initComponents() {
        chkAlwaysOnTop = new JCheckBox("Always on top");
        chkShowAtStartup = new JCheckBox("Show GUI at startup");
        JButton btnSave = new JButton("Save");

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
}
