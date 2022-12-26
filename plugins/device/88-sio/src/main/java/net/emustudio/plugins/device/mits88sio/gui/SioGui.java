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
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class SioGui extends JDialog {
    private final static Font FONT_BOLD_14 = new Font("sansserif", Font.BOLD, 14);
    private final static Font FONT_BOLD_13 = new Font("sansserif", Font.BOLD, 13);
    private final static Font FONT_MONOSPACED_BOLD_14 = new Font("Monospaced", Font.BOLD, 14);

    private final UART uart;
    private final JButton btnClearBuffer = new JButton("Clear buffer");
    private final JLabel lblData = new JLabel("0x00");
    private final JLabel lblDataAscii = new JLabel("empty");
    private final JLabel lblStatus = new JLabel("0x00");
    private final JLabel lblStatusLong = new JLabel(". . . . . . . .");
    private final JTextField txtAttachedDevice = new JTextField();
    public SioGui(JFrame parent, UART uart) {
        super(parent);

        this.uart = Objects.requireNonNull(uart);

        initComponents();
        setLocationRelativeTo(parent);

        setStatus(uart.getStatus());

        txtAttachedDevice.setText(uart.getDeviceId());
        uart.addObserver(new UART.Observer() {
            @Override
            public void statusChanged(int status) {
                setStatus(status);
            }

            @Override
            public void dataAvailable(byte data) {
                lblData.setText(String.format("0x%x", data & 0xFF));
                lblDataAscii.setText(String.valueOf(data));
            }

            @Override
            public void noData() {
                lblData.setText("0x00");
                lblDataAscii.setText("empty");
            }
        });
    }

    private void setStatus(int status) {
        lblStatus.setText(String.format("0x%x", status));
        String r = ((status & 0x80) == 0) ? "R . " : ". . ";
        String d = ((status & 0x20) == 0x20) ? "D " : ". ";
        String o = ((status & 0x10) == 0x10) ? "O . . " : ". . . ";
        String x = ((status & 0x2) == 0x2) ? "X " : ". ";
        String i = ((status & 0x1) == 0) ? "I" : ".";
        lblStatusLong.setText(r + d + o + x + i);
    }

    private void initComponents() {
        JPanel panelAttachedDevice = new JPanel();
        JPanel panelControl = new JPanel();
        JLabel lblNoteControl = new JLabel("<html>Control channel shows intermal status of 88-SIO.");
        JLabel lblHexControl = new JLabel("Hex value:");
        JSeparator sepControl = new JSeparator();
        JLabel lblR = new JLabel("R");
        JLabel lblD = new JLabel("D");
        JLabel lblO = new JLabel("O");
        JLabel lblX = new JLabel("X");
        JLabel lblI = new JLabel("I");
        JLabel lblNoteR = new JLabel("Output device ready");
        JLabel lblNoteD = new JLabel("Data available");
        JLabel lblNoteO = new JLabel("Data overflow");
        JLabel lblNoteX = new JLabel("Data sent to x-mitter");
        JLabel lblNoteI = new JLabel("Input device ready");
        JPanel panelData = new JPanel();
        JLabel lblHexData = new JLabel("Hex value:");
        JLabel lblNoteData = new JLabel("<html>Data buffer is an internal buffer to be read by CPU.");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        rootPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setResizable(false);
        setTitle("MITS 88-SIO");

        panelAttachedDevice.setBorder(BorderFactory.createTitledBorder(
                null, "Attached device", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                FONT_BOLD_13));

        txtAttachedDevice.setEditable(false);
        txtAttachedDevice.setFont(FONT_BOLD_14);

        GroupLayout panelAttachedDeviceLayout = new GroupLayout(panelAttachedDevice);
        panelAttachedDevice.setLayout(panelAttachedDeviceLayout);
        panelAttachedDeviceLayout.setHorizontalGroup(
                panelAttachedDeviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelAttachedDeviceLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txtAttachedDevice)
                                .addContainerGap())
        );
        panelAttachedDeviceLayout.setVerticalGroup(
                panelAttachedDeviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelAttachedDeviceLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txtAttachedDevice, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelControl.setBorder(BorderFactory.createTitledBorder(
                null, "Control channel", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                FONT_BOLD_13));

        lblStatusLong.setFont(FONT_MONOSPACED_BOLD_14);
        lblStatusLong.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatusLong.setBorder(BorderFactory.createEtchedBorder());

        lblR.setFont(FONT_BOLD_14);
        lblD.setFont(FONT_BOLD_14);
        lblO.setFont(FONT_BOLD_14);
        lblX.setFont(FONT_BOLD_14);
        lblI.setFont(FONT_BOLD_14);

        GroupLayout panelControlLayout = new GroupLayout(panelControl);
        panelControl.setLayout(panelControlLayout);
        panelControlLayout.setHorizontalGroup(
                panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelControlLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, panelControlLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(lblStatusLong, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                                                        .addComponent(sepControl)
                                                        .addComponent(lblNoteControl, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                                        .addGroup(panelControlLayout.createSequentialGroup()
                                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelControlLayout.createSequentialGroup()
                                                                .addComponent(lblHexControl)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblStatus))
                                                        .addGroup(panelControlLayout.createSequentialGroup()
                                                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lblR)
                                                                        .addComponent(lblD)
                                                                        .addComponent(lblO)
                                                                        .addComponent(lblX)
                                                                        .addComponent(lblI))
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lblNoteX)
                                                                        .addComponent(lblNoteO)
                                                                        .addComponent(lblNoteR)
                                                                        .addComponent(lblNoteD)
                                                                        .addComponent(lblNoteI))))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        panelControlLayout.setVerticalGroup(
                panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelControlLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblNoteControl, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblStatusLong, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblHexControl)
                                        .addComponent(lblStatus))
                                .addGap(18, 18, 18)
                                .addComponent(sepControl, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelControlLayout.createSequentialGroup()
                                                .addGap(24, 24, 24)
                                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(lblD)
                                                        .addComponent(lblNoteD)))
                                        .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblNoteR)
                                                .addComponent(lblR)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblO)
                                        .addComponent(lblNoteO))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblNoteX)
                                        .addComponent(lblX))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblNoteI)
                                        .addComponent(lblI))
                                .addContainerGap(13, Short.MAX_VALUE))
        );

        panelData.setBorder(BorderFactory.createTitledBorder(
                null, "Data buffer", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                FONT_BOLD_13));

        lblDataAscii.setFont(FONT_BOLD_14);
        lblDataAscii.setHorizontalAlignment(SwingConstants.CENTER);
        lblDataAscii.setBorder(BorderFactory.createEtchedBorder());

        btnClearBuffer.setDefaultCapable(false);

        GroupLayout panelDataLayout = new GroupLayout(panelData);
        panelData.setLayout(panelDataLayout);
        panelDataLayout.setHorizontalGroup(
                panelDataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelDataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelDataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, panelDataLayout.createSequentialGroup()
                                                .addGap(0, 102, Short.MAX_VALUE)
                                                .addComponent(btnClearBuffer))
                                        .addGroup(panelDataLayout.createSequentialGroup()
                                                .addComponent(lblHexData)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblData)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(lblNoteData, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(lblDataAscii, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        panelDataLayout.setVerticalGroup(
                panelDataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelDataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblNoteData, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblDataAscii, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelDataLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblHexData)
                                        .addComponent(lblData))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnClearBuffer)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(panelAttachedDevice, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(panelControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(panelData, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelAttachedDevice, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(panelControl, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelData, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        btnClearBuffer.addActionListener(e -> uart.readBuffer());
        pack();
    }
}
