/*
 * LoadingDialog.java
 *
 * Created on Utorok, 2008, september 16, 15:55
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class LoadingDialog extends JDialog {

    /** Creates new form LoadingDialog */
    public LoadingDialog() {
        super();
        initComponents();
        this.setLocationRelativeTo(null);
    }

    private void initComponents() {

        JLabel lblLoading = new JLabel();
        JLabel lblWarning = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/motherboard-icon.gif"))); // NOI18N
        lblLoading.setText("Loading architecture, please wait...");

        lblWarning.setText("<html>If you see some errors during the loading, check your abstract scheme or plugins.");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblWarning, GroupLayout.PREFERRED_SIZE, 338, GroupLayout.PREFERRED_SIZE).addComponent(lblLoading)).addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup().addContainerGap().addComponent(lblLoading).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblWarning).addContainerGap(lblWarning.getPreferredSize().height, lblWarning.getPreferredSize().height).addContainerGap());

        pack();
    }
}
