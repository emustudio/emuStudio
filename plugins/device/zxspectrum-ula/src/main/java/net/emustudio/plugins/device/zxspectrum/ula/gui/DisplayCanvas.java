/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.KeyboardCanvas.KEYBOARD_HEIGHT;

/**
 * Canvas responsible for rendering the ZX Spectrum screen and handling mouse interactions for the keyboard overlay.
 */
public class DisplayCanvas extends Canvas implements AutoCloseable {
    public static final float ZOOM = 2f;

    // ZX Spectrum palette, bit 0 = blue, bit 1 = red, bit 2 = green
    private static final Color[] COLOR_MAP = palette(0xD8);
    private static final Color[] BRIGHT_COLOR_MAP = palette(0xFF);

    private static Color[] palette(int intensity) {
        Color[] colors = new Color[8];
        for (int i = 0; i < 8; i++) {
            colors[i] = new Color(
                    (i & 2) == 0 ? 0 : intensity,
                    (i & 4) == 0 ? 0 : intensity,
                    (i & 1) == 0 ? 0 : intensity);
        }
        return colors;
    }

    private static final Color KEYBOARD_OVERLAY_COLOR = new Color(0, 0, 0, 127);

    private volatile Dimension size;
    private final Dimension minimumSize;

    private final ULA ula;
    private final ModelProfile profile;
    private final KeyboardCanvas keyboardCanvas;
    private final int borderWidth;
    private final int screenImageWidth;
    private final int screenImageHeight;
    private final int keyboardTop;
    private final BufferedImage screenImage;
    private final int[] screenImageData;
    private volatile Consumer<BufferedImage> frameListener;
    private volatile BufferedImage backBuffer;

