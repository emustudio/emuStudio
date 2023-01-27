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
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.emustudio.plugins.device.zxspectrum.display.ULA.SCREEN_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.display.ULA.SCREEN_WIDTH;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private static final Color FOREGROUND = new Color(255, 255, 255);
    private static final Color BACKGROUND = Color.BLACK;

    private final Timer repaintTimer;
    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

    private final ULA ula;

    public DisplayCanvas(ULA ula) {
        this.ula = Objects.requireNonNull(ula);

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        Font textFont = new Font("Monospaced", Font.PLAIN, 14);
        setFont(textFont);

        PaintCycle paintCycle = new PaintCycle();
        this.repaintTimer = new Timer(1000 / 50, e -> paintCycle.run()); // 50 HZ
        this.repaintTimer.setCoalesce(true);
    }

    public void start() {
        if (painting.compareAndSet(false, true)) {
            createBufferStrategy(2);
            this.repaintTimer.restart();
        }
    }


    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

    @Override
    public Dimension getMinimumSize() {
        return this.size;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.size = getSize();
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        this.size = getSize();
    }

    @Override
    public void close() {
        repaintTimer.stop();
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;

        @Override
        public void run() {
            ula.readScreen();
            strategy = getBufferStrategy();
            if (painting.get()) {
                paint();
            }
        }

        protected void paint() {
            Dimension dimension = size;
            try {
                do {
                    do {
                        Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                        graphics.setColor(BACKGROUND);
                        graphics.fillRect(0, 0, dimension.width, dimension.height);

                        graphics.setColor(FOREGROUND);
                        byte[][] memory = ula.videoMemory;
                        int screenX = 0;
                        for (int y = 0; y < SCREEN_HEIGHT; y++) {
                            for (int x = 0; x < SCREEN_WIDTH; x++) {
                                byte row = memory[x][y];
                                for (int i = 0; i < 8; i++) {
                                    boolean bit = ((row << i) & 0x80) == 0x80;
                                    if (bit) {
                                        graphics.drawLine(32 + screenX + i, y, 32 + screenX + i, y);
                                    }
                                }
                                screenX += 8;
                            }
                            screenX = 0;
                        }
                        graphics.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (Exception ignored) {
            }
        }
    }
}
