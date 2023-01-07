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
package net.emustudio.plugins.device.brainduck.terminal.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.brainduck.terminal.interaction.Display;
import net.emustudio.plugins.device.brainduck.terminal.interaction.KeyboardGui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Objects;
import java.util.StringTokenizer;

public class TerminalWindow extends JDialog {
    private final Dialogs dialogs;

    private final ImageIcon blueIcon;
    private final ImageIcon redIcon;
    private final ImageIcon greenIcon;

    private final Display display;
    private final DisplayCanvas canvas;
    private final KeyboardGui keyboard;

    public TerminalWindow(JFrame parent, Display display, Dialogs dialogs, KeyboardGui keyboard) {
        super(parent);
        this.display = Objects.requireNonNull(display);
        this.canvas = new DisplayCanvas(display);
        this.keyboard = Objects.requireNonNull(keyboard);
        this.dialogs = Objects.requireNonNull(dialogs);

        URL blueIconURL = getClass().getResource(
                "/net/emustudio/plugins/device/brainduck/terminal/16_circle_blue.png"
        );
        URL redIconURL = getClass().getResource(
                "/net/emustudio/plugins/device/brainduck/terminal/16_circle_red.png"
        );
        URL greenIconURL = getClass().getResource(
                "/net/emustudio/plugins/device/brainduck/terminal/16_circle_green.png"
        );

        blueIcon = new ImageIcon(Objects.requireNonNull(blueIconURL));
        redIcon = new ImageIcon(Objects.requireNonNull(redIconURL));
        greenIcon = new ImageIcon(Objects.requireNonNull(greenIconURL));

        initComponents();
        setLocationRelativeTo(parent);
    }

    public void startPainting() {
        this.canvas.start();
    }

    public void destroy() {
        this.canvas.close();
        this.display.close();
        this.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    private void initComponents() {
        JPanel panelStatus = new JPanel();
        JLabel lblStatusIcon = new JLabel();
        JButton btnASCII = new JButton();

        setTitle("BrainDuck Terminal");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(0, 0, 900, 700);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Waiting for input? (red - yes, blue - no)");
        lblStatusIcon.setVerticalAlignment(SwingConstants.TOP);

        btnASCII.setFont(btnASCII.getFont());
        btnASCII.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/device/brainduck/terminal/16_ascii.png")));
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
}