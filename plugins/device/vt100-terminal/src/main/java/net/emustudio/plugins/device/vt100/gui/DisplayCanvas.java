/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.plugins.device.vt100.VideoAttribute;
import net.emustudio.plugins.device.vt100.api.Display;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.RenderingHints.*;

public class DisplayCanvas extends Canvas implements AutoCloseable {
    private static final Color FOREGROUND = new Color(255, 255, 255);
    private static final Color BACKGROUND = Color.BLACK;
    private final Timer repaintTimer;
    private final Display display; // not owning this
    private final AtomicBoolean painting = new AtomicBoolean(false);
    private volatile Dimension size = new Dimension(0, 0);

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

                        int lineHeight = graphics.getFontMetrics().getHeight();
                        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
                        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);

                        char[] videoMem = display.getVideoMemory();
                        int[] attrMem = display.getAttributeMemory();
                        int columns = display.getColumns();
                        int rows = display.getRows();

                        Rectangle2D fontRect = getFont().getStringBounds("M", graphics.getFontMetrics().getFontRenderContext());
                        double charWidth = fontRect.getWidth();
                        int fontAscent = graphics.getFontMetrics().getAscent();

                        boolean hasAttributes = attrMem != null && attrMem.length == videoMem.length;
                        Font normalFont = graphics.getFont();
                        Font boldFont = normalFont.deriveFont(Font.BOLD);
                        Font italicFont = normalFont.deriveFont(Font.ITALIC);
                        Font boldItalicFont = normalFont.deriveFont(Font.BOLD | Font.ITALIC);

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
                        paintCursor(graphics, lineHeight, charWidth);
                        graphics.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (Exception ignored) {
            }
        }

        private void paintCursor(Graphics graphics, int lineHeight, double charWidth) {
            Point cursorPoint = display.getCursorPoint();

            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            int x = (int) (cursorPoint.x * charWidth) + 1;
            int y = cursorPoint.y * lineHeight + 5;

            graphics.fillRect(x, y, (int) charWidth, lineHeight - 5);
            graphics.setPaintMode();
        }
    }
}
