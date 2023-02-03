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
package net.emustudio.plugins.device.zxspectrum.display;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.plugins.device.zxspectrum.display.io.*;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Objects;
import java.util.StringTokenizer;

class ZxSpectrumDisplayGui extends JDialog implements OutputProvider {
    private final Dialogs dialogs;

    private final ImageIcon blueIcon;
    private final ImageIcon redIcon;

    private final Display canvas;
    private final Keyboard keyboard;
    boolean ignoreNext = false;
    int ignoreCount = 0;
    boolean sposx = false;
    boolean sposy = false;
    int posX = 0;
    int posY = 0;
    private JLabel lblStatusIcon;
    private JScrollPane scrollPane;

    private ZxSpectrumDisplayGui(JFrame parent, Keyboard keyboard, Dialogs dialogs) {
        super(parent);
        this.dialogs = Objects.requireNonNull(dialogs);

        URL blueIconURL = getClass().getResource(
                "/net/emustudio/plugins/device/zxspectrum/display/16_circle_blue.png"
        );
        URL redIconURL = getClass().getResource(
                "/net/emustudio/plugins/device/zxspectrum/display/16_circle_red.png"
        );

        blueIcon = new ImageIcon(Objects.requireNonNull(blueIconURL));
        redIcon = new ImageIcon(Objects.requireNonNull(redIconURL));

        this.keyboard = keyboard;

        setTitle("ZX Spectrum Terminal");
        initComponents();
        setLocationRelativeTo(parent);

        canvas = new Display();
        scrollPane.setViewportView(canvas);
        canvas.start();
    }

    static ZxSpectrumDisplayGui create(JFrame parent, Keyboard keyboard, Dialogs dialogs) {
        ZxSpectrumDisplayGui dialog = new ZxSpectrumDisplayGui(parent, keyboard, dialogs);
        GuiUtils.addKeyListener(dialog, dialog.keyboard);
        return dialog;
    }

    @Override
    public void reset() {
        canvas.clearScreen();
    }

    @Override
    public void write(int character) {
        if (ignoreNext && (--ignoreCount) > 0) {
            return;
        }
        ignoreNext = false;

        writeStarted();
        Cursor cursor = canvas.getTextCanvasCursor();

        if (sposy) {
            posY = character;
            sposy = false;
            sposx = true;
            return;
        }
        if (sposx) {
            posX = character;
            sposx = false;
            //  cursor.set(posX, posY);
            return;
        }


        switch (character) {
            case 5: // HERE IS
                // insertHereIs();
                break;
            case 6:
                // print COMMA
                int x = cursor.getCursorPoint().x;
                int y = cursor.getCursorPoint().y;
                cursor.moveForwardsTab();
                int x1 = cursor.getCursorPoint().x;
                for (; x < x1; x++) {
                    canvas.writeCharAt(' ', x, y);
                }
                break;
            case 7: // BELL
                canvas.writeAtCursor('?');
                break;
            case 8: // BACKSPACE
                cursor.moveBackwards();
                break;
            case 9:
                cursor.moveForwards();
                break;
            case 0x0A: // line feed
                cursor.moveDown(canvas);
                break;
            case 0x0B: // VT
                cursor.moveUp();
                break;
            case 0x0C: // delete
                canvas.writeAtCursor(' ');
                break;
            case 0x0D: // CARRIAGE RETURN
                cursor.carriageReturn();
                cursor.moveDown(canvas);
                break;
            case 0x10: // INK CONTROL
            case 0x11: // PAPER CONTROL
            case 0x12: // FLASH CONTROL
            case 0x13: // BRIGHT CONTROL
            case 0x14: // INVERSE CONTROL
            case 0x15: // OVER CONTROL
                ignoreCount = 1;
                ignoreNext = true;
                break;
            case 0x16: // AT CONTROL
                sposy = true;
                break;
            case 0x17: // 23
                ignoreCount = 1;
                ignoreNext = true;
                cursor.moveForwardsTab();
                break;

            case 127:
                canvas.writeAtCursor('\u00a9');
                cursor.moveForwardsRolling(canvas);
                break;

            case 0x82:
                canvas.writeAtCursor('\u2590');
                return;
            case 0x83:
                canvas.writeAtCursor('\u2580');
                return;
            case 0x85:
                canvas.writeAtCursor('\u2599');
                return;
            case 0x86:
                canvas.writeAtCursor('\u259C');
                return;
            case 0x89:
            case 0x8C:
            case 0x8A:
                canvas.writeAtCursor(' '); // Ᾱ0
                return;
            case 0xDE:
                canvas.writeAtCursor('*');
                return;
            case 0xFB:
                canvas.clearScreen();
                return;
        }

        if (character >= 32 && character <= 0x7F) {
            canvas.writeAtCursor((char) character);
            cursor.moveForwardsRolling(canvas);
        }
        repaint();
    }

    @Override
    public void showGUI() {
        this.setVisible(true);
    }

    @Override
    public void close() {
        canvas.stop();
        GuiUtils.removeKeyListener(this, keyboard);
        dispose();
    }

    private void writeStarted() {
        lblStatusIcon.setIcon(redIcon);
        lblStatusIcon.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    private void initComponents() {
        JPanel panelStatus = new JPanel();
        lblStatusIcon = new JLabel();
        JButton btnASCII = new JButton();
        scrollPane = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Waiting for input? (red - yes, blue - no)");
        lblStatusIcon.setVerticalAlignment(SwingConstants.TOP);

        btnASCII.setFont(btnASCII.getFont());
        btnASCII.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/device/zxspectrum/display/16_ascii.png")));
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
                                .addContainerGap(1000, Short.MAX_VALUE))
        );
        panelStatusLayout.setVerticalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblStatusIcon, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnASCII, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
        );

        scrollPane.setBackground(new java.awt.Color(255, 255, 255));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(scrollPane)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
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