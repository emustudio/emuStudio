/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.adm3a.api.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.RenderingHints.*;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayCanvas.class);
    private static final Color FOREGROUND = new Color(255, 255, 255);
    private static final Color BACKGROUND = Color.BLACK;
    private final Timer repaintTimer;
    private final Display display; // Canvas is not owning display!
    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile DisplayFont displayFont;
    private volatile Dimension size = new Dimension(0, 0);
    private volatile Dimension minimumSize = new Dimension(0, 0);

    public DisplayCanvas(DisplayFont displayFont, Display display) {
        this.display = Objects.requireNonNull(display);

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setDisplayFont(displayFont);

        PaintCycle paintCycle = new PaintCycle();
        this.repaintTimer = new Timer(1000 / 60, e -> paintCycle.run()); // 60 HZ
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
        return new Dimension(minimumSize);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        updateSize();
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        updateSize();
    }

    private void updateSize() {
        Dimension newSize = getSize();
        if (minimumSize.width == 0 && minimumSize.height == 0) {
            minimumSize = new Dimension(newSize);
        }
        size = newSize;
    }

    public synchronized void setDisplayFont(DisplayFont font) {
        // setting font must be atomic

        setFont(GUI.loadFontResource(font.path, DisplayCanvas.class, font.fontSize));
        this.displayFont = Objects.requireNonNull(font);
    }

    @Override
    public void close() {
        repaintTimer.stop();
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;
        private char[] videoMemory = new char[0];
        private Point cursorPoint = new Point();
        private Dimension dimension = new Dimension();
        private int columns;
        private int rows;
        private Font font;
        private DisplayFont renderFont;

        @Override
        public void run() {
            strategy = getBufferStrategy();
            if (painting.get() && strategy != null && refreshFrame()) {
                paint();
            }
        }

        private boolean refreshFrame() {
            char[] newVideoMemory = display.getVideoMemory();
            Point newCursorPoint = Objects.requireNonNullElse(display.getCursorPoint(), new Point());
            Dimension newDimension = size;
            int newColumns = display.getColumns();
            int newRows = display.getRows();
            Font newFont = getFont();
            DisplayFont newRenderFont = displayFont;

            if (Arrays.equals(videoMemory, newVideoMemory)
                    && cursorPoint.equals(newCursorPoint)
                    && dimension.equals(newDimension)
                    && columns == newColumns
                    && rows == newRows
                    && Objects.equals(font, newFont)
                    && renderFont == newRenderFont) {
                return false;
            }

            videoMemory = newVideoMemory == null ? new char[0] : Arrays.copyOf(newVideoMemory, newVideoMemory.length);
            cursorPoint = new Point(newCursorPoint);
            dimension = new Dimension(newDimension);
            columns = newColumns;
            rows = newRows;
            font = newFont;
            renderFont = newRenderFont;
            return true;
        }

        protected void paint() {
            Dimension dimension = this.dimension;
            Graphics2D graphics = null;
            try {
                do {
                    do {
                        graphics = (Graphics2D) strategy.getDrawGraphics();
                        graphics.setColor(BACKGROUND);
                        graphics.fillRect(0, 0, dimension.width, dimension.height);
                        graphics.setFont(font);

                        int lineHeight = graphics.getFontMetrics().getHeight() + renderFont.yLineHeightMultiplierOffset;
                        graphics.setColor(FOREGROUND);
                        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_FRACTIONALMETRICS, renderFont.fractionalMetrics);
                        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, renderFont.textAntiAliasing);
                        graphics.setRenderingHint(KEY_ANTIALIASING, renderFont.antiAliasing);
                        graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);
                        for (int y = 0; y < rows; y++) {
                            graphics.drawChars(
                                    videoMemory,
                                    y * columns,
                                    columns,
                                    1,
                                    (y + 1) * lineHeight);
                        }
                        paintCursor(graphics, lineHeight, cursorPoint, renderFont);
                        graphics.dispose();
                        graphics = null;
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (IllegalStateException e) {
                if (painting.get()) {
                    LOGGER.warn("Could not paint ADM-3A display", e);
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Could not paint ADM-3A display", e);
            } finally {
                if (graphics != null) {
                    graphics.dispose();
                }
            }
        }

        private void paintCursor(Graphics graphics, int lineHeight, Point cursorPoint, DisplayFont displayFont) {
            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            FontMetrics fontMetrics = graphics.getFontMetrics();
            int cellWidth = fontMetrics.charWidth('W');

            int x = displayFont.xCursorOffset + (cursorPoint.x * cellWidth);
            int y = displayFont.yCursorOffset + (cursorPoint.y * lineHeight);

            graphics.fillRect(x, y, cellWidth, fontMetrics.getHeight() + displayFont.yCursorExtend);
            graphics.setPaintMode();
        }
    }
}
