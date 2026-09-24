/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

final class DisplayPanel extends JPanel {
    static final int WIDTH = 224;
    static final int HEIGHT = 256;
    static final int FRAMEBUFFER = 0x2400;

    private final MemoryContext<Byte> memory;
    private final boolean colorOverlay;
    private final int scale;
    private final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

    DisplayPanel(MemoryContext<Byte> memory, SpaceInvadersHardware hardware, int scale, boolean colorOverlay) {
        this.memory = memory;
        this.scale = Math.max(1, scale);
        this.colorOverlay = colorOverlay;
        setPreferredSize(new Dimension(WIDTH * this.scale, HEIGHT * this.scale));
        setBackground(Color.BLACK);
        setFocusable(true);
        bind(hardware, KeyEvent.VK_C, SpaceInvadersHardware.COIN, 1);
        bind(hardware, KeyEvent.VK_1, SpaceInvadersHardware.START_1, 1);
        bind(hardware, KeyEvent.VK_2, SpaceInvadersHardware.START_2, 1);
        bind(hardware, KeyEvent.VK_SPACE, SpaceInvadersHardware.FIRE, 1);
        bind(hardware, KeyEvent.VK_LEFT, SpaceInvadersHardware.LEFT, 1);
        bind(hardware, KeyEvent.VK_RIGHT, SpaceInvadersHardware.RIGHT, 1);
        bind(hardware, KeyEvent.VK_T, SpaceInvadersHardware.TILT, 2);
    }

    static boolean pixelOn(MemoryContext<Byte> memory, int x, int y) {
        int hardwareY = HEIGHT - 1 - y;
        int address = FRAMEBUFFER + x * 32 + hardwareY / 8;
        return ((memory.read(address) & 0xFF) & (1 << (hardwareY & 7))) != 0;
    }

    private void bind(SpaceInvadersHardware hardware, int keyCode, int mask, int port) {
        String pressed = "pressed-" + keyCode;
        String released = "released-" + keyCode;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, false), pressed);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, true), released);
        getActionMap().put(pressed, action(() -> setInput(hardware, port, mask, true)));
        getActionMap().put(released, action(() -> setInput(hardware, port, mask, false)));
    }

    private static AbstractAction action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
    }

    private static void setInput(SpaceInvadersHardware hardware, int port, int mask, boolean pressed) {
        if (port == 1) {
            hardware.setInput1(mask, pressed);
        } else {
            hardware.setInput2(mask, pressed);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        for (int y = 0; y < HEIGHT; y++) {
            int on = colorOverlay ? overlayColor(y) : Color.WHITE.getRGB();
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, pixelOn(memory, x, y) ? on : Color.BLACK.getRGB());
            }
        }
        Graphics2D target = (Graphics2D) graphics.create();
        target.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        target.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        target.dispose();
    }

    private static int overlayColor(int y) {
        if (y < 64) {
            return new Color(255, 80, 80).getRGB();
        }
        if (y >= 184) {
            return new Color(80, 255, 80).getRGB();
        }
        return Color.WHITE.getRGB();
    }
}
