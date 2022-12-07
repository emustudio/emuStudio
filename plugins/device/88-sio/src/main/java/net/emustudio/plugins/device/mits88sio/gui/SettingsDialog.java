/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.mits88sio.SioUnitSettings;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88sio.gui.Constants.MONOSPACED_PLAIN;

public class SettingsDialog extends JDialog {
    private final Dialogs dialogs;

    private final SioUnitSettings settings;
    private final PortListModel statusPortsModel = new PortListModel();
    private final PortListModel dataPortsModel = new PortListModel();

    public SettingsDialog(JFrame parent, SioUnitSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);
        readSettings();
    }

    private void readSettings() {
        chkAnsiMode.setSelected(settings.isClearOutputBit8());
        chkTtyMode.setSelected(settings.isClearInputBit8());
        chkToUpperCase.setSelected(settings.isInputToUpperCase());
        cmbMapDel.setSelectedIndex(settings.getMapDeleteChar().ordinal());
        cmbMapBs.setSelectedIndex(settings.getMapBackspaceChar().ordinal());
        spnInputInterrupt.setValue(settings.getInputInterruptVector());
        spnOutputInterrupt.setValue(settings.getOutputInterruptVector());
        chkInterruptsSupported.setSelected(settings.getInterruptsSupported());

        statusPortsModel.clear();
        statusPortsModel.addAll(settings.getStatusPorts());

        dataPortsModel.clear();
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
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel panelSettings = new JPanel();
        JLabel lblMapDel = new JLabel("Map DEL char to:");
        JLabel lblMapBs = new JLabel("Map BACKSPACE char to:");
        JPanel panelCpu = new JPanel();
        JLabel lblCpuNote = new JLabel("<html>88-SIO has two ports/channels: Status channel and Data channel.  Attach these channels to CPU ports (possibly to multiple ports). Be aware of possible CPU-port conflicts.");
        JPanel panelStatusChannel = new JPanel();
        JScrollPane srlStatus = new JScrollPane();
        JPanel panelDataChannel = new JPanel();
        JScrollPane srlData = new JScrollPane();
        JPanel panelInterrupts = new JPanel();
        JLabel lblInputInterrupt = new JLabel("Input interrupt vector:");
        JLabel lblOutputInterrupt = new JLabel("Output interrupt vector:");
        JLabel lblInterruptsNote = new JLabel("<html>88-SIO can support input and output interrupts. Input interrupt is triggered when 88-SIO received data from connected device. Output interrupt is triggered when 88-SIO receives data from CPU.");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("88-SIO Settings");
        setResizable(false);
        rootPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        cmbMapBs.setEditable(false);
        cmbMapDel.setEditable(false);

        GroupLayout panelSettingsLayout = new GroupLayout(panelSettings);
        panelSettings.setLayout(panelSettingsLayout);
        panelSettingsLayout.setHorizontalGroup(
            panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelSettingsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkTtyMode)
                        .addComponent(chkAnsiMode)
                        .addComponent(chkToUpperCase)
                        .addGroup(panelSettingsLayout.createSequentialGroup()
                            .addGroup(panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblMapDel)
                                .addComponent(lblMapBs))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(cmbMapDel, 0, 157, Short.MAX_VALUE)
                                .addComponent(cmbMapBs, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addContainerGap(128, Short.MAX_VALUE))
        );
        panelSettingsLayout.setVerticalGroup(
            panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelSettingsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(chkTtyMode)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(chkAnsiMode)
                    .addGap(18, 18, 18)
                    .addComponent(chkToUpperCase)
                    .addGap(18, 18, 18)
                    .addGroup(panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblMapDel)
                        .addComponent(cmbMapDel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(cmbMapBs, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblMapBs))
                    .addContainerGap(57, Short.MAX_VALUE))
        );

        tabbedPane.addTab("General settings", panelSettings);

        panelStatusChannel.setBorder(BorderFactory.createTitledBorder("Status channel ports"));

        lstStatusPorts.setFont(MONOSPACED_PLAIN);
        lstStatusPorts.setModel(statusPortsModel);
        lstStatusPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        srlStatus.setViewportView(lstStatusPorts);

        GroupLayout panelStatusChannelLayout = new GroupLayout(panelStatusChannel);
        panelStatusChannel.setLayout(panelStatusChannelLayout);
        panelStatusChannelLayout.setHorizontalGroup(
            panelStatusChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelStatusChannelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(srlStatus, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                    .addGroup(panelStatusChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnStatusAdd, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStatusRemove, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStatusDefaults, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        panelStatusChannelLayout.setVerticalGroup(
            panelStatusChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelStatusChannelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelStatusChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(srlStatus, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(panelStatusChannelLayout.createSequentialGroup()
                            .addComponent(btnStatusAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnStatusRemove)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                            .addComponent(btnStatusDefaults)))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelDataChannel.setBorder(BorderFactory.createTitledBorder("Data channel ports"));

        lstDataPorts.setFont(MONOSPACED_PLAIN);
        lstDataPorts.setModel(dataPortsModel);
        lstDataPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        srlData.setViewportView(lstDataPorts);

        GroupLayout panelDataChannelLayout = new GroupLayout(panelDataChannel);
        panelDataChannel.setLayout(panelDataChannelLayout);
        panelDataChannelLayout.setHorizontalGroup(
            panelDataChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelDataChannelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(srlData, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelDataChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnDataRemove, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDataAdd, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDataDefaults, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        panelDataChannelLayout.setVerticalGroup(
            panelDataChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelDataChannelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelDataChannelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(srlData, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(panelDataChannelLayout.createSequentialGroup()
                            .addComponent(btnDataAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnDataRemove)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                            .addComponent(btnDataDefaults)))
                    .addContainerGap())
        );

        GroupLayout panelCpuLayout = new GroupLayout(panelCpu);
        panelCpu.setLayout(panelCpuLayout);
        panelCpuLayout.setHorizontalGroup(
            panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelCpuLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelCpuLayout.createSequentialGroup()
                            .addComponent(lblCpuNote, GroupLayout.PREFERRED_SIZE, 438, GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(panelCpuLayout.createSequentialGroup()
                            .addComponent(panelStatusChannel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panelDataChannel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        panelCpuLayout.setVerticalGroup(
            panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelCpuLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblCpuNote, GroupLayout.PREFERRED_SIZE, 63, GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelStatusChannel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(panelDataChannel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Connection with CPU", panelCpu);

        GroupLayout panelInterruptsLayout = new GroupLayout(panelInterrupts);
        panelInterrupts.setLayout(panelInterruptsLayout);
        panelInterruptsLayout.setHorizontalGroup(
            panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelInterruptsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblInterruptsNote, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(GroupLayout.Alignment.TRAILING, panelInterruptsLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnInterruptDefaults))
                        .addGroup(panelInterruptsLayout.createSequentialGroup()
                            .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(chkInterruptsSupported)
                                .addGroup(panelInterruptsLayout.createSequentialGroup()
                                    .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblInputInterrupt)
                                        .addComponent(lblOutputInterrupt))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(spnInputInterrupt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(spnOutputInterrupt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
                            .addGap(0, 225, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        panelInterruptsLayout.setVerticalGroup(
            panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, panelInterruptsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblInterruptsNote, GroupLayout.PREFERRED_SIZE, 63, GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addComponent(chkInterruptsSupported)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblInputInterrupt)
                        .addComponent(spnInputInterrupt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelInterruptsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblOutputInterrupt)
                        .addComponent(spnOutputInterrupt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                    .addComponent(btnInterruptDefaults)
                    .addContainerGap())
        );

        tabbedPane.addTab("Interrupts", panelInterrupts);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(btnSave)
                    .addContainerGap())
                .addComponent(tabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(tabbedPane)
                    .addGap(12, 12, 12)
                    .addComponent(btnSave)
                    .addContainerGap())
        );

        btnStatusAdd.addActionListener(e -> addPort("status", "data", statusPortsModel, dataPortsModel));
        btnStatusRemove.addActionListener(e -> removePort("status", lstStatusPorts, statusPortsModel));
        btnStatusDefaults.addActionListener(e -> setDefaultStatusPorts());

        btnDataAdd.addActionListener(e -> addPort("data", "status", dataPortsModel, statusPortsModel));
        btnDataRemove.addActionListener(e -> removePort("data", lstDataPorts, dataPortsModel));
        btnDataDefaults.addActionListener(e -> setDefaultDataPorts());

        btnSave.addActionListener(this::btnSaveActionPerformed);

        pack();
    }


    private void addPort(String nameAdd, String nameCheck, PortListModel portModelAdd, PortListModel portModelCheck) {
        try {
            dialogs
                .readInteger("Enter port number:", "Add " + nameAdd + " port", 0)
                .ifPresent(port -> {
                    if (portModelCheck.contains(port)) {
                        dialogs.showError("Port number is already taken by " + nameCheck + " port");
                    } else {
                        portModelAdd.add(port);
                    }
                });
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Add " + nameAdd + " port");
        }
    }

    private void removePort(String name, JList<String> lstPorts, PortListModel portModel) {
        int i = lstPorts.getSelectedIndex();
        if (i == -1) {
            dialogs.showError(name + " port must be selected", "Remove " + name + " port");
        } else {
            portModel.removeAt(i);
        }
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        int inputInterruptVector;
        int outputInterruptVector;
        try {
            inputInterruptVector = ((Number) spnInputInterrupt.getValue()).intValue();
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse input interrupt vector", "88-SIO Settings");
            spnInputInterrupt.grabFocus();
            return;
        }
        try {
            outputInterruptVector = ((Number) spnOutputInterrupt.getValue()).intValue();
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse interrupt vector", "88-SIO Settings");
            spnOutputInterrupt.grabFocus();
            return;
        }
        if (inputInterruptVector < 0 || inputInterruptVector > 7) {
            dialogs.showError("Allowed range of input interrupt vector is 0-7");
            spnInputInterrupt.grabFocus();
            return;
        }
        if (outputInterruptVector < 0 || outputInterruptVector > 7) {
            dialogs.showError("Allowed range of output interrupt vector is 0-7");
            spnOutputInterrupt.grabFocus();
            return;
        }

        settings.setStatusPorts(statusPortsModel.getAll());
        settings.setDataPorts(dataPortsModel.getAll());

        settings.setClearInputBit8(chkTtyMode.isSelected());
        settings.setClearOutputBit8(chkAnsiMode.isSelected());
        settings.setInputToUpperCase(chkToUpperCase.isSelected());

        settings.setMapBackspaceChar(cmbMapBs.getItemAt(cmbMapBs.getSelectedIndex()));
        settings.setMapDeleteChar(cmbMapDel.getItemAt(cmbMapDel.getSelectedIndex()));

        settings.setInterruptsSupported(chkInterruptsSupported.isSelected());
        settings.setInputInterruptVector(inputInterruptVector);
        settings.setOutputInterruptVector(outputInterruptVector);

        dispose();
    }


    private final JButton btnDataAdd = new JButton("Add");
    private final JButton btnDataDefaults = new JButton("Set default");
    private final JButton btnDataRemove = new JButton("Remove");
    private final JButton btnInterruptDefaults = new JButton("Set default");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnStatusAdd = new JButton("Add");
    private final JButton btnStatusDefaults = new JButton("Set default");
    private final JButton btnStatusRemove = new JButton("Remove");
    private final JCheckBox chkAnsiMode = new JCheckBox("ANSI mode (clear output bit 8)");
    private final JCheckBox chkInterruptsSupported = new JCheckBox("Interrupts supported");
    private final JCheckBox chkToUpperCase = new JCheckBox("Convert input to upper-case");
    private final JCheckBox chkTtyMode = new JCheckBox("TTY mode (clear input bit 8)");
    private final JComboBox<SioUnitSettings.MAP_CHAR> cmbMapBs = new JComboBox<>(new DefaultComboBoxModel<>(
        SioUnitSettings.MAP_CHAR.values()
    ));
    private final JComboBox<SioUnitSettings.MAP_CHAR> cmbMapDel = new JComboBox<>(new DefaultComboBoxModel<>(
        SioUnitSettings.MAP_CHAR.values()
    ));
    private final JList<String> lstDataPorts = new JList<>();
    private final JList<String> lstStatusPorts = new JList<>();
    private final JSpinner spnInputInterrupt = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
    private final JSpinner spnOutputInterrupt = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
}
