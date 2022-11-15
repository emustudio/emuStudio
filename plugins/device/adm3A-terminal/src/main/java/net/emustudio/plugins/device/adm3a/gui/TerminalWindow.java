/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.plugins.device.adm3a.interaction.Display;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TerminalWindow extends JDialog {
    private static final String BACKGROUND_IMAGE = "/net/emustudio/plugins/device/adm3a/gui/display.png";
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
        this.canvas.close();
        this.display.close();
        this.dispose();
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
        ImageIcon backgroundImage = new ImageIcon(getClass().getResource(BACKGROUND_IMAGE));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("LSI ADM-3A");
        setResizable(false);

        canvas.setBounds(100, 170, 830, 530);

        lblBack.setLocation(0, 0);
        lblBack.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        lblBack.setIcon(backgroundImage);
        lblBack.setFocusable(false);
        lblBack.setOpaque(true);
        lblBack.setBackground(Color.BLACK);
        lblBack.setBounds(0, 0, backgroundImage.getIconWidth(), backgroundImage.getIconHeight());
        lblBack.setDoubleBuffered(true);

        Container pane = getContentPane();
        pane.setBackground(Color.BLACK);
        pane.setLayout(null);

        pane.add(canvas);
        pane.add(lblBack);
        pane.setPreferredSize(new Dimension(backgroundImage.getIconWidth(), backgroundImage.getIconHeight()));

        pack();
    }
}
