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
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import javax.swing.*;
import java.awt.event.WindowEvent;

public class DisplayWindow extends JDialog {
    private final DisplayCanvas canvas;
    private final KeyboardCanvas keyboardCanvas;

    public final static int MARGIN = 30;

    public DisplayWindow(JFrame parent, ULA ula, Keyboard keyboard) {
        super(parent);
        this.canvas = new DisplayCanvas(ula);
        this.keyboardCanvas = new KeyboardCanvas(keyboard);

        initComponents();
        setLocationRelativeTo(parent);
        canvas.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(WindowEvent winEvt) {
                canvas.redrawNow();
            }

            public void windowActivated(WindowEvent winEvt) {
                canvas.redrawNow();
            }
        });
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    private void initComponents() {
        setTitle("ZX Spectrum48K");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(
                MARGIN, MARGIN,
                (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_WIDTH + 2 * MARGIN),
                (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_HEIGHT + 2 * MARGIN));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(canvas)
                        .addComponent(keyboardCanvas));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keyboardCanvas, GroupLayout.DEFAULT_SIZE, KeyboardCanvas.KEYBOARD_HEIGHT + 3, Short.MAX_VALUE)
                                .addContainerGap()));
        pack();
    }
}
