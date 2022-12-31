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
package net.emustudio.plugins.device.zxspectrum.display.io;

import net.jcip.annotations.ThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

@ThreadSafe
public class Display extends JPanel implements LineRoller, ActionListener {
    static final Color FOREGROUND = new Color(255, 255, 255);
    static final Color BACKGROUND = Color.BLACK;
    private final static int DEFAULT_COLUMNS = 120;
    private static FontRenderContext DEFAULT_FRC;
    private final Font terminalFont = new Font("Monospaced", Font.PLAIN, 14);
    private final char[] videoMemory;
    private final int columns;
    private final int rows;
    private final Cursor cursor;
    private final Timer cursorTimer = new Timer(800, this);
    private volatile boolean cursorShouldBePainted;
    private volatile Dimension size;

    public Display() {
        this.rows = 24;
        this.columns = DEFAULT_COLUMNS;
        this.videoMemory = new char[rows * columns];
        this.cursor = new Cursor(columns, rows);

        this.setBackground(java.awt.Color.WHITE);
        this.setForeground(java.awt.Color.BLACK);

        this.setFont(terminalFont);
        this.setDoubleBuffered(true);
        DisplayParameters displayParameters = measure();
        this.size = new Dimension(displayParameters.maxWidth, displayParameters.maxHeight);
    }

    public static FontRenderContext getDefaultFrc() {
        if (DEFAULT_FRC == null) {
            AffineTransform tx;
            if (GraphicsEnvironment.isHeadless()) {
                tx = new AffineTransform();
            } else {
                tx = GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .getDefaultTransform();
            }
            DEFAULT_FRC = new FontRenderContext(tx, false, false);
        }
        return DEFAULT_FRC;
    }

    public synchronized void start() {
        cursorTimer.restart();
    }

    public synchronized void stop() {
        cursorTimer.stop();
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
    public Dimension getPreferredSize() {
        return this.size;
    }

    @Override
    public Dimension getMinimumSize() {
        return this.size;
    }

    public final void clearScreen() {
        fillWithSpaces();
        cursor.home();
        repaint();
    }

    public Cursor getTextCanvasCursor() {
        return cursor;
    }

    public void writeAtCursor(char c) {
        Point cursorPoint = cursor.getCursorPoint();
        writeCharAt(c, cursorPoint.x, cursorPoint.y);
    }

    public void writeCharAt(char c, int x, int y) {
        synchronized (videoMemory) {
            videoMemory[y * columns + x] = c;
        }
        repaint();
    }

    @Override
    public void rollLine() {
        synchronized (videoMemory) {
            System.arraycopy(videoMemory, columns, videoMemory, 0, columns * rows - columns);
            for (int i = columns * rows - columns; i < (columns * rows); i++) {
                videoMemory[i] = ' ';
            }
        }
        repaint();
    }

    @Override
    public void clearLine(int x, int y) {
        synchronized (videoMemory) {
            for (int i = columns * y + x; i < (columns * y + columns); i++) {
                videoMemory[i] = ' ';
            }
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Dimension dimension = size;
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        int t_y;
        int x, y;
        int temp;
        StringBuilder sLine = new StringBuilder();

        int lineHeight = graphics.getFontMetrics().getHeight();
        graphics.setColor(FOREGROUND);
        Graphics2D g2d = (Graphics2D) graphics;
        for (y = 0; y < rows; y++) {
            t_y = (y + 1) * lineHeight;
            temp = y * columns;
            for (x = 0; x < columns; x++) {
                synchronized (videoMemory) {
                    sLine.append(videoMemory[temp + x]);
                }
            }
            g2d.drawString(sLine.toString(), 1, t_y);
            sLine = new StringBuilder();
        }

        paintCursor(graphics);
    }

    private void fillWithSpaces() {
        synchronized (videoMemory) {
            Arrays.fill(videoMemory, ' ');
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() != null && e.getSource() == cursorTimer) {
            repaint();
        }
    }

    private void paintCursor(Graphics graphics) {
        if (!cursorShouldBePainted) {
            Point paintPoint = cursor.getCursorPoint();

            graphics.setXORMode(Display.BACKGROUND);
            graphics.setColor(Display.FOREGROUND);

            Rectangle2D fontRectangle = terminalFont.getStringBounds("W", getDefaultFrc());
            int lineHeight = graphics.getFontMetrics().getHeight();

            int x = 2 + (int) (paintPoint.x * fontRectangle.getWidth());
            int y = 3 + (paintPoint.y * lineHeight);

            graphics.fillRect(x, y, (int) fontRectangle.getWidth(), (int) fontRectangle.getHeight());
            graphics.setPaintMode();

            cursorShouldBePainted = true;
        } else {
            cursorShouldBePainted = false;
        }
    }

    private DisplayParameters measure() {
        Font font = getFont();
        Rectangle2D metrics = font.getStringBounds("W", getDefaultFrc());
        LineMetrics lineMetrics = font.getLineMetrics("W", getDefaultFrc());

        int charWidth = (int) metrics.getWidth();
        int charHeight = (int) lineMetrics.getHeight();

        int maxWidth = columns * charWidth;
        int maxHeight = rows * charHeight;

        return new DisplayParameters(maxWidth, maxHeight, charWidth);
    }
}
