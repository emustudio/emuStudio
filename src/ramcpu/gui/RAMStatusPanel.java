/**
 * RAMStatusPanel.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package ramcpu.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;

import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.ICPU.ICPUListener;
import emuLib8.plugins.memory.IMemoryContext;

import ramcpu.impl.RAM;

@SuppressWarnings("serial")
public class RAMStatusPanel extends JPanel {

    public RAMStatusPanel(final RAM cpu, final IMemoryContext mem) {
        initComponents();

        cpu.addCPUListener(new ICPUListener() {

            @Override
            public void runChanged(EventObject evt, int state) {
                switch (state) {
                    case ICPU.STATE_STOPPED_NORMAL:
                        lblStatus.setText("stopped (normal)");
                        break;
                    case ICPU.STATE_STOPPED_BREAK:
                        lblStatus.setText("breakpoint");
                        break;
                    case ICPU.STATE_STOPPED_ADDR_FALLOUT:
                        lblStatus.setText("stopped (address fallout)");
                        break;
                    case ICPU.STATE_STOPPED_BAD_INSTR:
                        lblStatus.setText("stopped (instruction fallout)");
                        break;
                }

            }

            @Override
            public void stateUpdated(EventObject evt) {
                String s = cpu.getR0();
                if (s == null || s.equals("")) {
                    s = "<empty>";
                }
                txtR0.setText(s);
                txtIP.setText(String.format("%04d", cpu.getInstrPosition()));
            }
        });
    }

    private void initComponents() {
        JLabel lblR0 = new JLabel("R0");
        JLabel lblIP = new JLabel("IP");
        txtR0 = new JTextField("<empty>");
        txtIP = new JTextField("0000");
        lblStatus = new JLabel("breakpoint");

        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
        txtR0.setEditable(false);
        txtR0.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));
        txtIP.setEditable(false);
        txtIP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblR0).addComponent(lblIP)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtR0, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE).addComponent(txtIP, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE))).addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)).addContainerGap(20, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblR0).addComponent(txtR0)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblIP).addComponent(txtIP)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblStatus).addContainerGap());
    }
    private JTextField txtR0;
    private JTextField txtIP;
    private JLabel lblStatus;
}
