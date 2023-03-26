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
package net.emustudio.plugins.device.zxspectrum.display.gui;

import net.emustudio.plugins.device.zxspectrum.display.ULA;

import javax.swing.*;

public class TerminalWindow extends JDialog {
    private final DisplayCanvas canvas;

    public TerminalWindow(JFrame parent, ULA ula) {
        super(parent);
        this.canvas = new DisplayCanvas(ula);

        initComponents();
        setLocationRelativeTo(parent);
        canvas.start();
    }

    public void destroy() {
        this.canvas.close();
        this.dispose();
    }

    private void initComponents() {
        JPanel panelStatus = new JPanel();

        setTitle("ZX Spectrum48K");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(
                0, 0,
                (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_WIDTH),
                (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_HEIGHT));

        btnRedraw.setFocusable(false);

        GroupLayout panelStatusLayout = new GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelStatusLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnRedraw, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        panelStatusLayout.setVerticalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnRedraw, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(canvas)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(canvas, GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        pack();
        btnRedraw.addActionListener(e -> canvas.redrawNow());
    }

    private final JButton btnRedraw = new JButton("Redraw screen");
}
