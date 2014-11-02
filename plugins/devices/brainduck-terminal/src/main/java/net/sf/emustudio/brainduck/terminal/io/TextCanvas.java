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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class TextCanvas extends JComponent {
    @GuardedBy("this")
    private final int[][] memory;
    private final int width;
    private final int height;
    private final Cursor cursor;
    private volatile boolean needMeasure;

    private final Font textFont = Font.decode("monospace-PLAIN-14");

    public TextCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        this.memory = new int[height][width];
        this.cursor = new Cursor(500, TimeUnit.MILLISECONDS);

        this.setBackground(Color.WHITE);
        this.setForeground(Color.BLACK);
        this.setFont(textFont);
        this.add(cursor);
        this.setPreferredSize(new Dimension(width, height));
        needMeasure = true;
    }
    
    public void start() {
        cursor.start();
    }
    
    public void stop() {
        cursor.stop();
    }
    
    public void writeAtCursor(int c) {
        Point cursorPoint = cursor.getPoint();
        writeCharAt(c, cursorPoint.x, cursorPoint.y);
        cursor.advance(width);
    }
    
    public synchronized void writeCharAt(int c, int x, int y) {
        memory[y][x] = c;
        repaint();
    }
    
    public int getCharAt(int x, int y) {
        return memory[y][x];
    }
    
    public synchronized void clear() {
        for (int[] row : memory) {
            for (int col = 0; col < row.length; col++) {
                row[col] = 0;
            }
        }
        cursor.reset();
        repaint();
    }
    
    @Override
    public synchronized void paint(Graphics g) {
        g.setFont(textFont);
        FontMetrics fontMetrics = g.getFontMetrics();
        int charWidth = fontMetrics.charWidth('Y');
        int charHeight = fontMetrics.getHeight();

        if (needMeasure) {
            Dimension newDimension = new Dimension(width * charWidth, height + height * charHeight);
            setPreferredSize(newDimension);
            setSize(newDimension);
            needMeasure = false;
        }
        
        for (int row = 0; row < memory.length; row++) {
            int y = charHeight + row * charHeight;
            String string = "";
            for (int col = 0; col < memory[row].length; col++) {
                int value = memory[row][col];
                if (value > 0) {
                    string += value;
                } else if (value < 0) {
                    break;
                } else {
                    string += " ";
                }
            }
            g.drawString(string, 0, y);
        }
    }
    
}
