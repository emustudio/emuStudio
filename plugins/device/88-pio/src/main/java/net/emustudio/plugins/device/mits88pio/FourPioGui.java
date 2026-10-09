/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import javax.swing.*;
import java.awt.GridLayout;

final class FourPioGui extends JFrame {
    private final PioBoard board;
    private final JLabel[] values;
    private final Timer timer = new Timer(100, event -> refresh());

    FourPioGui(JFrame parent, PioBoard board) {
        super("MITS 88-4PIO");
        this.board = board;
        values = new JLabel[board.getChannelCount()];
        setLayout(new GridLayout(values.length, 1, 4, 4));
        for (int channel = 0; channel < values.length; channel++) {
            final int port = channel;
            JPanel row = new JPanel();
            values[channel] = new JLabel();
            row.add(values[channel]);
            JSpinner input = new JSpinner(new SpinnerNumberModel(255, 0, 255, 1));
            row.add(input);
            JButton apply = new JButton("Set input pins");
            apply.addActionListener(event -> board.setInputPins(port, ((Number) input.getValue()).intValue()));
            row.add(apply);
            for (int line = 1; line <= 2; line++) {
                final int controlLine = line;
                JCheckBox high = new JCheckBox("C" + line, true);
                high.addActionListener(event -> {
                    if (controlLine == 1) { board.setControlLine1(port, high.isSelected()); }
                    else { board.setControlLine2(port, high.isSelected()); }
                });
                row.add(high);
            }
            add(row);
        }
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        refresh();
        pack();
        setLocationRelativeTo(parent);
        timer.start();
    }

    private void refresh() {
        for (int channel = 0; channel < values.length; channel++) {
            values[channel].setText(String.format("PIA%d-%s  Input: %02X  Output: %02X  DDR: %02X  Control: %02X",
                    channel / 2 + 1, (channel & 1) == 0 ? "A" : "B",
                    board.getInputPins(channel), board.getOutputPins(channel),
                    board.getOutputMask(channel), board.getControl(channel)));
        }
    }

    @Override public void dispose() { timer.stop(); super.dispose(); }
}
