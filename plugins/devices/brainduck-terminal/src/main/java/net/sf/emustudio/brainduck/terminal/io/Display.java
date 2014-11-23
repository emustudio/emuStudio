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

import javax.swing.JPanel;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ThreadSafe
public class Display extends JPanel {

    @GuardedBy("memory")
    private final List<List<Integer>> memory = new LinkedList<>();
    @GuardedBy("memory")
    private volatile int memYsize;

    private final Cursor cursor;
    private volatile boolean needMeasure;
    private volatile int charWidth;
    private volatile int charHeight;

    private final Font textFont = new Font("Monospaced", 0, 14);

    public Display() {
        this.cursor = new Cursor(1, TimeUnit.SECONDS);

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
        synchronized (memory) {
            cursor.stop();
            memory.clear();
        }
    }

    public Cursor getTextCanvasCursor() {
        return cursor;
    }

    public void writeAtCursor(int c) {
        Point cursorPoint = cursor.getLogicalPoint();
        writeCharAt(c, cursorPoint.x, cursorPoint.y);
        cursor.advance(1);
    }

    @GuardedBy("memory")
    private void ensureMemorySize(int x, int y) {
        if (memYsize <= y) {
            for (int i = memYsize; i <= y; i++) {
                memory.add(new LinkedList<Integer>());
                memYsize++;
            }
        }
        List<Integer> row = memory.get(y);
        int memXsize = row.size();
        if (memXsize <= x) {
            for (int i = memXsize; i <= x; i++) {
                row.add(0);
            }
        }
    }

    public void writeCharAt(int c, int x, int y) {
        synchronized (memory) {
            ensureMemorySize(x, y);
            memory.get(y).set(x, c);
        }
        repaint();
    }

    public void clear() {
        synchronized (memory) {
            memory.clear();
            memYsize = 0;
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

            for (int i = 0; i < memYsize; i++) {
                List<Integer> row = memory.get(i);
                int memXsize = row.size();
                x = 0;
                for (int col = 0; col < memXsize; col++) {
                    int value = row.get(col);

                    if (value < 32) {
                        continue;
                    }

                    g.drawString("" + (char) value, x, y);
                    x += charWidth;
                }
                y += charHeight;
            }
        }
    }

}
