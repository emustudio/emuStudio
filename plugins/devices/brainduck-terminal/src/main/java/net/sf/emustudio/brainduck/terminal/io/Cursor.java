/*
 * Copyright (C) 2014 Peter Jakubčo
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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class Cursor extends Component {
    private final int howOften;
    private final TimeUnit timeUnit;

    private final BlockingDeque<Point> repaintTasks = new LinkedBlockingDeque<>();

    private volatile Point cursorPoint; // set only in swing thread
    private volatile boolean xored = false; // used only in swing thread
    
    @GuardedBy("schedulerLock")
    private ScheduledExecutorService repaintScheduler;
    private final Object schedulerLock = new Object();

    public Cursor(int howOften, TimeUnit timeUnit) {
        this.howOften = howOften;
        this.timeUnit = timeUnit;
        reset();
    }

    public void start() {
        synchronized (schedulerLock) {
            repaintScheduler = Executors.newSingleThreadScheduledExecutor();
            repaintScheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    repaint();
                }
            }, 0, howOften, timeUnit);
        }
    }

    public void stop() {
        synchronized (schedulerLock) {
            if (repaintScheduler != null) {
                repaintScheduler.shutdownNow();
                try {
                    repaintScheduler.awaitTermination(3 * howOften, timeUnit);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void advance(int maxColumn) {
        Point point = getPoint();
        point = point.getLocation();
        if (point.x + 1 > maxColumn) {
            point.y++;
            point.x = 0;
        } else {
            point.x++;
        }
        repaintTasks.add(point);
    }

    public final void reset() {
        repaintTasks.add(new Point());
    }
    
    public Point getPoint() {
        Point point = repaintTasks.peekLast();
        if (point == null) {
            return cursorPoint;
        }
        return point;
    }
    
    private void paintCursor(Graphics g, int row, int col, int width, int height) {
        int x = col * width;
        int y = height + row * height;
        
        g.setXORMode(Color.BLACK);
        g.fillRect(x, y, width, height);
    }

    @Override
    public void paint(Graphics g) {
        // works only for monospaced fonts
        int rectWidth = g.getFontMetrics().charWidth('Y');
        int rectHeight = g.getFontMetrics().getHeight();

        List<Point> tasks = new ArrayList<>();
        repaintTasks.drainTo(tasks);

        Point point = null;
        while (!tasks.isEmpty()) {
            point = tasks.remove(0);
        }

        // get the current point
        Point tmpCursorPoint = cursorPoint;
        if (point == null) {
            point = tmpCursorPoint;
        }

        if (xored && tmpCursorPoint != null && !tmpCursorPoint.equals(point)) {
            paintCursor(g, tmpCursorPoint.x, tmpCursorPoint.y, rectWidth, rectHeight);
            xored = false;
        }
        paintCursor(g, point.x, point.y, rectWidth, rectHeight);
        xored = !xored;
        cursorPoint = point;
    }

}
