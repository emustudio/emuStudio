/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.brainduck.terminal.io;

import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ThreadSafe
public class Cursor {
    private final int howOften;
    private final TimeUnit timeUnit;

    private volatile Display canvas;

    private volatile int charWidth;
    private volatile int charHeight;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    private final ScheduledExecutorService repaintScheduler = Executors.newSingleThreadScheduledExecutor();

    Cursor(int howOften, TimeUnit timeUnit) {
        this.howOften = howOften;
        this.timeUnit = Objects.requireNonNull(timeUnit);
        reset();
    }

    void start(Display canvas) {
        this.canvas = Objects.requireNonNull(canvas);
        this.charWidth = canvas.getCharWidth();
        this.charHeight = canvas.getCharHeight();

        repaintScheduler.scheduleWithFixedDelay(this::repaint, 0, howOften, timeUnit);
    }

    void stop() {
        repaintScheduler.shutdownNow();
        try {
            repaintScheduler.awaitTermination(3 * howOften, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void back() {
        Point oldPoint;
        Point point;
        do {
            oldPoint = cursorPoint.get();
            point = new Point(oldPoint);
            if (point.x - 1 < 0) {
                point.x = 0;
            } else {
                point.x--;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, point));
    }

    public void newLine() {
        Point oldPoint;
        Point point;
        do {
            oldPoint = cursorPoint.get();
            point = new Point(oldPoint);
            point.y++;
        } while (!cursorPoint.compareAndSet(oldPoint, point));
    }

    public void carriageReturn() {
        Point oldPoint;
        Point point;
        do {
            oldPoint = cursorPoint.get();
            point = new Point(oldPoint);
            point.x = 0;
        } while (!cursorPoint.compareAndSet(oldPoint, point));
    }

    public void advance(final int count) {
        Point oldPoint;
        Point point;
        do {
            oldPoint = cursorPoint.get();
            point = new Point(oldPoint);
            for (int i = 0; i < count; i++) {
                point.x++;
            }
        } while (!cursorPoint.compareAndSet(oldPoint, point));
    }

    public final void reset() {
        Point oldPoint;
        Point point;
        do {
            oldPoint = cursorPoint.get();
            point = new Point();
        } while (!cursorPoint.compareAndSet(oldPoint, point));
    }
    
    Point getLogicalPoint() {
        return new Point(cursorPoint.get());
    }
    
    private void repaint() {
        Graphics graphics = canvas.getGraphics();
        if (graphics == null) {
            return;
        }

        Point point = cursorPoint.get();

        graphics.setXORMode(Color.WHITE);
        graphics.fillRect(point.x * charWidth, point.y * charHeight, charWidth, charHeight);
        graphics.setPaintMode();
    }

}
