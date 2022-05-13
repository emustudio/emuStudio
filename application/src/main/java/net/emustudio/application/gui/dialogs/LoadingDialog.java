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
package net.emustudio.application.gui.dialogs;

import javax.swing.*;

public class LoadingDialog extends JDialog {

    public LoadingDialog() {
        super();
        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {

        JLabel lblLoading = new JLabel();
        JLabel lblWarning = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setTitle("emuStudio");

        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setIcon(new ImageIcon(getClass()
            .getResource("/net/emustudio/application/gui/dialogs/loading.gif")));
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
