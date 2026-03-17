/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Objects;
import java.util.function.Consumer;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.*;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.KeyboardCanvas.KEYBOARD_HEIGHT;

/**
 * Canvas responsible for rendering the ZX Spectrum screen and handling mouse interactions for the keyboard overlay.
 */
public class DisplayCanvas extends Canvas implements AutoCloseable {
    public static final float ZOOM = 2f;
    public static final int BORDER_WIDTH = 48; // pixels

    public static final int SCREEN_IMAGE_WIDTH = 2 * BORDER_WIDTH + SCREEN_WIDTH_PIXELS;
    public static final int SCREEN_IMAGE_HEIGHT = PRE_SCREEN_LINES + SCREEN_HEIGHT_PIXELS + POST_SCREEN_LINES;
    public static final int KEYBOARD_TOP = (int) (ZOOM * SCREEN_IMAGE_HEIGHT - KEYBOARD_HEIGHT + MARGIN);

    private final BufferedImage screenImage = new BufferedImage(
            SCREEN_IMAGE_WIDTH, SCREEN_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] screenImageData;

    private static final Color[] COLOR_MAP = new Color[]{
            new Color(0, 0, 0),  // black
            new Color(0, 0, 0xD8), // blue
            new Color(0xD8, 0, 0), // red
            new Color(0xD8, 0, 0xD8), // magenta
            new Color(0, 0xD8, 0), // green
            new Color(0, 0xD8, 0xD8), // cyan
            new Color(0xD8, 0xD8, 0), // yellow
            new Color(0xD8, 0xD8, 0xD8) // white
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

    private static final Color KEYBOARD_OVERLAY_COLOR = new Color(0, 0, 0, 127);

    private volatile Dimension size = new Dimension(
            (int) (ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN),
            (int) (ZOOM * SCREEN_IMAGE_HEIGHT + 2 * MARGIN)
    );

    private final ULA ula;
    private final KeyboardCanvas keyboardCanvas;
    private volatile Consumer<BufferedImage> frameListener;
    private volatile BufferedImage backBuffer;

    public DisplayCanvas(ULA ula, KeyboardCanvas keyboardCanvas) {
        this.ula = Objects.requireNonNull(ula);
        this.keyboardCanvas = Objects.requireNonNull(keyboardCanvas);
        this.screenImage.setAccelerationPriority(1.0f);
        this.screenImageData = ((DataBufferInt) this.screenImage.getRaster().getDataBuffer()).getData();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleOverlayMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleOverlayMouseReleased();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                handleOverlayMouseReleased();
            }
        });
    }

    public void setFrameListener(Consumer<BufferedImage> frameListener) {
        this.frameListener = frameListener;
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
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        BufferedImage buffer = ensureBackBuffer(w, h);
        Graphics2D g2d = buffer.createGraphics();
        try {
            renderFrame(g2d);
        } finally {
            g2d.dispose();
        }
        g.drawImage(buffer, 0, 0, null);

        Consumer<BufferedImage> listener = frameListener;
        if (listener != null) {
            BufferedImage frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D fg = frame.createGraphics();
            fg.drawImage(buffer, 0, 0, null);
            fg.dispose();
            listener.accept(frame);
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
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
        keyboardCanvas.releaseMouseKeys(ula);
    }

    private BufferedImage ensureBackBuffer(int width, int height) {
        BufferedImage buffer = backBuffer;
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            backBuffer = buffer;
        }
        return buffer;
    }

    private void handleOverlayMousePressed(MouseEvent e) {
        if (keyboardCanvas.handleMousePressed(e.getX(), e.getY() - KEYBOARD_TOP, ula)) {
            repaint();
        }
    }

    private void handleOverlayMouseReleased() {
        if (keyboardCanvas.handleMouseReleased(ula)) {
            repaint();
        }
    }

    private void renderFrame(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

        graphics.drawImage(
                screenImage, MARGIN, MARGIN,
                (int) (SCREEN_IMAGE_WIDTH * ZOOM), (int) (SCREEN_IMAGE_HEIGHT * ZOOM), null
        );

        if (keyboardCanvas.getAlpha() > 0) {
            Color color = graphics.getColor();
            graphics.setColor(KEYBOARD_OVERLAY_COLOR);
            graphics.translate(0, KEYBOARD_TOP);
            keyboardCanvas.paint(graphics);
            graphics.setColor(color);
            graphics.translate(0, -KEYBOARD_TOP);
        }
    }
}
