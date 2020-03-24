/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
    private final Display display;

    public TerminalWindow(JFrame parent, Display display) {
        super(parent);
        this.display = Objects.requireNonNull(display);

        initComponents();
        setVisible(false);
        this.setLocationRelativeTo(null);
    }

    public void destroy() {
        this.dispose();
    }

    private void initComponents() {
        JLabel lblBack = new JLabel();
        ImageIcon img = new ImageIcon(getClass().getResource("/net/emustudio/plugins/device/adm3a/gui/display.gif"));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Terminal ADM-3A");
        setResizable(false);

        display.setBounds(53, 60, 653, 400);

        lblBack.setLocation(0, 0);
        lblBack.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); // NOI18N
        lblBack.setIcon(img); // NOI18N
        lblBack.setFocusable(false);

        lblBack.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());

        Container pane = getContentPane();
        pane.setBackground(Color.BLACK);
        pane.setLayout(null);

        pane.add(display);
        pane.add(lblBack);
        pack();
        setSize(img.getIconWidth(), img.getIconHeight());
    }
}
