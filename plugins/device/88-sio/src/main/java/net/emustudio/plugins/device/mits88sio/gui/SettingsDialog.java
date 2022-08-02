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

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.mits88sio.settings.SioUnitSettings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static net.emustudio.plugins.device.mits88sio.gui.Constants.MONOSPACED_PLAIN;

public class SettingsDialog extends JDialog {
    private final Dialogs dialogs;

    private final SioUnitSettings settings;
    private final PortListModel statusPortsModel = new PortListModel();
    private final PortListModel dataPortsModel = new PortListModel();
    private final RadixUtils radixUtils = RadixUtils.getInstance();

    public SettingsDialog(JFrame parent, SioUnitSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);

        statusPortsModel.addAll(settings.getStatusPorts());
        dataPortsModel.addAll(settings.getDataPorts());
        reload();
    }

    private void reload() {
        chkClearInputBit8.setSelected(settings.isClearInputBit8());
        chkClearOutputBit8.setSelected(settings.isClearOutputBit8());
        chkInputToUpperCase.setSelected(settings.isInputToUpperCase());
        cmbMapBackspaceChar.setSelectedIndex(settings.getMapBackspaceChar().ordinal());
        cmbMapDeleteChar.setSelectedIndex(settings.getMapDeleteChar().ordinal());
        txtInputInterruptVector.setText(String.valueOf(settings.getInputInterruptVector()));
        txtOutputInterruptVector.setText(String.valueOf(settings.getOutputInterruptVector()));
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
        JPanel panelStatusPorts = new JPanel();
        JScrollPane scrollStatusPorts = new JScrollPane();
        JButton btnStatusAdd = new JButton();
        JButton btnStatusRemove = new JButton();
        JButton btnStatusDefault = new JButton();

        JPanel panelDataPorts = new JPanel();
        JScrollPane scrollDataPorts = new JScrollPane();
        JButton btnDataAdd = new JButton();
        JButton btnDataRemove = new JButton();
        JButton btnDataDefault = new JButton();

        JPanel panelGeneral = new JPanel();
        JLabel lblMapDeleteChar = new JLabel("Map DEL char to:");
        JLabel lblMapBackspaceChar = new JLabel("Map BS char to:");
        JLabel lblInputInterruptVector = new JLabel("Input interrupt vector (0-7):");
        JLabel lblOutputInterruptVector = new JLabel("Output interrupt vector (0-7):");

        JLabel lblCaution = new JLabel();
        JButton btnSave = new JButton("Save");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("MITS 88-SIO Settings");

        panelStatusPorts.setBorder(BorderFactory.createTitledBorder("Status port -> CPU"));
        lstStatusPorts.setFont(MONOSPACED_PLAIN);
        lstStatusPorts.setModel(statusPortsModel);
        lstStatusPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollStatusPorts.setViewportView(lstStatusPorts);

        btnStatusAdd.setText("+");
        btnStatusAdd.addActionListener(e -> addPort("status", "data", statusPortsModel, dataPortsModel));

        btnStatusRemove.setText("-");
        btnStatusRemove.setToolTipText("");
        btnStatusRemove.addActionListener(e -> removePort("status", lstStatusPorts, statusPortsModel));

        btnStatusDefault.setText("Set default");
        btnStatusDefault.addActionListener(e -> setDefaultStatusPorts());

        GroupLayout layoutStatusPorts = new GroupLayout(panelStatusPorts);
        panelStatusPorts.setLayout(layoutStatusPorts);
        layoutStatusPorts.setHorizontalGroup(
            layoutStatusPorts.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutStatusPorts.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layoutStatusPorts.createParallelGroup(Alignment.LEADING)
                        .addGroup(layoutStatusPorts.createSequentialGroup()
                            .addComponent(scrollStatusPorts, PREFERRED_SIZE, 173, PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layoutStatusPorts.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(btnStatusAdd, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnStatusRemove, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(btnStatusDefault))
                    .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layoutStatusPorts.setVerticalGroup(
            layoutStatusPorts.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutStatusPorts.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layoutStatusPorts.createParallelGroup(Alignment.LEADING)
                        .addComponent(scrollStatusPorts, PREFERRED_SIZE, 107, PREFERRED_SIZE)
                        .addGroup(layoutStatusPorts.createSequentialGroup()
                            .addComponent(btnStatusAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnStatusRemove)))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnStatusDefault)
                    .addContainerGap(16, Short.MAX_VALUE))
        );

        panelDataPorts.setBorder(BorderFactory.createTitledBorder("Data port -> CPU"));
        lstDataPorts.setFont(MONOSPACED_PLAIN);
        lstDataPorts.setModel(dataPortsModel);
        lstDataPorts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollDataPorts.setViewportView(lstDataPorts);

        btnDataAdd.setText("+");
        btnDataAdd.addActionListener(e -> addPort("data", "status", dataPortsModel, statusPortsModel));

        btnDataRemove.setText("-");
        btnDataRemove.setToolTipText("");
        btnDataRemove.addActionListener(e -> removePort("data", lstDataPorts, dataPortsModel));

        btnDataDefault.setText("Set default");
        btnDataDefault.addActionListener(e -> setDefaultDataPorts());

        GroupLayout layoutDataPorts = new GroupLayout(panelDataPorts);
        panelDataPorts.setLayout(layoutDataPorts);
        layoutDataPorts.setHorizontalGroup(
            layoutDataPorts.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutDataPorts.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layoutDataPorts.createParallelGroup(Alignment.LEADING)
                        .addGroup(layoutDataPorts.createSequentialGroup()
                            .addComponent(scrollDataPorts, PREFERRED_SIZE, 173, PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layoutDataPorts.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(btnDataAdd, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnDataRemove, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(btnDataDefault))
                    .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layoutDataPorts.setVerticalGroup(
            layoutDataPorts.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutDataPorts.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layoutDataPorts.createParallelGroup(Alignment.LEADING)
                        .addComponent(scrollDataPorts, PREFERRED_SIZE, 107, PREFERRED_SIZE)
                        .addGroup(layoutDataPorts.createSequentialGroup()
                            .addComponent(btnDataAdd)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnDataRemove)))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnDataDefault)
                    .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelGeneral.setBorder(BorderFactory.createTitledBorder("General settings"));
        GroupLayout layoutGeneral = new GroupLayout(panelGeneral);
        panelGeneral.setLayout(layoutGeneral);
        cmbMapBackspaceChar.setEditable(false);
        cmbMapDeleteChar.setEditable(false);

        layoutGeneral.setHorizontalGroup(
            layoutGeneral.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutGeneral.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkClearInputBit8)
                        .addComponent(chkClearOutputBit8)
                        .addComponent(chkInputToUpperCase)
                        .addGroup(layoutGeneral.createSequentialGroup()
                            .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblMapDeleteChar)
                                .addComponent(lblMapBackspaceChar)
                                .addComponent(lblInputInterruptVector)
                                .addComponent(lblOutputInterruptVector))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                                .addComponent(cmbMapDeleteChar)
                                .addComponent(cmbMapBackspaceChar)
                                .addComponent(txtInputInterruptVector)
                                .addComponent(txtOutputInterruptVector))
                            .addContainerGap()))));

        layoutGeneral.setVerticalGroup(
            layoutDataPorts.createParallelGroup(Alignment.LEADING)
                .addGroup(layoutGeneral.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(chkClearInputBit8)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(chkClearOutputBit8)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(chkInputToUpperCase)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblMapDeleteChar)
                        .addComponent(cmbMapDeleteChar))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblMapBackspaceChar)
                        .addComponent(cmbMapBackspaceChar))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblInputInterruptVector)
                        .addComponent(txtInputInterruptVector))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layoutGeneral.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblOutputInterruptVector)
                        .addComponent(txtOutputInterruptVector))
                    .addContainerGap()));


        lblCaution.setText("<html>Attach 88-SIO ports to one or more CPU ports." +
            " Be aware of possible port conflicts with other devices.");
        lblCaution.setVerticalAlignment(SwingConstants.TOP);

        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.addActionListener(this::btnSaveActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblCaution, PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelStatusPorts, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panelDataPorts, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelGeneral, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnSave, PREFERRED_SIZE, 83, PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblCaution, PREFERRED_SIZE, 49, PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(panelStatusPorts, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelDataPorts, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(panelGeneral, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(btnSave))
                    .addContainerGap())
        );

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
            inputInterruptVector = radixUtils.parseRadix(txtInputInterruptVector.getText().trim());
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse input interrupt vector", "88-SIO Settings");
            return;
        }
        try {
            outputInterruptVector = radixUtils.parseRadix(txtOutputInterruptVector.getText().trim());
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse interrupt vector", "88-SIO Settings");
            return;
        }
        if (inputInterruptVector < 0 || inputInterruptVector > 7) {
            dialogs.showError("Allowed range of input interrupt vector is 0-7");
            return;
        }
        if (outputInterruptVector < 0 || outputInterruptVector > 7) {
            dialogs.showError("Allowed range of output interrupt vector is 0-7");
            return;
        }

        settings.setStatusPorts(statusPortsModel.getAll());
        settings.setDataPorts(dataPortsModel.getAll());

        settings.setClearInputBit8(chkClearInputBit8.isSelected());
        settings.setClearOutputBit8(chkClearOutputBit8.isSelected());
        settings.setInputToUpperCase(chkInputToUpperCase.isSelected());

        settings.setMapBackspaceChar(cmbMapBackspaceChar.getItemAt(cmbMapBackspaceChar.getSelectedIndex()));
        settings.setMapDeleteChar(cmbMapDeleteChar.getItemAt(cmbMapDeleteChar.getSelectedIndex()));

        settings.setInputInterruptVector(inputInterruptVector);
        settings.setOutputInterruptVector(outputInterruptVector);

        dispose();
    }

    private final JList<String> lstDataPorts = new JList<>();
    private final JList<String> lstStatusPorts = new JList<>();
    private final JCheckBox chkClearInputBit8 = new JCheckBox("TTY mode (clear bit 8 of input)");
    private final JCheckBox chkClearOutputBit8 = new JCheckBox("ANSI mode (clear bit 8 on output)");
    private final JCheckBox chkInputToUpperCase = new JCheckBox("Convert input to upper-case");
    private final JTextField txtInputInterruptVector = new JTextField();
    private final JTextField txtOutputInterruptVector = new JTextField();
    private final JComboBox<SioUnitSettings.MAP_CHAR> cmbMapBackspaceChar = new JComboBox<>(new DefaultComboBoxModel<>(
        SioUnitSettings.MAP_CHAR.values()
    ));
    private final JComboBox<SioUnitSettings.MAP_CHAR> cmbMapDeleteChar = new JComboBox<>(new DefaultComboBoxModel<>(
        SioUnitSettings.MAP_CHAR.values()
    ));
}
