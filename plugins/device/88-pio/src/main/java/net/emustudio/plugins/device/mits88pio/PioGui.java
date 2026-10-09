/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import javax.swing.*;
import java.awt.FlowLayout;

final class PioGui extends JFrame {
    private final PioUnit pio;
    private final JLabel values = new JLabel();
    private final Timer timer = new Timer(100, event -> refresh());

    PioGui(JFrame parent, PioUnit pio) {
        super("MITS 88-PIO");
        this.pio = pio;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setLayout(new FlowLayout());
        add(values);
        JSpinner input = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        add(input);
        JButton send = new JButton("Strobe input");
        send.addActionListener(event -> pio.writeData(((Number) input.getValue()).byteValue()));
        add(send);
        JCheckBox ready = new JCheckBox("Output device ready");
        ready.addActionListener(event -> pio.setOutputReady(ready.isSelected()));
        add(ready);
        refresh();
        pack();
        setLocationRelativeTo(parent);
        timer.start();
    }

    private void refresh() {
        values.setText(String.format("Input: %02X  Output: %02X  Interrupt enables: %02X",
                pio.getInputLatch(), pio.getOutputLatch(), pio.getEnables()));
    }

    @Override
    public void dispose() {
        timer.stop();
        super.dispose();
    }
}
