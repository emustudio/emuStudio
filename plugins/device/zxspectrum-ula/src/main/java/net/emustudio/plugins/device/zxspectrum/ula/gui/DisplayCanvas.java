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
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.emustudio.plugins.device.zxspectrum.ula.ULA.SCREEN_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.ula.ULA.SCREEN_WIDTH;

// https://worldofspectrum.org/faq/reference/48kreference.htm

/**
 * DisplayCanvas
 * <p>
 * A frame is (64+192+56)*224=69888 T states long, which means that the '50 Hz' interrupt is actually
 * a 3.5MHz/69888=50.08 Hz interrupt.
 */
public class DisplayCanvas extends Canvas implements AutoCloseable {
    private static final int PRE_SCREEN_LINES = 64;
    private static final int POST_SCREEN_LINES = 56;
    private static final int BORDER_WIDTH = 48; // pixels

    public static final float ZOOM = 2f;
    public static final int SCREEN_IMAGE_WIDTH = 2 * BORDER_WIDTH + SCREEN_WIDTH * 8;
    public static final int SCREEN_IMAGE_HEIGHT = PRE_SCREEN_LINES + SCREEN_HEIGHT + POST_SCREEN_LINES;

    private static final int REPAINT_CPU_TSTATES = 69888;
    private static final int LINE_CPU_TSTATES = 224;

    private final BufferedImage screenImage = new BufferedImage(
            SCREEN_IMAGE_WIDTH, SCREEN_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] screenImageData;

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
    private int interrupts = 0;

    public DisplayCanvas(ULA ula) {
        this.ula = Objects.requireNonNull(ula);
        this.ted = ula.getCpu()
                .getTimedEventsProcessor()
                .orElseThrow(() -> new NoSuchElementException("The CPU does not provide TimedEventProcessor"));
        this.screenImage.setAccelerationPriority(1.0f);
        this.screenImageData = ((DataBufferInt) this.screenImage.getRaster().getDataBuffer()).getData();
    }

    public void start() {
        if (painting.compareAndSet(false, true)) {
            createBufferStrategy(2);

            ted.schedule(REPAINT_CPU_TSTATES, this::triggerCpuInterrupt);
            for (int i = 0; i < SCREEN_IMAGE_HEIGHT; i++) {
                int finalI = i;
                ted.schedule(i * LINE_CPU_TSTATES + 1, () -> drawNextLine(finalI));
            }
        }
    }

    private void triggerCpuInterrupt() {
        interrupts = (interrupts + 1) % 3;  // 0 1 2
        ula.triggerInterrupt();
        if (interrupts == 0) {
            paintCycle.run();
        }
    }

    private void drawNextLine(int line) {
        int borderColor = COLOR_MAP[ula.getBorderColor()].getRGB();
        if (line < PRE_SCREEN_LINES || line >= (PRE_SCREEN_LINES + SCREEN_HEIGHT)) {
            for (int i = 0; i < SCREEN_IMAGE_WIDTH; i++) {
                screenImageData[line * SCREEN_IMAGE_WIDTH + i] = borderColor;
            }
            if (line < PRE_SCREEN_LINES) {
                for (int i = SCREEN_IMAGE_WIDTH; i < SCREEN_IMAGE_WIDTH + BORDER_WIDTH; i++) {
                    screenImageData[line * SCREEN_IMAGE_WIDTH + i] = borderColor;
                }
            }
        } else {
            int y = line - PRE_SCREEN_LINES;
            ula.readLine(y);
            int screenX = 0;
            for (int byteX = 0; byteX < SCREEN_WIDTH; byteX++) {
                byte row = ula.videoMemory[byteX][y];
                int attr = ula.attributeMemory[byteX][y / 8];
                Color[] colorMap = ((attr & 0x40) == 0x40) ? BRIGHT_COLOR_MAP : COLOR_MAP;

                for (int i = 0; i < 8; i++) {
                    boolean bit = ((row << i) & 0x80) == 0x80;
                    int color = (bit ? colorMap[attr & 7] : colorMap[(attr >>> 3) & 7]).getRGB();
                    int offset = line * SCREEN_IMAGE_WIDTH + BORDER_WIDTH + screenX + i;
                    screenImageData[offset] = color;
                }
                screenX += 8;
            }
            for (int i = 0; i < 2 * BORDER_WIDTH; i++) {
                int offset = line * SCREEN_IMAGE_WIDTH + BORDER_WIDTH + SCREEN_WIDTH * 8 + i;
                screenImageData[offset] = borderColor;
            }
        }
    }

    public void redrawNow() {
        ula.readScreen();
        for (int i = 0; i < SCREEN_IMAGE_HEIGHT; i++) {
            drawNextLine(i);
        }
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
        for (int i = 0; i < SCREEN_IMAGE_HEIGHT; i++) {
            int finalI = i;
            ted.remove(i * LINE_CPU_TSTATES, () -> drawNextLine(finalI));
        }
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;

        @Override
        public void run() {
            strategy = getBufferStrategy();
            if (painting.get()) {
                paint();
            }
        }

        protected void paint() {
            // The buffers in a buffer strategy are usually type VolatileImage, they may become lost.
            // VolatileImage differs from other Image variants in that if possible, VolatileImage is stored in
            // Video RAM. This means that instead of keeping the image in the system memory with everything else,
            // it is kept on the memory local to the graphics card. This allows for much faster drawing-to and
            // copying-from operations.
            do {
                do {
                    Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

                    graphics.drawImage(
                            screenImage, 0, 0,
                            (int) (SCREEN_IMAGE_WIDTH * ZOOM), (int) (SCREEN_IMAGE_HEIGHT * ZOOM),
                            null);
                    graphics.dispose();
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
        }
    }
}
