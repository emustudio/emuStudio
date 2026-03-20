/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.mits88sio.SioUnitSettings;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

public class SettingsDialog extends DialogBase {
    private final GUI gui;
    private final Dialogs dialogs;
    private final SioUnitSettings settings;
    private final PortListModel statusPortsModel = new PortListModel();
    private final PortListModel dataPortsModel = new PortListModel();
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
    private final JSpinner spnInputInterrupt = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
    private final JSpinner spnOutputInterrupt = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));

    public SettingsDialog(JFrame parent, SioUnitSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "88-SIO Settings", true);
        this.gui = gui;

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        setResizable(false);
        readSettings();
        buildContent();
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
        statusPortsModel.setAll(settings.getStatusPorts());
        dataPortsModel.setAll(settings.getDataPorts());
    }

    @Override
    protected JComponent initializeComponents() {
        // Tab 1: General settings
        JPanel panelSettings = gui.panel("insets dialog", "[grow]", "[][][][18][][18][][][grow]");
        panelSettings.add(chkTtyMode, "wrap");
        panelSettings.add(chkAnsiMode, "wrap");
        panelSettings.add(chkToUpperCase, "gaptop 18, wrap");
        panelSettings.add(gui.label("Map DEL char to:"), "gaptop 18, split 2");
        panelSettings.add(cmbMapDel, "w 157!, wrap");
        panelSettings.add(gui.label("Map BACKSPACE char to:"), "split 2");
        panelSettings.add(cmbMapBs, "w 157!, wrap");

        // Tab 2: Connection with CPU
        JList<String> lstStatusPorts = new JList<>(statusPortsModel);
        JList<String> lstDataPorts = new JList<>(dataPortsModel);

        JPanel panelCpu = gui.panel("insets dialog", "[grow][grow]", "[][grow]");
        panelCpu.add(gui.label("<html>88-SIO has two ports/channels: Status channel and Data channel.  Attach these channels to CPU ports (possibly to multiple ports). Be aware of possible CPU-port conflicts."), "span, growx, h 63!, wrap");
        panelCpu.add(createPortChannelSection("Status channel ports", lstStatusPorts, statusPortsModel, "status", "data", dataPortsModel, settings::getDefaultStatusPorts), "grow");
        panelCpu.add(createPortChannelSection("Data channel ports", lstDataPorts, dataPortsModel, "data", "status", statusPortsModel, settings::getDefaultDataPorts), "grow");

        // Tab 3: Interrupts
        JButton btnInterruptDefaults = new JButton("Set default");
        btnInterruptDefaults.addActionListener(e -> {
            spnInputInterrupt.setValue(0);
            spnOutputInterrupt.setValue(0);
            chkInterruptsSupported.setSelected(false);
        });

        JPanel panelInterrupts = gui.panel("insets dialog", "[grow]", "[][][][][][grow][]");
        panelInterrupts.add(gui.label("<html>88-SIO can support input and output interrupts. Input interrupt is triggered when 88-SIO received data from connected device. Output interrupt is triggered when 88-SIO receives data from CPU."), "growx, h 63!, wrap");
        panelInterrupts.add(chkInterruptsSupported, "gaptop 18, wrap");
        panelInterrupts.add(gui.label("Input interrupt vector:"), "split 2");
        panelInterrupts.add(spnInputInterrupt, "wrap");
        panelInterrupts.add(gui.label("Output interrupt vector:"), "split 2");
        panelInterrupts.add(spnOutputInterrupt, "wrap");
        panelInterrupts.add(new JPanel(), "grow, wrap");
        panelInterrupts.add(btnInterruptDefaults, "align right");

        // Tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General settings", panelSettings);
        tabbedPane.addTab("Connection with CPU", panelCpu);
        tabbedPane.addTab("Interrupts", panelInterrupts);

        // Save button
        JButton btnSave = new JButton("Save");
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.addActionListener(e -> {
            settings.setStatusPorts(statusPortsModel.getAll());
            settings.setDataPorts(dataPortsModel.getAll());
            settings.setClearInputBit8(chkTtyMode.isSelected());
            settings.setClearOutputBit8(chkAnsiMode.isSelected());
            settings.setInputToUpperCase(chkToUpperCase.isSelected());
            settings.setMapBackspaceChar(cmbMapBs.getItemAt(cmbMapBs.getSelectedIndex()));
            settings.setMapDeleteChar(cmbMapDel.getItemAt(cmbMapDel.getSelectedIndex()));
            settings.setInterruptsSupported(chkInterruptsSupported.isSelected());
            settings.setInputInterruptVector(((Number) spnInputInterrupt.getValue()).intValue());
            settings.setOutputInterruptVector(((Number) spnOutputInterrupt.getValue()).intValue());
            dispose();
        });

        JPanel content = gui.panel("insets dialog", "[grow]", "[grow][]");
        content.add(tabbedPane, "grow, w 460!, wrap");
        content.add(btnSave, "align right");
        return content;
    }

    private JPanel createPortChannelSection(String title, JList<String> list, PortListModel model,
                                            String nameAdd, String nameCheck, PortListModel checkModel,
                                            java.util.function.Supplier<Collection<Integer>> defaultPorts) {
        list.setFont(FONT_MONOSPACED);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnAdd = new JButton("Add");
        JButton btnRemove = new JButton("Remove");
        JButton btnDefaults = new JButton("Set default");
        btnAdd.addActionListener(e -> addPort(nameAdd, nameCheck, model, checkModel));
        btnRemove.addActionListener(e -> removePort(nameAdd, list, model));
        btnDefaults.addActionListener(e -> model.setAll(defaultPorts.get()));

        JPanel buttons = gui.panel("insets 0, flowy", "[grow]", "[][][grow][]");
        buttons.add(btnAdd, "growx");
        buttons.add(btnRemove, "growx");
        buttons.add(new JPanel(), "grow");
        buttons.add(btnDefaults, "growx");

        JPanel section = gui.section(title, "insets dialog", "[67!,grow][grow]", "[grow]");
        section.add(new JScrollPane(list), "grow");
        section.add(buttons, "grow");
        return section;
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
}
