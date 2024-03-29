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
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.api.Display;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static java.awt.FlowLayout.LEFT;
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class TerminalWindow extends JDialog {
    private static final String BACKGROUND_IMAGE = "/net/emustudio/plugins/device/adm3a/gui/display.png";
    private static final String CLEAR_SCREEN_ICON = "/net/emustudio/plugins/device/adm3a/gui/clear.png";
    private static final String ROLL_LINE_ICON = "/net/emustudio/plugins/device/adm3a/gui/roll.png";

    private final Display display;
    private final DisplayCanvas canvas;

    public TerminalWindow(JFrame parent, Display display, DisplayFont font) {
        super(parent);
        this.display = Objects.requireNonNull(display);
        this.canvas = new DisplayCanvas(font, display);

        initComponents();
        setVisible(false);
        setLocationRelativeTo(parent);
    }

    public void startPainting() {
        this.canvas.start();
    }

    public void clearScreen() {
        display.clearScreen();
        canvas.repaint();
    }

    public void destroy() {
        this.dispose();
        this.canvas.close();
    }

    public void rollLine() {
        display.rollLine();
        canvas.repaint();
    }

    public void setDisplayFont(DisplayFont displayFont) {
        canvas.setDisplayFont(displayFont);
    }

    private void initComponents() {
        JLabel lblBack = new JLabel();
        Icon backgroundImage = loadIcon(BACKGROUND_IMAGE);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("LSI ADM-3A");
        setResizable(false);

        // 14pt original font: 9px 1 glyph
        canvas.setBounds(150, 170, 725, 530);

        lblBack.setLocation(0, 0);
        lblBack.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        lblBack.setIcon(backgroundImage);
        lblBack.setFocusable(false);
        lblBack.setOpaque(true);
        lblBack.setBackground(Color.BLACK);
        lblBack.setBounds(0, 0, backgroundImage.getIconWidth(), backgroundImage.getIconHeight());
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

        FlowLayout panelLayout = new FlowLayout(LEFT);
        panelControl.setLayout(panelLayout);
        panelControl.setBounds(0, 790, 150, 70);

        panelControl.add(btnClear);
        panelControl.add(btnRoll);

        Container pane = getContentPane();
        pane.setBackground(Color.BLACK);
        pane.setLayout(null);

        pane.add(panelControl);
        pane.add(canvas);
        pane.add(lblBack);
        pane.setPreferredSize(new Dimension(backgroundImage.getIconWidth(), backgroundImage.getIconHeight()));

        pack();
    }
}
