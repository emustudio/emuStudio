/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.Font;
import java.text.ParseException;
import java.util.function.Supplier;

final class PioGui extends DialogBase {
    private static final Font FONT_VALUES = new Font("Monospaced", Font.BOLD, 14);
    private final GUI gui;
    private final PioUnit pio;
    private final PioBoard board;
    private final int basePort;
    private final Supplier<String> attachedDeviceId;
    private final JComboBox<String> channels = new JComboBox<>();
    private final JTextField device = new JTextField();
    private final JLabel flags = new JLabel();
    private final JLabel control = new JLabel();
    private final JLabel enables = new JLabel();
    private final JLabel input = new JLabel();
    private final JLabel output = new JLabel();
    private final JLabel direction = new JLabel();
    private final JSpinner inputValue = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
    private final JCheckBox ready = new JCheckBox("Output device ready");
    private final JCheckBox c1 = new JCheckBox("C1 high");
    private final JCheckBox c2 = new JCheckBox("C2 high");
    private final JButton applyInput;
    private final Timer timer = new Timer(100, event -> refresh());

    PioGui(JFrame parent, PioUnit pio, PioBoard board, int basePort, GUI gui, Supplier<String> attachedDeviceId) {
        super(parent, board == null ? "MITS 88-PIO" : "MITS 88-4PIO", false);
        this.gui = gui;
        this.pio = pio;
        this.board = board;
        this.basePort = basePort;
        this.attachedDeviceId = attachedDeviceId;
        applyInput = gui.button(board == null ? "Strobe input" : "Set input pins");
        if (board != null) {
            for (int channel = 0; channel < board.getChannelCount(); channel++) {
                channels.addItem("PIA" + (channel / 2 + 1) + ((channel & 1) == 0 ? "-A" : "-B"));
            }
            channels.addActionListener(event -> {
                inputValue.setValue(board.getInputPins(channels.getSelectedIndex()));
                refresh();
            });
            inputValue.setValue(board.getInputPins(0));
        }
        applyInput.addActionListener(event -> {
            try {
                inputValue.commitEdit();
                int value = ((Number) inputValue.getValue()).intValue();
                if (board == null) { pio.writeData((byte) value); }
                else {
                    synchronized (board) {
                        if (!board.hasPeripheral()) { board.setInputPins(channels.getSelectedIndex(), value); }
                    }
                }
                refresh();
            } catch (ParseException e) {
                inputValue.requestFocusInWindow();
            }
        });
        ready.addActionListener(event -> { pio.setOutputReady(ready.isSelected()); refresh(); });
        c1.addActionListener(event -> {
            synchronized (board) {
                if (!board.hasPeripheral()) { board.setControlLine1(channels.getSelectedIndex(), c1.isSelected()); }
            }
            refresh();
        });
        c2.addActionListener(event -> {
            synchronized (board) {
                if (!board.hasPeripheral() && (board.getControl(channels.getSelectedIndex()) & 0x20) == 0) {
                    board.setControlLine2(channels.getSelectedIndex(), c2.isSelected());
                }
            }
            refresh();
        });
        setResizable(false);
        refresh();
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        device.setEditable(false);
        device.setFont(new Font("sansserif", Font.BOLD, 14));
        JPanel attached = gui.section("Attached device", "insets dialog, fill", "[grow]", "[]");
        attached.add(device, "growx, h 43!");

        flags.setFont(FONT_VALUES);
        flags.setHorizontalAlignment(SwingConstants.CENTER);
        flags.setBorder(BorderFactory.createEtchedBorder());
        JPanel status = gui.section("Control channel", "insets dialog", "[grow]", "[][][][][][][grow]");
        status.add(gui.label("Ready flags and interrupt status."), "growx, h 46!, wrap");
        status.add(flags, "growx, h 33!, wrap");
        status.add(gui.label("Hex value:"), "split 2");
        status.add(control, "wrap");
        status.add(new JSeparator(), "growx, wrap");
        if (board == null) {
            status.add(gui.label("R  Output device ready"), "wrap");
            status.add(gui.label("D  Input data ready"), "wrap");
            status.add(gui.label("Interrupt enables:"), "split 2");
            status.add(enables, "wrap");
        } else {
            status.add(gui.label("1  C1 interrupt flag"), "wrap");
            status.add(gui.label("2  C2 interrupt flag"), "wrap");
            status.add(gui.label("D  Data register selected (otherwise DDR)"), "wrap");
            status.add(gui.label("I  Enabled interrupt pending"), "wrap");
        }

        JPanel data = gui.section(board == null ? "Data latches" : "Data pins", "insets dialog", "[][grow]", "[][][][][grow]");
        data.add(gui.label("Eight-bit input and output values."), "span, growx, h 46!, wrap");
        for (JLabel value : new JLabel[]{input, output, direction}) { value.setFont(FONT_VALUES); }
        data.add(gui.label("Input:"));
        data.add(input, "growx, h 33!, wrap");
        data.add(gui.label("Output:"));
        data.add(output, "growx, h 33!, wrap");
        if (board != null) {
            data.add(gui.label("DDR:"));
            data.add(direction, "growx, wrap");
            data.add(gui.label("DDR: 1 = output, 0 = input"), "span, wrap");
        }

        JPanel manual = gui.section("Peripheral input", "insets dialog", "[][grow][]", "[][]");
        manual.add(gui.label("Input byte:"));
        manual.add(inputValue, "w 100!");
        manual.add(applyInput, "wrap");
        if (board == null) { manual.add(ready, "span"); }
        else {
            manual.add(c1, "split 2");
            manual.add(c2, "span, wrap");
            manual.add(gui.label("Manual input is available when no peripheral is connected."), "span");
        }

        JPanel content = gui.panel("insets dialog", "[grow][grow]", "[][][grow][]");
        content.add(attached, "span, growx, wrap");
        if (board != null) {
            content.add(gui.label("Channel:"), "split 2");
            content.add(channels, "span, wrap");
        }
        content.add(status, "grow");
        content.add(data, "grow, wrap");
        content.add(manual, "span, growx");
        return content;
    }

