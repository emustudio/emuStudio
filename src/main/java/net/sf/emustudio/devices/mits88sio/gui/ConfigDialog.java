/*
 * ConfigDialog.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.mits88sio.gui;

import emulib.emustudio.SettingsManager;
import emulib.runtime.StaticDialogs;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import net.sf.emustudio.devices.mits88sio.impl.SIOImpl;
import net.sf.emustudio.devices.mits88sio.impl.NiceButton;

@SuppressWarnings("serial")
public class ConfigDialog extends JDialog {

    private SettingsManager settings;
    private long pluginId;

    public ConfigDialog(long hash, SettingsManager settings) {
        super((Frame) null, true);
        this.settings = settings;
        this.pluginId = hash;

        initComponents();
        readSettings();
        this.setLocationRelativeTo(null);
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginId, "port1");
        if (s != null) {
            txtPort1.setText(s);
        } else {
            txtPort1.setText(String.valueOf(SIOImpl.CPU_PORT1));
        }
        s = settings.readSetting(pluginId, "port2");
        if (s != null) {
            txtPort2.setText(s);
        } else {
            txtPort2.setText(String.valueOf(SIOImpl.CPU_PORT2));
        }
    }

    private void writeSettings() {
        settings.writeSetting(pluginId, "port1", txtPort1.getText());
        settings.writeSetting(pluginId, "port2", txtPort2.getText());
    }

    private void initComponents() {
        JLabel lblDesc = new JLabel("The device has 2 I/O ports (status,data)\n that must be connected to CPU.");
        JLabel lblDesc2 = new JLabel("Settings will appear after emulator restart.");
        JLabel lblPort1LBL = new JLabel("CPU port1:");
        JLabel lblStatus = new JLabel("(status)");
        JLabel lblPort2LBL = new JLabel("CPU port2:");
        JLabel lblData = new JLabel("(data)");
        txtPort1 = new JTextField();
        txtPort2 = new JTextField();
        btnOK = new NiceButton("OK");
        btnDefault = new NiceButton("Default");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MITS 88-SIO Configuration");
        setResizable(false);

        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnOKActionPerformed(e);
            }
        });

        btnDefault.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnDefaultActionPerformed(e);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblDesc)
                .addComponent(lblDesc2)
                .addGroup(layout.createSequentialGroup()
                .addComponent(lblPort1LBL)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPort1))
                .addComponent(lblStatus)
                .addGroup(layout.createSequentialGroup()
                .addComponent(lblPort2LBL)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPort2))
                .addComponent(lblData)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(btnDefault)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnOK)))
                .addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblDesc)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDesc2)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblPort1LBL)
                .addComponent(txtPort1))
                .addComponent(lblStatus)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblPort2LBL)
                .addComponent(txtPort2))
                .addComponent(lblData)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(btnDefault)
                .addComponent(btnOK))
                .addContainerGap());
        pack();
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Integer.decode(txtPort1.getText());
        } catch (NumberFormatException e) {
            StaticDialogs.showErrorMessage("Wrong port1 number format");
            txtPort1.grabFocus();
            return;
        }
        try {
            Integer.decode(txtPort2.getText());
        } catch (NumberFormatException e) {
            StaticDialogs.showErrorMessage("Wrong port2 number format");
            txtPort2.grabFocus();
            return;
        }
        writeSettings();
        dispose();
    }

    private void btnDefaultActionPerformed(java.awt.event.ActionEvent evt) {
        txtPort1.setText(String.valueOf(SIOImpl.CPU_PORT1));
        txtPort2.setText(String.valueOf(SIOImpl.CPU_PORT2));
    }
    private JTextField txtPort1;
    private JTextField txtPort2;
    private NiceButton btnOK;
    private NiceButton btnDefault;
}
