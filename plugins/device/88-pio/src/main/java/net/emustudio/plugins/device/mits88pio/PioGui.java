/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

final class PioGui extends JFrame {
    private final Pio8255 pio;
    private final JLabel control = new JLabel();
    private final JLabel[] values = {new JLabel(), new JLabel(), new JLabel()};
    private final Timer refreshTimer = new Timer(100, event -> refresh());

    PioGui(JFrame parent, Pio8255 pio) {
        super("MITS 88-PIO");
        this.pio = pio;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(control, BorderLayout.NORTH);

        JPanel ports = new JPanel(new GridLayout(3, 1, 4, 4));
        for (int port = 0; port < 3; port++) {
            ports.add(createPortRow(port));
        }
        add(ports, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(parent);
        refreshTimer.start();
        refresh();
    }

    private JPanel createPortRow(int port) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner input = new JSpinner(new SpinnerNumberModel(255, 0, 255, 1));
        JButton apply = new JButton("Set input");
        apply.addActionListener(event -> pio.getChannel(port).writeData(((Number) input.getValue()).byteValue()));
        row.add(new JLabel("Port " + (char) ('A' + port) + ":"));
        row.add(values[port]);
        row.add(input);
        row.add(apply);
        return row;
    }

    private void refresh() {
        control.setText(String.format("Control: %02X", pio.getControl()));
        for (int port = 0; port < 3; port++) {
            int value = pio.getChannel(port).readData() & 0xFF;
            values[port].setText(String.format("%s %02X", pio.isInput(port) ? "IN " : "OUT", value));
        }
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
