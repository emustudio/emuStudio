/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.mits88sio.UART;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class SioGui extends DialogBase {
    private final static Font FONT_BOLD_14 = new Font("sansserif", Font.BOLD, 14);
    private final static Font FONT_MONOSPACED_BOLD_14 = new Font("Monospaced", Font.BOLD, 14);

    private final UART uart;
    private final JButton btnClearBuffer = new JButton("Clear buffer");
    private final JLabel lblData = new JLabel("0x00");
    private final JLabel lblDataAscii = new JLabel("empty");
    private final JLabel lblStatus = new JLabel("0x00");
    private final JLabel lblStatusLong = new JLabel(". . . . . . . .");
    private final JTextField txtAttachedDevice = new JTextField();

    public SioGui(JFrame parent, UART uart) {
        super(parent, "MITS 88-SIO", false);

        this.uart = Objects.requireNonNull(uart);
        setResizable(false);

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

        buildContent();
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

    @Override
    protected JComponent initializeComponents() {
        // Attached device section
        txtAttachedDevice.setEditable(false);
        txtAttachedDevice.setFont(FONT_BOLD_14);

        JPanel panelAttachedDevice = GUI.section("Attached device", "insets dialog, fill", "[grow]", "[]");
        panelAttachedDevice.add(txtAttachedDevice, "growx, h 43!");

        // Control channel section
        lblStatusLong.setFont(FONT_MONOSPACED_BOLD_14);
        lblStatusLong.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatusLong.setBorder(BorderFactory.createEtchedBorder());

        JPanel panelControl = GUI.section("Control channel", "insets dialog", "[grow]", "[][pref!][][][][][][]");
        panelControl.add(GUI.label("<html>Control channel shows intermal status of 88-SIO."), "growx, h 46!, wrap");
        panelControl.add(lblStatusLong, "growx, h 33!, wrap");

        panelControl.add(GUI.label("Hex value:"), "split 2");
        panelControl.add(lblStatus, "wrap");

        panelControl.add(new JSeparator(), "growx, wrap");

        JLabel lblR = GUI.labelBold("R");
        JLabel lblD = GUI.labelBold("D");
        JLabel lblO = GUI.labelBold("O");
        JLabel lblX = GUI.labelBold("X");
        JLabel lblI = GUI.labelBold("I");

        panelControl.add(lblR, "split 2");
        panelControl.add(GUI.label("Output device ready"), "wrap");
        panelControl.add(lblD, "split 2");
        panelControl.add(GUI.label("Data available"), "wrap");
        panelControl.add(lblO, "split 2");
        panelControl.add(GUI.label("Data overflow"), "wrap");
        panelControl.add(lblX, "split 2");
        panelControl.add(GUI.label("Data sent to x-mitter"), "wrap");
        panelControl.add(lblI, "split 2");
        panelControl.add(GUI.label("Input device ready"));

        // Data buffer section
        lblDataAscii.setFont(FONT_BOLD_14);
        lblDataAscii.setHorizontalAlignment(SwingConstants.CENTER);
        lblDataAscii.setBorder(BorderFactory.createEtchedBorder());

        btnClearBuffer.setDefaultCapable(false);
        btnClearBuffer.addActionListener(e -> uart.readBuffer());

        JPanel panelData = GUI.section("Data buffer", "insets dialog", "[grow]", "[][pref!][][grow][]");
        panelData.add(GUI.label("<html>Data buffer is an internal buffer to be read by CPU."), "growx, h 46!, wrap");
        panelData.add(lblDataAscii, "growx, h 33!, wrap");
        panelData.add(GUI.label("Hex value:"), "split 2");
        panelData.add(lblData, "wrap");
        panelData.add(new JPanel(), "grow, wrap"); // spacer
        panelData.add(btnClearBuffer, "align right");

        // Main layout
        JPanel content = GUI.panel("insets dialog", "[grow][grow]", "[][grow]");
        content.add(panelAttachedDevice, "span, growx, wrap");
        content.add(panelControl, "grow");
        content.add(panelData, "grow");

        return content;
    }
}
