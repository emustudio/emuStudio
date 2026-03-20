/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.adm3a.api.Display;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static java.awt.FlowLayout.LEFT;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class TerminalWindow extends DialogBase {
    private static final String BACKGROUND_IMAGE = "/net/emustudio/plugins/device/adm3a/gui/display.png";
    private static final String CLEAR_SCREEN_ICON = "/net/emustudio/plugins/device/adm3a/gui/clear.png";
    private static final String ROLL_LINE_ICON = "/net/emustudio/plugins/device/adm3a/gui/roll.png";
    private static final Rectangle DEFAULT_CANVAS_BOUNDS = new Rectangle(150, 170, 725, 530);
    private static final int CANVAS_RIGHT_PADDING = 3;

    private final Display display;
    private final DisplayCanvas canvas;
    private volatile DisplayFont displayFont;

    public TerminalWindow(JFrame parent, Display display, DisplayFont font) {
        super(parent, "LSI ADM-3A", false);
        this.display = Objects.requireNonNull(display);
        this.displayFont = Objects.requireNonNull(font);
        this.canvas = new DisplayCanvas(font, display);

        setResizable(false);
        buildContent();
    }

    public void startPainting() {
        this.canvas.start();
    }

    public void clearScreen() {
        display.clearScreen();
        canvas.repaint();
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    public void rollLine() {
        display.rollLine();
        canvas.repaint();
    }

    public void setDisplayFont(DisplayFont displayFont) {
        this.displayFont = Objects.requireNonNull(displayFont);
        canvas.setDisplayFont(displayFont);
        updateCanvasBounds();
        canvas.repaint();
    }

    @Override
    protected boolean shouldCloseOnEscape() {
        return false;
    }

    @Override
    protected JComponent initializeComponents() {
        Icon backgroundImage = loadIcon(BACKGROUND_IMAGE);
        int bgWidth = backgroundImage.getIconWidth();
        int bgHeight = backgroundImage.getIconHeight();

        updateCanvasBounds();

        JLabel lblBack = new JLabel();
        lblBack.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        lblBack.setIcon(backgroundImage);
        lblBack.setFocusable(false);
        lblBack.setOpaque(true);
        lblBack.setBackground(Color.BLACK);
        lblBack.setBounds(0, 0, bgWidth, bgHeight);
        lblBack.setDoubleBuffered(true);

        JButton btnClear = new JButton(loadIcon(CLEAR_SCREEN_ICON));
        btnClear.setFocusable(false);
        btnClear.setToolTipText("Clear screen");
        btnClear.addActionListener(e -> clearScreen());

        JButton btnRoll = new JButton(loadIcon(ROLL_LINE_ICON));
        btnRoll.setFocusable(false);
        btnRoll.setToolTipText("Roll line up");
        btnRoll.addActionListener(e -> rollLine());

        JPanel panelControl = new JPanel();
        panelControl.setBorder(null);
        panelControl.setOpaque(false);
        panelControl.setLayout(new FlowLayout(LEFT));
        panelControl.setBounds(0, 790, 150, 70);

        panelControl.add(btnClear);
        panelControl.add(btnRoll);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBackground(Color.BLACK);
        layeredPane.setOpaque(true);
        layeredPane.setPreferredSize(new Dimension(bgWidth, bgHeight));

        layeredPane.add(lblBack, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(canvas, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(panelControl, JLayeredPane.PALETTE_LAYER);

        return layeredPane;
    }

    static Rectangle computeCanvasBounds(DisplayFont displayFont, int columns, int cellWidth) {
        int width = displayFont.xCursorOffset + (columns * cellWidth) + CANVAS_RIGHT_PADDING;
        int centerX = DEFAULT_CANVAS_BOUNDS.x + (DEFAULT_CANVAS_BOUNDS.width / 2);
        int x = centerX - (width / 2);
        return new Rectangle(x, DEFAULT_CANVAS_BOUNDS.y, width, DEFAULT_CANVAS_BOUNDS.height);
    }

    private void updateCanvasBounds() {
        int cellWidth = canvas.getFontMetrics(canvas.getFont()).charWidth('W');
        canvas.setBounds(computeCanvasBounds(displayFont, display.getColumns(), cellWidth));
    }
}