    void refresh() {
        device.setText(attachedDeviceId.get());
        if (board == null) {
            synchronized (pio) {
                int status = pio.read(basePort) & 255; // Status reads do not acknowledge input.
                flags.setText(((status & 1) != 0 ? "R" : ".") + " " + ((status & 2) != 0 ? "D" : "."));
                control.setText(hex(status));
                enables.setText(hex(pio.getEnables()));
                input.setText(hex(pio.getInputLatch()));
                output.setText(hex(pio.getOutputLatch()));
                ready.setSelected((status & 1) != 0);
            }
        } else {
            synchronized (board) {
                int channel = channels.getSelectedIndex();
                int value = board.getControl(channel);
                boolean irq = ((value & 0x81) == 0x81) || ((value & 0x68) == 0x48);
                flags.setText(((value & 0x80) != 0 ? "1" : ".") + " "
                        + ((value & 0x40) != 0 ? "2" : ".") + " "
                        + ((value & 4) != 0 ? "D" : ".") + " " + (irq ? "I" : "."));
                control.setText(hex(value));
                input.setText(hex(board.getInputPins(channel)));
                output.setText(hex(board.getOutputPins(channel)));
                direction.setText(hex(board.getOutputMask(channel)));
                c1.setSelected(board.getControlLine1(channel));
                c2.setSelected(board.getControlLine2(channel));
                boolean manual = !board.hasPeripheral();
                inputValue.setEnabled(manual);
                applyInput.setEnabled(manual);
                c1.setEnabled(manual);
                c2.setEnabled(manual && (value & 0x20) == 0);
            }
        }
    }

    private static String hex(int value) { return String.format("0x%02X", value); }

    @Override public void setVisible(boolean visible) {
        if (visible) { refresh(); timer.start(); }
        else { timer.stop(); }
        super.setVisible(visible);
    }

    @Override public void dispose() { timer.stop(); super.dispose(); }
}