    public DisplayCanvas(ULA ula, ModelProfile profile, KeyboardCanvas keyboardCanvas) {
        this.ula = Objects.requireNonNull(ula);
        this.profile = Objects.requireNonNull(profile);
        this.keyboardCanvas = Objects.requireNonNull(keyboardCanvas);
        this.borderWidth = profile.borderWidthPixels;
        this.screenImageWidth = profile.screenWidthPixels + 2 * profile.borderWidthPixels;
        this.screenImageHeight = profile.frameLineCount;
        this.keyboardTop = (int) (ZOOM * screenImageHeight - KEYBOARD_HEIGHT + MARGIN);
        this.screenImage = new BufferedImage(screenImageWidth, screenImageHeight, BufferedImage.TYPE_INT_RGB);
        this.screenImage.setAccelerationPriority(1.0f);
        this.screenImageData = ((DataBufferInt) this.screenImage.getRaster().getDataBuffer()).getData();
        this.size = new Dimension(
                (int) (ZOOM * screenImageWidth + 2 * MARGIN),
                (int) (ZOOM * screenImageHeight + 2 * MARGIN)
        );
        this.minimumSize = new Dimension(size);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (keyboardCanvas.handleMousePressed(e.getX(), e.getY() - keyboardTop, ula)) {
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (keyboardCanvas.handleMouseReleased(ula)) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (keyboardCanvas.handleMouseReleased(ula)) {
                    repaint();
                }
            }
        });
    }

    public void setFrameListener(Consumer<BufferedImage> frameListener) {
        this.frameListener = frameListener;
    }

    /** Native raster dimensions, padded to even dimensions for H.264. Independent of window size. */
    public Dimension getRecordingSize() {
        return new Dimension((screenImageWidth + 1) & ~1, (screenImageHeight + 1) & ~1);
    }

    /** Captures a completed emulated frame, independently of Swing painting. */
    public void frameReady() {
        Consumer<BufferedImage> listener = frameListener;
        if (listener != null) {
            Dimension size = getRecordingSize();
            BufferedImage frame = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < screenImageHeight; y++) {
                System.arraycopy(screenImageData, y * screenImageWidth, pixels, y * size.width, screenImageWidth);
            }
            listener.accept(frame);
        }
    }

    /** Paints the border at the beam position, preserving changes within a raster line. */
    public void drawBorderCycles(long frameCycle, long cycles, int color) {
        int rgb = COLOR_MAP[color].getRGB();
        long origin = profile.firstContendedTstate + 1L
                - (long) profile.preScreenLines * profile.displayLineTstates;
        for (long cycle = frameCycle; cycle < frameCycle + cycles; cycle++) {
            long position = Math.floorMod(cycle - origin + borderWidth / 2, profile.displayFrameTstates);
            int line = (int) (position / profile.displayLineTstates);
            int x = (int) (position % profile.displayLineTstates) * 2;
            // The 48K ULA latches color once per 8-pixel (4T) cell, at its third T-state.
            // An OUT changes the port register immediately, but cannot split this color cell.
            if ((x & 7) != 4) {
                continue;
            }
            x &= ~7;
            if (x >= screenImageWidth) {
                continue; // horizontal blanking
            }
            boolean paperLine = line >= profile.preScreenLines
                    && line < profile.preScreenLines + profile.screenHeightPixels;
            if (!paperLine || x < borderWidth || x >= borderWidth + profile.screenWidthPixels) {
                int offset = line * screenImageWidth + x;
                Arrays.fill(screenImageData, offset, offset + 8, rgb);
            }
        }
    }

    /** Renders a complete line for a paused display or an explicit redraw. */
    public void drawNextLine(int line) {
        drawNextLine(line, true);
    }

    /** Renders the bitmap; live border pixels are painted separately as the beam advances. */
    public void drawNextLine(int line, boolean drawBorder) {
        if (line < 0 || line >= screenImageHeight) {
            return;
        }
        int borderColor = COLOR_MAP[ula.getBorderColor()].getRGB();
        if (line < profile.preScreenLines || line >= (profile.preScreenLines + profile.screenHeightPixels)) {
            if (drawBorder) {
                Arrays.fill(screenImageData, line * screenImageWidth,
                        (line + 1) * screenImageWidth, borderColor);
            }
        } else {
            int y = line - profile.preScreenLines;
            ula.readLine(y);
            int screenX = 0;
            for (int byteX = 0; byteX < profile.attributesWidth; byteX++) {
                byte row = ula.videoMemory[byteX][y];
                int attr = ula.attributeMemory[byteX][y / 8];
                Color[] colorMap = ((attr & 0x40) == 0x40) ? BRIGHT_COLOR_MAP : COLOR_MAP;
                boolean flash = (attr & 0x80) == 0x80;

                for (int i = 0; i < 8; i++) {
                    boolean bit = ((row << i) & 0x80) == 0x80;
                    int color;
                    if (ula.videoFlash.get() && flash) {
                        color = (bit ? colorMap[(attr >>> 3) & 7] : colorMap[attr & 7]).getRGB();
                    } else {
                        color = (bit ? colorMap[attr & 7] : colorMap[(attr >>> 3) & 7]).getRGB();
                    }

                    int offset = line * screenImageWidth + borderWidth + screenX + i;
                    screenImageData[offset] = color;
                }
                screenX += 8;
            }
            if (drawBorder) {
                int start = line * screenImageWidth;
                Arrays.fill(screenImageData, start, start + borderWidth, borderColor);
                Arrays.fill(screenImageData, start + borderWidth + profile.screenWidthPixels,
                        start + screenImageWidth, borderColor);
            }
        }
    }

    public void redrawNow() {
        ula.readScreen();
        for (int i = 0; i < screenImageHeight; i++) {
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
        return new Dimension(minimumSize);
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

    private void renderFrame(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

        graphics.setColor(new Color(0xD8, 0xD8, 0xD8));
        graphics.fillRect(0, 0, getWidth(), getHeight());

        graphics.drawImage(
                screenImage, MARGIN, MARGIN,
                (int) (screenImageWidth * ZOOM), (int) (screenImageHeight * ZOOM), null
        );

        if (keyboardCanvas.getAlpha() > 0) {
            Color color = graphics.getColor();
            graphics.setColor(KEYBOARD_OVERLAY_COLOR);
            graphics.translate(0, keyboardTop);
            keyboardCanvas.paint(graphics);
            graphics.setColor(color);
            graphics.translate(0, -keyboardTop);
        }
    }
}
