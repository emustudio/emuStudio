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

import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.plugins.device.zxspectrum.display.ULA;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.emustudio.plugins.device.zxspectrum.display.ULA.SCREEN_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.display.ULA.SCREEN_WIDTH;

// https://worldofspectrum.org/faq/reference/48kreference.htm
public class DisplayCanvas extends Canvas implements AutoCloseable {
    // a frame is (64+192+56)*224=69888 T states long, which means that the '50 Hz' interrupt is actually
    // a 3.5MHz/69888=50.08 Hz interrupt
    private static final int REPAINT_CPU_TSTATES = 69888;

    private static final int BORDER_WIDTH = 48; // pixels
    private static final int BORDER_HEIGHT = 56; // pixels
    private static final int X_GAP = 48; // pixels
    private static final int Y_GAP = 48; // pixels

    private static final Color FOREGROUND = new Color(255, 255, 255);
    private static final Color BACKGROUND = new Color(0xAA, 0xAA, 0xAA);

    private static final Color[] COLOR_MAP = new Color[]{
            new Color(0, 0, 0),  // black
            new Color(0, 0, 0xEE), // blue
            new Color(0xEE, 0, 0), // red
            new Color(0xEE, 0, 0xEE), // magenta
            new Color(0, 0xEE, 0), // green
            new Color(0, 0xEE, 0xEE), // cyan
            new Color(0xEE, 0xEE, 0), // yellow
            new Color(0xEE, 0xEE, 0xEE) // white
    };

    private static final Color[] BRIGHT_COLOR_MAP = new Color[]{
            new Color(0, 0, 0),  // black
            new Color(0, 0, 0xFF), // blue
            new Color(0xFF, 0, 0), // red
            new Color(0xFF, 0, 0xFF), // magenta
            new Color(0, 0xFF, 0), // green
            new Color(0, 0xFF, 0xFF), // cyan
            new Color(0xFF, 0xFF, 0), // yellow
            new Color(0xFF, 0xFF, 0xFF) // white
    };

    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

    private final ULA ula;
    private final TimedEventsProcessor ted;
    private final PaintCycle paintCycle = new PaintCycle();

    public DisplayCanvas(ULA ula) {
        this.ula = Objects.requireNonNull(ula);

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        Font textFont = new Font("Monospaced", Font.PLAIN, 14);
        setFont(textFont);

        this.ted = ula.getCpu()
                .getTimedEventsProcessor()
                .orElseThrow(() -> new NoSuchElementException("The CPU does not provide TimedEventProcessor"));
    }

    public void start() {
        if (painting.compareAndSet(false, true)) {
            createBufferStrategy(2);

            ted.schedule(REPAINT_CPU_TSTATES, paintCycle);
        }
    }

    public void redrawNow() {
        paintCycle.run();
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
        ted.remove(REPAINT_CPU_TSTATES, paintCycle);
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
            // The buffers in a buffer strategy are usually type VolatileImage, they may become lost.
            // VolatileImage differs from other Image variants in that if possible, VolatileImage is stored in
            // Video RAM. This means that instead of keeping the image in the system memory with everything else,
            // it is kept on the memory local to the graphics card. This allows for much faster drawing-to and
            // copying-from operations.
            do {
                do {
                    Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                    graphics.setColor(BACKGROUND);
                    graphics.fillRect(0, 0, dimension.width, dimension.height);

                    graphics.setColor(FOREGROUND);
                    byte[][] videoMemory = ula.videoMemory;
                    byte[][] attrMemory = ula.attributeMemory;

                    graphics.setBackground(COLOR_MAP[ula.getBorderColor()]);
                    graphics.setColor(COLOR_MAP[ula.getBorderColor()]);
                    graphics.fillRect(
                            X_GAP, Y_GAP,
                            2*(BORDER_WIDTH + SCREEN_WIDTH*8 + BORDER_WIDTH),
                            2*(SCREEN_HEIGHT + 2*BORDER_HEIGHT)
                    );
                    int screenX = 0;
                    for (int y = 0; y < SCREEN_HEIGHT; y++) {
                        for (int x = 0; x < SCREEN_WIDTH; x++) {
                            byte row = videoMemory[x][y];
                            int attr = attrMemory[x][y / 8];
                            Color[] colorMap = ((attr & 0x40) == 0x40) ? BRIGHT_COLOR_MAP : COLOR_MAP;

                            for (int i = 0; i < 8; i++) {
                                boolean bit = ((row << i) & 0x80) == 0x80;
                                if (bit) {
                                    graphics.setColor(colorMap[attr & 7]);
                                } else {
                                    graphics.setColor(colorMap[(attr >>> 3) & 7]);
                                }
                                graphics.drawLine(
                                        X_GAP + 2 * (BORDER_WIDTH + screenX + i), Y_GAP + 2 * (BORDER_HEIGHT + y),
                                        X_GAP + 2 * (BORDER_WIDTH + screenX + i) + 1, Y_GAP + 2 * (BORDER_HEIGHT + y));
                                graphics.drawLine(
                                        X_GAP + 2 * (BORDER_WIDTH + screenX + i), Y_GAP + 2 * (BORDER_HEIGHT + y) + 1,
                                        X_GAP + 2 * (BORDER_WIDTH + screenX + i) + 1, Y_GAP + 2 * (BORDER_HEIGHT + y) + 1);
                            }
                            screenX += 8;
                        }
                        screenX = 0;
                    }
                    graphics.dispose();
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
        }
    }
}
