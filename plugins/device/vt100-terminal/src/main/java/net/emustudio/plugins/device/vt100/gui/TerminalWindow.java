/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import net.emustudio.plugins.device.vt100.interaction.KeyboardGui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.StringTokenizer;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class TerminalWindow extends JDialog {
    private final Dialogs dialogs;

    private final ImageIcon blueIcon; // not waiting for input
    private final ImageIcon redIcon; // waiting for input

    private final DisplayCanvas canvas;
    private final KeyboardGui keyboard;
    private final JLabel lblStatusIcon = new JLabel();
    private final JButton btnASCII = new JButton();

    public TerminalWindow(JFrame parent, DisplayImpl display, Dialogs dialogs, KeyboardGui keyboard) {
        super(parent);
        this.canvas = new DisplayCanvas(display);
        this.keyboard = Objects.requireNonNull(keyboard);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.blueIcon = loadIcon("/net/emustudio/plugins/device/vt100/16_circle_blue.png");
        this.redIcon = loadIcon("/net/emustudio/plugins/device/vt100/16_circle_red.png");

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
        setTitle("VT100 Terminal");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(0, 0, 900, 700);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Input not requested");
        lblStatusIcon.setVerticalAlignment(SwingConstants.TOP);

        btnASCII.setIcon(loadIcon("/net/emustudio/plugins/device/vt100/16_ascii.png"));
        btnASCII.setToolTipText("Input by ASCII code");
        btnASCII.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnASCII.setEnabled(false);
        btnASCII.setVerticalAlignment(SwingConstants.TOP);
        btnASCII.addActionListener(this::btnASCIIActionPerformed);

        JPanel panelStatus = GUI.panel("insets 2 6 2 6", "[20!]6[]push", "[24!]");
        panelStatus.add(lblStatusIcon);
        panelStatus.add(btnASCII);

        JPanel content = GUI.panel("insets 0", "[grow]", "[grow]0[]");
        content.add(canvas, "grow, wrap");
        content.add(panelStatus, "growx");

        setContentPane(content);
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
