/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.SCREEN_IMAGE_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.SCREEN_IMAGE_WIDTH;

public class DisplayWindow extends JDialog {
    public final static int MARGIN = 30;

    private final static int BOUND_X = (int) (DisplayCanvas.ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN);
    private final static int BOUND_Y = (int) (DisplayCanvas.ZOOM * SCREEN_IMAGE_HEIGHT + 2 * MARGIN);

    private final DisplayCanvas canvas;

    public DisplayWindow(JFrame parent, ULA ula) {
        super(parent);
        KeyboardCanvas keyboardCanvas = new KeyboardCanvas(70);
        this.canvas = new DisplayCanvas(ula, keyboardCanvas);

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
        KeyboardDispatcher keyboardDispatcher = new KeyboardDispatcher();
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(keyboardDispatcher);

        keyboardDispatcher.addOnKeyListener(keyboardCanvas);
        keyboardDispatcher.addOnKeyListener(ula);
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    private void initComponents() {
        setTitle("ZX Spectrum48K");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(MARGIN, MARGIN, BOUND_X, BOUND_Y);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(canvas));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                                .addContainerGap()));
        pack();
    }

    public DisplayCanvas getCanvas() {
        return canvas;
    }
}
