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
package net.emustudio.plugins.device.mits88sio.gui;

import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.mits88sio.SIOSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88sio.gui.Constants.MONOSPACED_PLAIN;

public class SettingsDialog extends javax.swing.JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final Dialogs dialogs;

    private final SIOSettings settings;
    private final PortListModel statusPortsModel = new PortListModel();
    private final PortListModel dataPortsModel = new PortListModel();

    public SettingsDialog(JFrame parent, SIOSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);

        statusPortsModel.addAll(settings.getStatusPorts());
        dataPortsModel.addAll(settings.getDataPorts());
    }

    private void setDefaultStatusPorts() {
        statusPortsModel.clear();
        statusPortsModel.addAll(settings.getDefaultStatusPorts());
    }

    private void setDefaultDataPorts() {
        dataPortsModel.clear();
        dataPortsModel.addAll(settings.getDefaultDataPorts());
    }


    private void initComponents() {
        JPanel jPanel1 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        JButton btnStatusAdd = new JButton();
        JButton btnStatusRemove = new JButton();
        JButton btnStatusDefault = new JButton();
        JPanel jPanel2 = new JPanel();
        JScrollPane jScrollPane2 = new JScrollPane();
        JButton btnDataAdd = new JButton();
        JButton btnDataRemove = new JButton();
        JButton btnDataDefault = new JButton();
        JLabel jLabel1 = new JLabel();
        JButton btnSave = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("MITS SIO Settings");

        jPanel1.setBorder(BorderFactory.createTitledBorder("Status port -> CPU"));

        lstStatusPorts.setFont(MONOSPACED_PLAIN);
        lstStatusPorts.setModel(statusPortsModel);
        lstStatusPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(lstStatusPorts);

        btnStatusAdd.setText("+");
        btnStatusAdd.addActionListener(this::btnStatusAddActionPerformed);

        btnStatusRemove.setText("-");
        btnStatusRemove.setToolTipText("");
        btnStatusRemove.addActionListener(this::btnStatusRemoveActionPerformed);

        btnStatusDefault.setText("Default");
        btnStatusDefault.addActionListener(this::btnStatusDefaultActionPerformed);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 173, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnStatusAdd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnStatusRemove, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(btnStatusDefault))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 107, GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(btnStatusAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnStatusRemove)))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnStatusDefault)
                    .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Data port -> CPU"));

        lstDataPorts.setFont(MONOSPACED_PLAIN);
        lstDataPorts.setModel(dataPortsModel);
        lstDataPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(lstDataPorts);

        btnDataAdd.setText("+");
        btnDataAdd.addActionListener(this::btnDataAddActionPerformed);

        btnDataRemove.setText("-");
        btnDataRemove.setToolTipText("");
        btnDataRemove.addActionListener(this::btnDataRemoveActionPerformed);

        btnDataDefault.setText("Default");
        btnDataDefault.addActionListener(this::btnDataDefaultActionPerformed);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 173, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnDataAdd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnDataRemove, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(btnDataDefault))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 107, GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(btnDataAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnDataRemove)))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnDataDefault)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel1.setText("<html>It is possible to bind status port of this device to one or more CPU ports." +
            " But please be aware of conflicting with other devices.");
        jLabel1.setVerticalAlignment(SwingConstants.TOP);

        btnSave.setText("Save");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnSave))
                    .addContainerGap())
        );

        pack();
    }

    private void btnStatusAddActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            dialogs
                .readInteger("Enter port number:", "Add status port", 0)
                .ifPresent(port -> {
                    if (dataPortsModel.contains(port)) {
                        dialogs.showError("Port number is already taken by data port", "Add status port");
                    } else {
                        statusPortsModel.add(port);
                    }

                });
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Add status port");
        }
    }

    private void btnDataAddActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            dialogs
                .readInteger("Enter port number:", "Add data port", 0)
                .ifPresent(port -> {
                    if (statusPortsModel.contains(port)) {
                        dialogs.showError("Port number is already taken by status port");
                    } else {
                        dataPortsModel.add(port);
                    }
                });
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Add data port");
        }
    }

    private void btnStatusRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        int i = lstStatusPorts.getSelectedIndex();
        if (i == -1) {
            dialogs.showError("Status port must be selected", "Remove status port");
        } else {
            statusPortsModel.removeAt(i);
        }
    }

    private void btnDataRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        int i = lstDataPorts.getSelectedIndex();
        if (i == -1) {
            dialogs.showError("Data port must be selected", "Remove data port");
        } else {
            dataPortsModel.removeAt(i);
        }
    }

    private void btnDataDefaultActionPerformed(java.awt.event.ActionEvent evt) {
        setDefaultDataPorts();
    }

    private void btnStatusDefaultActionPerformed(java.awt.event.ActionEvent evt) {
        setDefaultStatusPorts();
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        settings.setStatusPorts(statusPortsModel.getAll());
        settings.setDataPorts(dataPortsModel.getAll());

        try {
            settings.write();
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not write MITS 88-SIO settings", e);
            dialogs.showError("Could not write MITS 88-SIO settings. Please see log file for details.", "Save settings");
        }

        dispose();
    }

    private final JList<String> lstDataPorts = new JList<>();
    private final JList<String> lstStatusPorts = new JList<>();
}
