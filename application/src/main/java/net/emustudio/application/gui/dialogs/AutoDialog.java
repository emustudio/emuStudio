/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * This is the dialog form that displays when the emuStudio automatization
 * is running.
 */
public class AutoDialog extends JDialog {
    private final VirtualComputer computer;

    private final JLabel lblAction = new JLabel();
    private final JButton btnStop = new JButton("Stop");

    public AutoDialog(VirtualComputer computer) {
        this.computer = Objects.requireNonNull(computer);

        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JLabel lblPerforming = new JLabel();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        lblPerforming.setFont(lblPerforming.getFont().deriveFont(lblPerforming.getFont().getStyle() | java.awt.Font.BOLD));
        lblPerforming.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/dialogs/motherboard-icon.gif")));
        lblPerforming.setText("Running automatic emulation, please wait...");

        lblAction.setText("Initializing...");

        btnStop.addActionListener(this::btnStopActionPerformed);
        btnStop.setEnabled(false);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblAction, GroupLayout.PREFERRED_SIZE, 338, GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblPerforming)
                                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(btnStop))
                        ).addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblPerforming)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAction)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStop)
                        .addContainerGap());

        pack();
    }

    /**
     * Sets abstract action. The abstract action is represented by a state
     * string and whether to enable the "Stop" button.
     *
     * @param action           action to show in the dialog
     * @param enableStopButton whether to enable the "Stop" button
     */
    public void setAction(String action, boolean enableStopButton) {
        lblAction.setText(action);
        lblAction.repaint();
        btnStop.setEnabled(enableStopButton);
    }

    private void btnStopActionPerformed(ActionEvent e) {
        computer.getCPU().ifPresent(CPU::stop);
    }
}
