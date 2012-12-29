/*
 * SIODialog.java
 *
 * Created on Nedeľa, 2008, júl 27, 19:52
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.mits88sio.gui;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class SIODialog extends JDialog {

    public SIODialog(int port1CPU, int port2CPU) {
        initComponents();
        lblPort1.setText(String.format("0x%X", port1CPU));
        lblPort2.setText(String.format("0x%X", port2CPU));
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JLabel lblAttached = new JLabel("Attached device:");
        JLabel lblPort1LBL = new JLabel("CPU port1: ");
        JLabel lblPort2LBL = new JLabel("CPU port2: ");

        lblPort1 = new JLabel("0x10");
        lblPort2 = new JLabel("0x11");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("88-SIO run-time");

        lblPort1.setFont(lblPort1.getFont().deriveFont(lblPort1.getFont().getStyle() | java.awt.Font.BOLD));
        lblPort2.setFont(lblPort2.getFont().deriveFont(lblPort2.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addComponent(lblAttached)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
                .addGroup(layout.createSequentialGroup()
                .addComponent(lblPort1LBL)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPort1))
                .addGroup(layout.createSequentialGroup()
                .addComponent(lblPort2LBL)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPort2)))
                .addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblAttached))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblPort1LBL)
                .addComponent(lblPort1))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblPort2LBL)
                .addComponent(lblPort2))
                .addContainerGap());
        pack();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel lblPort1;
    private JLabel lblPort2;
    // End of variables declaration//GEN-END:variables
}
