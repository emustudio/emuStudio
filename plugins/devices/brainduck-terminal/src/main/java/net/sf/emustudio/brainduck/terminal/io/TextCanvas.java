/*
 * Copyright (C) 2014 Peter Jakubčo
 * KISS, DRY, YAGNI
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.emustudio.brainduck.terminal.io;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;


@ThreadSafe
public class TextCanvas extends JPanel {
    @GuardedBy("memory")
    private final int[][] memory;
    private final int maxColumn;
    private final int maxRow;

    private final Cursor cursor;
    private volatile boolean needMeasure;
    private volatile int charWidth;
    private volatile int charHeight;

    private final Font textFont = new Font("Monospaced", 0, 14);

    public TextCanvas(int maxColumn, int maxRow) {
        this.maxColumn = maxColumn;
        this.maxRow = maxRow;
        this.memory = new int[maxRow][maxColumn];
        this.cursor = new Cursor(1, TimeUnit.SECONDS, maxColumn);

        this.setBackground(java.awt.Color.WHITE);
        this.setForeground(java.awt.Color.BLACK);
        this.setFont(textFont);
        this.setDoubleBuffered(true);
        needMeasure = true;
    }
    
    public void start() {
        measureIfNeeded();
        cursor.start(this);
    }
    
    public void stop() {
        cursor.stop();
    }

    public Cursor getTextCanvasCursor() {
        return cursor;
    }
    
    public void writeAtCursor(int c) {
        Point cursorPoint = cursor.getLogicalPoint();
        writeCharAt(c, cursorPoint.x, cursorPoint.y);
        cursor.advance(1);
    }
    
    public void writeCharAt(int c, int x, int y) {
        synchronized (memory) {
            memory[y][x] = c;
        }
        repaint();
    }
    
    public void clear() {
        synchronized (memory) {
            for (int[] row : memory) {
                for (int col = 0; col < row.length; col++) {
                    row[col] = 0;
                }
            }
        }
        cursor.reset();
        repaint();
    }

    private void measureIfNeeded() {
        if (needMeasure) {
            FontMetrics fontMetrics = getGraphics().getFontMetrics();
            charWidth = fontMetrics.charWidth('W');
            charHeight = fontMetrics.getHeight();
            needMeasure = false;
        }
    }

    public int getCharWidth() {
        return charWidth;
    }

    public int getCharHeight() {
        return charHeight;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setFont(textFont);
        g.clearRect(0, 0, getSize().width, getSize().height);

        synchronized (memory) {
            int y = charHeight;
            int x = 0;

            for (int row = 0; row < memory.length; row++) {
                for (int col = 0; col < memory[row].length; col++) {
                    int value = memory[row][col];

                    if (value < 32) {
                        continue;
                    }

                    g.drawString("" + (char)value, x, y);
                    x += charWidth;
                }
                y += charHeight;
            }
        }
    }
    
}
