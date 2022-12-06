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
package net.emustudio.plugins.device.mits88sio.gui;

import net.emustudio.plugins.device.mits88sio.UART;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class SioGui extends JDialog {

    public SioGui(JFrame parent, UART uart) {
        super(parent);

        initComponents();
        setLocationRelativeTo(parent);

        txtStatus.setText(String.format("0x%x", uart.getStatus()));

        lblAttachedDevice.setText(uart.getDeviceId());
        uart.addObserver(new UART.Observer() {
            @Override
            public void statusChanged(int status) {
                txtStatus.setText(Integer.toBinaryString(status));
            }

            @Override
            public void dataAvailable(byte data) {
                txtData.setText(String.format("0x%X", data & 0xFF));
                txtDataDisplay.setText(String.valueOf(data));
            }

            @Override
            public void noData() {
                txtData.setText("N/A");
                txtDataDisplay.setText("");
            }
        });
    }

    private void initComponents() {
        JPanel panelDevice = new JPanel();
        JPanel panelPorts = new JPanel();
        JLabel lblStatus = new JLabel("Status:");
        JLabel lblData = new JLabel("Data:");
        JLabel lblDataDisplay = new JLabel("Data char:");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("MITS 88-SIO Status");

        panelDevice.setBorder(BorderFactory.createTitledBorder("Attached device"));

        lblAttachedDevice.setFont(lblAttachedDevice.getFont().deriveFont(14.0f));
        lblAttachedDevice.setHorizontalAlignment(SwingConstants.CENTER);

        GroupLayout jPanel1Layout = new GroupLayout(panelDevice);
        panelDevice.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblAttachedDevice, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblAttachedDevice)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelPorts.setBorder(BorderFactory.createTitledBorder("88-SIO Ports (for reading)"));

        txtDataDisplay.setEditable(false);
        txtStatus.setEditable(false);
        txtData.setEditable(false);

        GroupLayout jPanel2Layout = new GroupLayout(panelPorts);
        panelPorts.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblStatus)
                        .addComponent(lblData)
                        .addComponent(lblDataDisplay))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtStatus)
                        .addComponent(txtData, GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                        .addComponent(txtDataDisplay, GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblStatus)
                        .addComponent(txtStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(txtData, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblDataDisplay)
                        .addComponent(txtDataDisplay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelDevice, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelPorts, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panelDevice, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(panelPorts, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private final JLabel lblAttachedDevice = new JLabel("None");
    private final JTextField txtData = new JTextField("N/A");
    private final JTextField txtDataDisplay = new JTextField();
    private final JTextField txtStatus = new JTextField();
}
