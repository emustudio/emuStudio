/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.plugins.device.vt100.api.Display;
import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@ThreadSafe
public class Cursor {
    private volatile int columns;
    private volatile int rows;

    // Scrolling region margins (0-based, inclusive)
    private volatile int scrollTop;
    private volatile int scrollBottom;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    public Cursor(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.scrollTop = 0;
        this.scrollBottom = rows - 1;
    }

    public synchronized void setSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.scrollTop = 0;
        this.scrollBottom = rows - 1;
    }

    /**
     * Sets the scrolling region (DECSTBM).
     * Both top and bottom are 0-based row indices, inclusive.
     * The cursor is moved to the home position after this call.
     *
     * @param top    top margin (0-based, inclusive)
     * @param bottom bottom margin (0-based, inclusive)
     */
    public synchronized void setScrollingRegion(int top, int bottom) {
        if (top < 0) {
            top = 0;
        }
        if (bottom >= rows) {
            bottom = rows - 1;
        }
        if (top < bottom) {
            this.scrollTop = top;
            this.scrollBottom = bottom;
        }
        home();
    }

    /**
     * Returns the top margin of the scrolling region (0-based).
     */
    public int getScrollTop() {
        return scrollTop;
    }

    /**
     * Returns the bottom margin of the scrolling region (0-based).
     */
    public int getScrollBottom() {
        return scrollBottom;
    }

    public void home() {
        cursorPoint.set(new Point());
    }

    public void move(int x, int y) {
        Point oldPoint;
        Point newPoint;

        do {
            oldPoint = cursorPoint.get();
            if (x < 0) {
                x = 0;
            } else if (x >= columns) {
                x = columns - 1;
            }
            if (y < 0) {
                y = 0;
            } else if (y >= rows) {
                y = rows - 1;
            }
            newPoint = new Point(x, y);
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public void move(Point point) {
        cursorPoint.set(point);
    }

    public void moveForwardsRolling(Display display) {
        int tmpScrollBottom;
        int tmpColumns;

        synchronized (this) {
            tmpScrollBottom = scrollBottom;
            tmpColumns = columns - 1;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            newPoint.x++;
            if (newPoint.x > tmpColumns) {
                newPoint.x = 0;
                newPoint.y++;
                // automatic line rolling within scrolling region
                if (newPoint.y > tmpScrollBottom) {
                    display.rollUp();
                    newPoint.y = tmpScrollBottom;
                }
            }
            return newPoint;
        });
    }

    public void moveForwards() {
        moveForwards(1);
    }

    public void moveForwards(int count) {
        int tmpColumns;
        synchronized (this) {
            tmpColumns = columns - 1;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if ((newPoint.x + count) <= tmpColumns) {
                newPoint.x += count;
            } else {
                newPoint.x = tmpColumns;
            }
            return newPoint;
        });
    }

    public void moveBackwards() {
        moveBackwards(1);
    }

    public void moveBackwards(int count) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x - count > 0) {
                newPoint.x -= count;
            } else {
                newPoint.x = 0;
            }
            return newPoint;
        });
    }


    public void moveUpRolling(Display display) {
        int tmpScrollTop = scrollTop;

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y > tmpScrollTop) {
                newPoint.y--;
            } else if (newPoint.y == tmpScrollTop) {
                display.rollDown();
            }
            return newPoint;
        });
    }

    public void moveUp(int lines) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y > lines) {
                newPoint.y -= lines;
            } else {
                newPoint.y = 0;
            }
            return newPoint;
        });
    }

    public void moveDownRolling(Display display) {
        int tmpScrollBottom;
        synchronized (this) {
            tmpScrollBottom = scrollBottom;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y == tmpScrollBottom) {
                display.rollUp();
            } else {
                newPoint.y++;
            }
            return newPoint;
        });
    }

    public void moveDown() {
        moveDown(1);
    }

    public void moveDown(int lines) {
        int tmpRows;
        synchronized (this) {
            tmpRows = rows;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y < (tmpRows - lines)) {
                newPoint.y += lines;
            } else {
                newPoint.y = tmpRows - 1;
            }
            return newPoint;
        });
    }

    public void carriageReturn() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);
            newPoint.x = 0;
            return newPoint;
        });
    }

    public synchronized Rectangle getRect() {
        Point point = cursorPoint.get();
        return new Rectangle(point.x, point.y, columns, rows);
    }

    private void setCursorPoint(Function<Point, Point> changer) {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = changer.apply(oldPoint);
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }
}
