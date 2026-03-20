/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import net.emustudio.plugins.device.vt100.interaction.KeyboardGui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.StringTokenizer;

import static net.emustudio.plugins.device.vt100.gui.Constants.*;

public class TerminalWindow extends DialogBase {
    private final GUI gui;
    private final Dialogs dialogs;

    private final DisplayCanvas canvas;
    private final KeyboardGui keyboard;
    private final JLabel lblStatusIcon = new JLabel();
    private final JButton btnASCII = new JButton();

    public TerminalWindow(JFrame parent, DisplayImpl display, Dialogs dialogs, KeyboardGui keyboard, GUI gui) {
        super(parent, "VT100 Terminal", false);
        this.gui = gui;
        this.canvas = new DisplayCanvas(display);
        this.keyboard = Objects.requireNonNull(keyboard);
        this.dialogs = Objects.requireNonNull(dialogs);

        keyboard.addInputRequestHandler(inputRequested -> {
            if (inputRequested) {
                lblStatusIcon.setIcon(RED_ICON);
                lblStatusIcon.setToolTipText("Input requested from keyboard");
                btnASCII.setEnabled(true);
            } else {
                lblStatusIcon.setIcon(BLUE_ICON);
                lblStatusIcon.setToolTipText("Input not requested");
                btnASCII.setEnabled(false);
            }
        });

        buildContent();
        setLocationRelativeTo(parent);
    }

    public void startPainting() {
        this.canvas.start();
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    @Override
    protected boolean shouldCloseOnEscape() {
        return false;
    }

    @Override
    protected JComponent initializeComponents() {
        canvas.setBounds(0, 0, 900, 700);

        lblStatusIcon.setIcon(BLUE_ICON);
        lblStatusIcon.setToolTipText("Input not requested");
        lblStatusIcon.setVerticalAlignment(SwingConstants.TOP);

        btnASCII.setIcon(ASCII_ICON);
        btnASCII.setToolTipText("Input by ASCII code");
        btnASCII.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnASCII.setEnabled(false);
        btnASCII.setVerticalAlignment(SwingConstants.TOP);
        btnASCII.addActionListener(this::btnASCIIActionPerformed);

        JPanel panelStatus = gui.panel("insets 2 6 2 6", "[20!]6[]push", "[24!]");
        panelStatus.add(lblStatusIcon);
        panelStatus.add(btnASCII);

        JPanel content = gui.panel("insets 0", "[grow]", "[grow]0[]");
        content.add(canvas, "grow, wrap");
        content.add(panelStatus, "growx");

        return content;
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
