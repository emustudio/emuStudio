/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

/**
 * This is the dialog form that displays when the emuStudio automatization
 * is running.
 */
public class AutoDialog extends JDialog {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/motherboard-icon.gif";
    private final VirtualComputer computer;

    private final JLabel lblAction = new JLabel();
    private final JButton btnStop = new JButton("Stop");

    public AutoDialog(VirtualComputer computer) {
        this.computer = Objects.requireNonNull(computer);

        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JLabel lblPerforming = new JLabel(loadIcon(ICON_FILE));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        lblPerforming.setFont(lblPerforming.getFont().deriveFont(lblPerforming.getFont().getStyle() | java.awt.Font.BOLD));
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
