/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.plugins.device.vt100.VideoAttribute;
import net.emustudio.plugins.device.vt100.api.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
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
    private final Display display; // not owning this
    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);
    private volatile Dimension minimumSize = new Dimension(0, 0);

    public DisplayCanvas(Display display) {
        this.display = Objects.requireNonNull(display);

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        Font textFont = new Font("Monospaced", Font.PLAIN, 14);
        setFont(textFont);

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

    @Override
    public void close() {
        repaintTimer.stop();
        painting.set(false);
    }

    public class PaintCycle implements Runnable {
        private BufferStrategy strategy;
        private char[] videoMemory = new char[0];
        private int[] attributeMemory = new int[0];
        private Point cursorPoint = new Point();
        private Dimension dimension = new Dimension();
        private int columns;
        private int rows;
        private Font normalFont;
        private Font boldFont;
        private Font italicFont;
        private Font boldItalicFont;

        @Override
        public void run() {
            strategy = getBufferStrategy();
            if (painting.get() && strategy != null && refreshFrame()) {
                paint();
            }
        }

        private boolean refreshFrame() {
            char[] newVideoMemory = display.getVideoMemory();
            int[] newAttributeMemory = display.getAttributeMemory();
            Point newCursorPoint = Objects.requireNonNullElse(display.getCursorPoint(), new Point());
            Dimension newDimension = size;
            int newColumns = display.getColumns();
            int newRows = display.getRows();
            Font newNormalFont = getFont();

            if (Arrays.equals(videoMemory, newVideoMemory)
                    && Arrays.equals(attributeMemory, newAttributeMemory)
                    && cursorPoint.equals(newCursorPoint)
                    && dimension.equals(newDimension)
                    && columns == newColumns
                    && rows == newRows
                    && Objects.equals(normalFont, newNormalFont)) {
                return false;
            }

            // getVideoMemory/getAttributeMemory already return defensive copies
            videoMemory = newVideoMemory == null ? new char[0] : newVideoMemory;
            attributeMemory = newAttributeMemory;
            cursorPoint = new Point(newCursorPoint);
            dimension = new Dimension(newDimension);
            columns = newColumns;
            rows = newRows;
            if (!Objects.equals(normalFont, newNormalFont)) {
                normalFont = newNormalFont;
                boldFont = normalFont.deriveFont(Font.BOLD);
                italicFont = normalFont.deriveFont(Font.ITALIC);
                boldItalicFont = normalFont.deriveFont(Font.BOLD | Font.ITALIC);
            }
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
                        graphics.setFont(normalFont);

                        int lineHeight = graphics.getFontMetrics().getHeight();
                        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
                        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);

                        char[] videoMem = videoMemory;
                        int[] attrMem = attributeMemory;
                        int columns = this.columns;
                        int rows = this.rows;

                        Rectangle2D fontRect = getFont().getStringBounds("M", graphics.getFontMetrics().getFontRenderContext());
                        double charWidth = fontRect.getWidth();
                        int fontAscent = graphics.getFontMetrics().getAscent();

                        boolean hasAttributes = attrMem != null && attrMem.length == videoMem.length;
                        Font normalFont = this.normalFont;
                        Font boldFont = this.boldFont;
                        Font italicFont = this.italicFont;
                        Font boldItalicFont = this.boldItalicFont;

                        for (int y = 0; y < rows; y++) {
                            for (int x = 0; x < columns; x++) {
                                int offset = y * columns + x;
                                if (offset >= videoMem.length) break;

                                char ch = videoMem[offset];
                                int attr = hasAttributes ? attrMem[offset] : VideoAttribute.DEFAULT;

                                int px = (int) (x * charWidth) + 1;
                                int py = y * lineHeight;

                                // Draw background if non-default
                                Color bgColor = VideoAttribute.resolveBgColor(attr);
                                if (!bgColor.equals(BACKGROUND)) {
                                    graphics.setColor(bgColor);
                                    graphics.fillRect(px, py, (int) charWidth + 1, lineHeight);
                                }

                                // Draw character
                                if (!VideoAttribute.isHidden(attr) && ch != ' ') {
                                    Color fgColor = VideoAttribute.resolveFgColor(attr);
                                    graphics.setColor(fgColor);

                                    // Select font style
                                    boolean bold = VideoAttribute.isBold(attr);
                                    boolean italic = VideoAttribute.isItalic(attr);
                                    if (bold && italic) {
                                        graphics.setFont(boldItalicFont);
                                    } else if (bold) {
                                        graphics.setFont(boldFont);
                                    } else if (italic) {
                                        graphics.setFont(italicFont);
                                    } else {
                                        graphics.setFont(normalFont);
                                    }

                                    graphics.drawChars(videoMem, offset, 1, px, py + fontAscent);
                                }

                                // Draw underline
                                if (VideoAttribute.isUnderline(attr)) {
                                    Color fgColor = VideoAttribute.resolveFgColor(attr);
                                    graphics.setColor(fgColor);
                                    int underlineY = py + fontAscent + 2;
                                    graphics.drawLine(px, underlineY, px + (int) charWidth, underlineY);
                                }

                                // Draw strikethrough
                                if (VideoAttribute.isStrikethrough(attr)) {
                                    Color fgColor = VideoAttribute.resolveFgColor(attr);
                                    graphics.setColor(fgColor);
                                    int strikeY = py + fontAscent / 2;
                                    graphics.drawLine(px, strikeY, px + (int) charWidth, strikeY);
                                }
                            }
                        }

                        // Restore default font for cursor painting
                        graphics.setFont(normalFont);
                        paintCursor(graphics, lineHeight, charWidth, cursorPoint);
                        graphics.dispose();
                        graphics = null;
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (IllegalStateException e) {
                if (painting.get()) {
                    LOGGER.warn("Could not paint VT100 display", e);
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Could not paint VT100 display", e);
            } finally {
                if (graphics != null) {
                    graphics.dispose();
                }
            }
        }

        private void paintCursor(Graphics graphics, int lineHeight, double charWidth, Point cursorPoint) {
            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            int x = (int) (cursorPoint.x * charWidth) + 1;
            int y = cursorPoint.y * lineHeight + 5;

            graphics.fillRect(x, y, (int) charWidth, lineHeight - 5);
            graphics.setPaintMode();
        }
    }
}
