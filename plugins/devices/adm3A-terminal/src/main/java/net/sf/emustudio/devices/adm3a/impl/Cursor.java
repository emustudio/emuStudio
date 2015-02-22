/*
 * Copyright (C) 2014-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.adm3a.impl;

import net.jcip.annotations.ThreadSafe;

import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@ThreadSafe
public class Cursor {
    private final int colCount;
    private final int rowCount;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    private volatile boolean reset = true;

    private final CursorPainter cursorPainter = new CursorPainter();
    private final Timer cursorPainterTimer = new Timer(0, cursorPainter);

    public interface LineRoller {

        void rollLine();
    }

    // run by only one thread
    private class CursorPainter implements ActionListener {
        private Point visiblePoint;
        private volatile DisplayParameters displayParameters;
        private volatile Graphics graphics;

        public void setDisplayParameters(DisplayParameters displayParameters) {
            this.displayParameters = Objects.requireNonNull(displayParameters);
        }

        public void setGraphics(Graphics graphics) {
            this.graphics = Objects.requireNonNull(graphics);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (displayParameters == null || graphics == null) {
                return;
            }
            if (reset || visiblePoint == null) {
                visiblePoint = cursorPoint.get();
            }

            graphics.setXORMode(Display.BACKGROUND);
            graphics.setColor(Display.FOREGROUND);
            graphics.fillRect(
                    visiblePoint.x * displayParameters.charWidth,
                    visiblePoint.y * displayParameters.charHeight + displayParameters.startY - displayParameters.charHeight,
                    displayParameters.charWidth, displayParameters.charHeight);
            graphics.setPaintMode();
            reset = !reset;
        }
    }

    public Cursor(int colCount, int rowCount) {
        this.colCount = colCount;
        this.rowCount = rowCount;
    }

    public void home() {
        cursorPoint.set(new Point());
    }

    public void reset() {
        this.reset = true;
    }

    public int getColCount() {
        return colCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Point getPoint(){
        return new Point(cursorPoint.get());
    }

    public void set(int x, int y) {
        cursorPoint.set(new Point(x, y));
    }

    public void move(LineRoller lineRoller) {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);

            newPoint.x++;
            if (newPoint.x > (colCount - 1)) {
                newPoint.x = 0;
                newPoint.y++;
                // automatic line rolling
                if (newPoint.y > (rowCount - 1)) {
                    lineRoller.rollLine();
                    newPoint.y = (rowCount - 1);
                }
            }
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void back() {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);

            if (newPoint.x > 0) {
                newPoint.x--;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void up() {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);

            if (newPoint.y > 0) {
                newPoint.y--;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void down(LineRoller lineRoller) {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);

            if (newPoint.y == (rowCount - 1)) {
                lineRoller.rollLine();
            } else {
                newPoint.y++;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void forward() {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);

            if (newPoint.x < (colCount - 1)) {
                newPoint.x++;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void carriageReturn() {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = new Point(oldPoint);
            newPoint.x = 0;
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public synchronized void start(Graphics graphics, DisplayParameters displayParameters) {
        cursorPainterTimer.stop();

        cursorPainter.setDisplayParameters(displayParameters);
        cursorPainter.setGraphics(graphics);
        cursorPainterTimer.setDelay(800);
        cursorPainterTimer.start();
    }

    public synchronized void destroy() {
        cursorPainterTimer.stop();
    }
}
