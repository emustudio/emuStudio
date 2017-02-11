/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.abstracttape.gui;

import emulib.emustudio.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {

    private SettingsManager settings;
    private long hash;
    private TapeDialog gui;

    public SettingsDialog(SettingsManager settings, long hash, TapeDialog gui) {
        this.settings = settings;
        this.hash = hash;
        initComponents();
        this.setSize(250, this.getHeight());
        String s = settings.readSetting(hash, "alwaysOnTop");
        boolean b;
        b = !(s == null || !s.toLowerCase().equals("true"));
        chkAlwaysOnTop.setSelected(b);

        s = settings.readSetting(hash, "showAtStartup");
        b = !(s == null || !s.toLowerCase().equals("true"));
        chkShowAtStartup.setSelected(b);

        this.gui = gui;
    }

    private void initComponents() {
        chkAlwaysOnTop = new JCheckBox("Always on top");
        chkShowAtStartup = new JCheckBox("Show GUI at startup");
        JButton btnOK = new JButton("OK");

        setTitle("AbstractTape settings");
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        chkAlwaysOnTop.addActionListener(e -> {
            if (gui != null) {
                gui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
            }
        });
        btnOK.addActionListener(e -> {
            if (chkAlwaysOnTop.isSelected()) {
                settings.writeSetting(hash, "alwaysOnTop", "true");
            } else {
                settings.writeSetting(hash, "alwaysOnTop", "false");
            }
            if (chkShowAtStartup.isSelected()) {
                settings.writeSetting(hash, "showAtStartup", "true");
            } else {
                settings.writeSetting(hash, "showAtStartup", "false");
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
                .addComponent(chkShowAtStartup))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addComponent(btnOK))
                .addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkAlwaysOnTop)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkShowAtStartup)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnOK));
        pack();
    }
    private JCheckBox chkAlwaysOnTop;
    private JCheckBox chkShowAtStartup;
}
