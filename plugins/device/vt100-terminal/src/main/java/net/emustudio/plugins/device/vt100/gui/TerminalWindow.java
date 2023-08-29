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
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import net.emustudio.plugins.device.vt100.interaction.KeyboardGui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Objects;
import java.util.StringTokenizer;

public class TerminalWindow extends JDialog {
    private final Dialogs dialogs;

    private final ImageIcon blueIcon; // not waiting for input
    private final ImageIcon redIcon; // waiting for input

    private final DisplayCanvas canvas;
    private final KeyboardGui keyboard;

    public TerminalWindow(JFrame parent, DisplayImpl display, Dialogs dialogs, KeyboardGui keyboard) {
        super(parent);
        this.canvas = new DisplayCanvas(display);
        this.keyboard = Objects.requireNonNull(keyboard);
        this.dialogs = Objects.requireNonNull(dialogs);

        URL blueIconURL = ClassLoader.getSystemResource(
                "/net/emustudio/plugins/device/vt100/16_circle_blue.png"
        );
        URL redIconURL = ClassLoader.getSystemResource(
                "/net/emustudio/plugins/device/vt100/16_circle_red.png"
        );

        blueIcon = new ImageIcon(Objects.requireNonNull(blueIconURL));
        redIcon = new ImageIcon(Objects.requireNonNull(redIconURL));

        initComponents();
        setLocationRelativeTo(parent);

        keyboard.addInputRequestHandler(inputRequested -> {
            if (inputRequested) {
                lblStatusIcon.setIcon(redIcon);
                lblStatusIcon.setToolTipText("Input requested from keyboard");
                btnASCII.setEnabled(true);
            } else {
                lblStatusIcon.setIcon(blueIcon);
                lblStatusIcon.setToolTipText("Input not requested");
                btnASCII.setEnabled(false);
            }
        });
    }

    public void startPainting() {
        this.canvas.start();
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    private void initComponents() {
        JPanel panelStatus = new JPanel();

        setTitle("VT100 Terminal");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(0, 0, 900, 700);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Input not requested");
        lblStatusIcon.setVerticalAlignment(SwingConstants.TOP);

        btnASCII.setFont(btnASCII.getFont());
        btnASCII.setIcon(new ImageIcon(ClassLoader.getSystemResource("/net/emustudio/plugins/device/vt100/16_ascii.png")));
        btnASCII.setToolTipText("Input by ASCII code");
        btnASCII.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnASCII.setEnabled(false);
        btnASCII.setVerticalAlignment(SwingConstants.TOP);
        btnASCII.addActionListener(this::btnASCIIActionPerformed);

        GroupLayout panelStatusLayout = new GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelStatusLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblStatusIcon, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnASCII)
                                .addContainerGap(900, Short.MAX_VALUE))
        );
        panelStatusLayout.setVerticalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblStatusIcon, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnASCII, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(canvas)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }

    private void btnASCIIActionPerformed(java.awt.event.ActionEvent evt) {
        dialogs
                .readString("Enter ASCII codes separated with spaces:", "Add ASCII codes")
                .ifPresent(asciiCodes -> {
                    StringTokenizer tokenizer = new StringTokenizer(asciiCodes);

                    RadixUtils radixUtils = RadixUtils.getInstance();
                    try {
                        while (tokenizer.hasMoreTokens()) {
                            int ascii = radixUtils.parseRadix(tokenizer.nextToken());
                            keyboard.keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, ascii, (char) ascii));
                        }
                    } catch (NumberFormatException ex) {
                        dialogs.showError("Invalid number format in the input: " + ex.getMessage(), "Add ASCII codes");
                    }
                });
    }

    private final JLabel lblStatusIcon = new JLabel();
    private final JButton btnASCII = new JButton();
}
