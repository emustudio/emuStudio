/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.WindowEvent;

public class DisplayWindow extends JDialog {
    public final static int MARGIN = 30;

    private final static int BOUND_X = (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_WIDTH + 2 * MARGIN);
    private final static int BOUND_Y = (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_HEIGHT + 2 * MARGIN);

    private final DisplayCanvas canvas;
    private final ULA ula;
    private final KeyboardCanvas keyboardCanvas = new KeyboardCanvas(0);

    public DisplayWindow(JFrame parent, ULA ula) {
        super(parent);
        this.ula = ula;
        this.canvas = new DisplayCanvas(ula, keyboardCanvas);

        initComponents();
        setLocationRelativeTo(parent);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(WindowEvent winEvt) {
                canvas.ensureStarted();
                canvas.redrawNow();
            }

            public void windowActivated(WindowEvent winEvt) {
                canvas.ensureStarted();
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

        JLabel lblOpacity = new JLabel("Keyboard opacity:");
        JLabel lblOpacityPercent = new JLabel(keyboardCanvas.getAlpha() + "%");

        JSlider sliderOpacity = new JSlider(0, 100, keyboardCanvas.getAlpha());
        sliderOpacity.addChangeListener(e -> {
            int value = sliderOpacity.getValue();
            keyboardCanvas.setAlpha(value);
            lblOpacityPercent.setText(value + "%");
            if (keyboardCanvas.isInteractiveInvisible()) {
                keyboardCanvas.releaseMouseKeys(ula);
            }
            canvas.ensureStarted();
            canvas.runPaintCycle();
        });

        JPanel statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(lblOpacity);
        statusBar.add(sliderOpacity);
        statusBar.add(lblOpacityPercent);

        JPanel content = GUI.panel("insets 0", "[grow]", "[grow]0[46!]");
        content.add(canvas, "grow, wrap");
        content.add(statusBar, "growx");

        setContentPane(content);
        pack();
    }

    public DisplayCanvas getCanvas() {
        return canvas;
    }
}
