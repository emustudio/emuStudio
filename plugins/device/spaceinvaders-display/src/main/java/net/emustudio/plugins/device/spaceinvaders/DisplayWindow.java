/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.JFrame;

final class DisplayWindow extends JFrame {
    private final DisplayPanel display;

    DisplayWindow(JFrame parent, MemoryContext<Byte> memory, SpaceInvadersHardware hardware,
                  int scale, boolean colorOverlay) {
        super("Space Invaders");
        display = new DisplayPanel(memory, hardware, scale, colorOverlay);
        add(display);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(parent);
    }

    void frameReady() {
        display.repaint();
    }
}
