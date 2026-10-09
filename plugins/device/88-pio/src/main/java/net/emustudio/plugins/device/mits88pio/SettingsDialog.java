/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.text.ParseException;

final class SettingsDialog extends DialogBase {
    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final JComboBox<String> boardType = new JComboBox<>(new String[]{"88-PIO", "88-4PIO"});
    private final JSpinner piaCount = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
    private final JTextField basePort = new JTextField();
    private final JSpinner interruptVector = new JSpinner(new SpinnerNumberModel(7, 0, 7, 1));
    private final JLabel hardware = new JLabel();
    private final JLabel addressing = new JLabel();
    private final DefaultListModel<String> ports = new DefaultListModel<>();
    private final String[] baseDrafts = {"0x04", "0xA0"};
    private int selectedBoard;

    SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "88-PIO Settings", true);
        this.settings = settings;
        this.dialogs = dialogs;
        this.gui = gui;
        boardType.setSelectedItem(settings.getString("boardType", "88-4PIO"));
        selectedBoard = boardType.getSelectedIndex();
        baseDrafts[selectedBoard] = String.format("0x%02X", settings.getInt("basePort", original() ? 4 : 0xA0));
        basePort.setText(baseDrafts[selectedBoard]);
        piaCount.setValue(settings.getInt("piaCount", 2));
        interruptVector.setValue(settings.getInt("interruptVector", 7));
        boardType.addActionListener(event -> {
            baseDrafts[selectedBoard] = basePort.getText();
            selectedBoard = boardType.getSelectedIndex();
            basePort.setText(baseDrafts[selectedBoard]);
            updateBoard();
        });
        piaCount.addChangeListener(event -> updatePorts());
        basePort.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePorts(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePorts(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePorts(); }
        });
        setResizable(false);
        updateBoard();
        buildContent();
    }

    private boolean original() { return "88-PIO".equals(boardType.getSelectedItem()); }

    private void updateBoard() {
        piaCount.setEnabled(!original());
        hardware.setText(original() ? "Two Intel 8212 latches: one input, one output."
                : "Motorola 6820 PIAs, each with two eight-bit channels.");
        addressing.setText(original() ? "Even base address: 00h–FEh (two ports)."
                : "Base aligned to 16 ports: 00h–F0h (four ports per PIA).");
        updatePorts();
    }

    private int validatePorts() {
        int base;
        try { base = Integer.decode(basePort.getText().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a CPU base port in decimal or 0x hexadecimal notation."); }
        if (original()) { new PioUnit(base); }
        else { new PioBoard(base, ((Number) piaCount.getValue()).intValue()); }
        return base;
    }

    private void updatePorts() {
        ports.clear();
        try {
            int base = validatePorts();
            if (original()) {
                ports.addElement(String.format("%02Xh  Status / interrupt enables", base));
                ports.addElement(String.format("%02Xh  Input / output data", base + 1));
            } else {
                for (int channel = 0; channel < ((Number) piaCount.getValue()).intValue() * 2; channel++) {
                    ports.addElement(String.format("PIA%d-%s:  %02Xh control, %02Xh DDR/data",
                            channel / 2 + 1, (channel & 1) == 0 ? "A" : "B", base + channel * 2, base + channel * 2 + 1));
                }
            }
        } catch (IllegalArgumentException e) {
            ports.addElement(e.getMessage());
        }
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel general = gui.panel("insets dialog", "[][grow]", "[][][][grow]");
        general.add(gui.label("Board:"));
        general.add(boardType, "growx, wrap");
        general.add(hardware, "span, gaptop 18, wrap");
        general.add(gui.label("Populated PIAs:"), "gaptop 18");
        general.add(piaCount, "wrap");
        general.add(gui.label("<html>MITS hard disks connect through 88-4PIO.<br>Board changes require matching peripheral connections."), "span, gaptop 18, growx");

        JPanel cpu = gui.panel("insets dialog", "[][grow][]", "[][][][grow][]");
        cpu.add(gui.label("CPU base port:"));
        cpu.add(basePort, "growx, w 100!");
        JButton defaults = gui.button("Set default");
        defaults.addActionListener(event -> { basePort.setText(original() ? "0x04" : "0xA0"); piaCount.setValue(2); });
        cpu.add(defaults, "wrap");
        cpu.add(addressing, "span, wrap");
        cpu.add(gui.label("Use decimal or 0x hexadecimal notation. Avoid CPU-port conflicts."), "span, wrap");
        JList<String> portList = new JList<>(ports);
        portList.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        portList.setVisibleRowCount(8);
        JPanel mapping = gui.section("Channel ports", "insets dialog", "[grow]", "[grow]");
        mapping.add(gui.scrollPane(portList), "grow");
        cpu.add(mapping, "span, grow");

        JPanel interrupts = gui.panel("insets dialog", "[][grow]", "[][][grow][]");
        interrupts.add(gui.label("<html>Guest software enables interrupts in the status register (88-PIO)<br>or PIA control registers (88-4PIO). The CPU must support interrupts."), "span, growx, h 63!, wrap");
        interrupts.add(gui.label("Interrupt vector (RST):"));
        interrupts.add(interruptVector, "wrap");
        interrupts.add(new JPanel(), "span, grow, wrap");
        JButton interruptDefaults = gui.button("Set default");
        interruptDefaults.addActionListener(event -> interruptVector.setValue(7));
        interrupts.add(interruptDefaults, "span, align right");

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General settings", general);
        tabs.addTab("Connection with CPU", cpu);
        tabs.addTab("Interrupts", interrupts);
        JButton save = gui.button("Save");
        gui.buttonMakePrimary(save);
        getRootPane().setDefaultButton(save);
        save.addActionListener(event -> save());
        JPanel content = gui.panel("insets dialog", "[grow]", "[grow][][]");
        content.add(tabs, "grow, w 510!, wrap");
        content.add(gui.label("Changes take effect after reopening the computer."), "wrap");
        content.add(save, "align right");
        return content;
    }

    private void save() {
        try {
            if (!original()) { piaCount.commitEdit(); }
            interruptVector.commitEdit();
            int base = validatePorts();
            int count = ((Number) piaCount.getValue()).intValue();
            int vector = ((Number) interruptVector.getValue()).intValue();
            if (count < 1 || count > 4) { throw new IllegalArgumentException("Populated PIAs must be between 1 and 4."); }
            if (vector < 0 || vector > 7) { throw new IllegalArgumentException("Interrupt vector must be between 0 and 7."); }
            settings.setString("boardType", (String) boardType.getSelectedItem());
            settings.setInt("basePort", base);
            settings.setInt("piaCount", count);
            settings.setInt("interruptVector", vector);
            dispose();
        } catch (IllegalArgumentException | ParseException | CannotUpdateSettingException e) {
            dialogs.showError(e.getMessage(), "Save settings");
        }
    }
}
