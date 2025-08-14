/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

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
                        graphics.setColor(FOREGROUND);
                        graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
                        graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                        graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
                        graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);
                        for (int y = 0; y < display.getRows(); y++) {
                            graphics.drawChars(
                                    display.getVideoMemory(),
                                    y * display.getColumns(),
                                    display.getColumns(),
                                    1,
                                    (y + 1) * lineHeight);
                        }
                        paintCursor(graphics, lineHeight);
                        graphics.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            } catch (Exception ignored) {
            }
        }

        private void paintCursor(Graphics graphics, int lineHeight) {
            Point cursorPoint = display.getCursorPoint();

            graphics.setXORMode(BACKGROUND);
            graphics.setColor(FOREGROUND);

            Rectangle2D fontRectangle = getFont().getStringBounds("M", graphics.getFontMetrics().getFontRenderContext());

            int x = (int) (cursorPoint.x * fontRectangle.getWidth());
            int y = cursorPoint.y * lineHeight + 5;

            graphics.fillRect(x, y, (int) fontRectangle.getWidth(), (int) fontRectangle.getHeight());
            graphics.setPaintMode();
        }
    }
}
