/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import javax.swing.*;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class LoadingDialog extends JDialog {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/loading.gif";

    public LoadingDialog() {
        super();
        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JLabel lblLoading = new JLabel(loadIcon(ICON_FILE));
        JLabel lblWarning = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setTitle("emuStudio");

        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setText("Loading computer, please wait...");

        lblWarning.setText("If you see some errors, please see the log file.");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblWarning, GroupLayout.PREFERRED_SIZE, 338,
                                        GroupLayout.PREFERRED_SIZE).addComponent(lblLoading))
                        .addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup().addContainerGap()
                        .addComponent(lblLoading).addPreferredGap(LayoutStyle
                                .ComponentPlacement.UNRELATED).addComponent(lblWarning)
                        .addContainerGap(lblWarning.getPreferredSize().height,
                                lblWarning.getPreferredSize().height).addContainerGap());

        pack();
    }
}
