/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.*;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    public static final float ZOOM = 2f;
    public static final int BORDER_WIDTH = 48; // pixels

    public static final int SCREEN_IMAGE_WIDTH = 2 * BORDER_WIDTH + SCREEN_WIDTH_PIXELS;
    public static final int SCREEN_IMAGE_HEIGHT = PRE_SCREEN_LINES + SCREEN_HEIGHT_PIXELS + POST_SCREEN_LINES;

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

    private static final Color KEYBOARD_OVERLAY_COLOR = new Color(0, 0, 0, 127); // Cache the color

    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

    private final ULA ula;
    private final PaintCycle paintCycle = new PaintCycle();
    private final KeyboardCanvas keyboardCanvas;

    public DisplayCanvas(ULA ula, KeyboardCanvas keyboardCanvas) {
        this.ula = Objects.requireNonNull(ula);
        this.keyboardCanvas = Objects.requireNonNull(keyboardCanvas);
        this.screenImage.setAccelerationPriority(1.0f);
        this.screenImageData = ((DataBufferInt) this.screenImage.getRaster().getDataBuffer()).getData();
    }

    public void ensureStarted() {
        if (!isDisplayable()) {
            return;
        }
        if (painting.compareAndSet(false, true)) {
            createBufferStrategy(2);
        }
    }

    public void runPaintCycle() {
        paintCycle.run();
    }

    public void drawNextLine(int line) {
        int borderColor = COLOR_MAP[ula.getBorderColor()].getRGB();
        if (line < PRE_SCREEN_LINES || line >= (PRE_SCREEN_LINES + SCREEN_HEIGHT_PIXELS)) {
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
            for (int byteX = 0; byteX < ATTRIBUTES_WIDTH; byteX++) {
                byte row = ula.videoMemory[byteX][y];
                int attr = ula.attributeMemory[byteX][y / 8];
                Color[] colorMap = ((attr & 0x40) == 0x40) ? BRIGHT_COLOR_MAP : COLOR_MAP;
                boolean flash = (attr & 0x80) == 0x80;

                for (int i = 0; i < 8; i++) {
                    boolean bit = ((row << i) & 0x80) == 0x80;
                    int color;
                    if (ula.videoFlash && flash) {
                        color = (bit ? colorMap[(attr >>> 3) & 7] : colorMap[attr & 7]).getRGB();
                    } else {
                        color = (bit ? colorMap[attr & 7] : colorMap[(attr >>> 3) & 7]).getRGB();
                    }

                    int offset = line * SCREEN_IMAGE_WIDTH + BORDER_WIDTH + screenX + i;
                    screenImageData[offset] = color;
                }
                screenX += 8;
            }
            for (int i = 0; i < 2 * BORDER_WIDTH; i++) {
                int offset = line * SCREEN_IMAGE_WIDTH + BORDER_WIDTH + SCREEN_WIDTH_PIXELS + i;
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
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;

        @Override
        public void run() {
            strategy = getBufferStrategy();
            if (painting.get() && strategy != null) {
                paint();
            }
        }

        protected void paint() {
            // The buffers in a buffer strategy are usually type VolatileImage, they may become lost.
            // VolatileImage differs from other Image variants in that if possible, VolatileImage is stored in
            // Video RAM. This means that instead of keeping the image in the system memory with everything else,
            // it is kept on the memory local to the graphics card. This allows for much faster drawing-to and
            // copying-from operations.
            try {
                do {
                    do {
                        Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
                        // Disable expensive rendering hints for maximum performance
                        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

                        graphics.drawImage(
                                screenImage, MARGIN, MARGIN,
                                (int) (SCREEN_IMAGE_WIDTH * ZOOM), (int) (SCREEN_IMAGE_HEIGHT * ZOOM), null);

                        // Only draw keyboard overlay if explicitly enabled (disabled by default for performance)
                        if (keyboardCanvas.getAlpha() > 0) {
                            Color color = graphics.getColor();
                            graphics.setColor(KEYBOARD_OVERLAY_COLOR);
                            graphics.translate(0, SCREEN_IMAGE_HEIGHT * ZOOM - KeyboardCanvas.KEYBOARD_HEIGHT + MARGIN);
                            keyboardCanvas.paint(graphics);
                            graphics.setColor(color);
                        }

                        graphics.dispose();

                    } while (strategy.contentsRestored());
                    strategy.show();
                    Toolkit.getDefaultToolkit().sync();
                } while (strategy.contentsLost());
            } catch (Exception ignored) {
                repaint();
            }
        }
    }
}
