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

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    public Cursor(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public synchronized void setSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
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
        int tmpRows;
        int tmpColumns;

        synchronized (this) {
            tmpRows = rows - 1;
            tmpColumns = columns - 1;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            newPoint.x++;
            if (newPoint.x > tmpColumns) {
                newPoint.x = 0;
                newPoint.y++;
                // automatic line rolling
                if (newPoint.y > tmpRows) {
                    display.rollUp();
                    newPoint.y = tmpRows;
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
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y > 0) {
                newPoint.y--;
            } else {
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
        int tmpRows;
        synchronized (this) {
            tmpRows = rows - 1;
        }

        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y == tmpRows) {
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
