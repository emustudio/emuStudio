/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.WindowEvent;

import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.SCREEN_IMAGE_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.SCREEN_IMAGE_WIDTH;

public class DisplayWindow extends JDialog {
    public final static int MARGIN = 30;

    private final static int BOUND_X = (int) (DisplayCanvas.ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN);
    private final static int BOUND_Y = (int) (DisplayCanvas.ZOOM * SCREEN_IMAGE_HEIGHT + 2 * MARGIN);

    private final DisplayCanvas canvas;
    private final KeyboardCanvas keyboardCanvas = new KeyboardCanvas(0);
    private final JPanel statusBar = new JPanel();

    public DisplayWindow(JFrame parent, ULA ula) {
        super(parent);
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

        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));

        JLabel lblOpacity = new JLabel("Keyboard opacity:");
        lblOpacity.setHorizontalAlignment(SwingConstants.LEFT);
        statusBar.add(lblOpacity);

        JSlider sliderOpacity = new JSlider();
        sliderOpacity.setMinimum(0);
        sliderOpacity.setMaximum(100);
        sliderOpacity.setValue(keyboardCanvas.getAlpha());
        statusBar.add(sliderOpacity);

        JLabel lblOpacityPercent = new JLabel(keyboardCanvas.getAlpha() + "%");
        statusBar.add(lblOpacityPercent);
        sliderOpacity.addChangeListener(e -> {
            int value = sliderOpacity.getValue();
            keyboardCanvas.setAlpha(value);
            lblOpacityPercent.setText(value + "%");
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(canvas)
                        .addComponent(statusBar, GroupLayout.DEFAULT_SIZE, 400, 400)); // TODO: gap?
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(statusBar, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    public DisplayCanvas getCanvas() {
        return canvas;
    }
}
