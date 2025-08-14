/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@ThreadSafe
public class Cursor {
    public final int columns;
    public final int rows;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    public Cursor(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    void home() {
        cursorPoint.set(new Point());
    }

    void move(int x, int y) {
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
        cursorPoint.set(new Point(x, y));
    }

    void moveForwardsRolling(LineRoller lineRoller) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            newPoint.x++;
            if (newPoint.x >= columns) {
                newPoint.x = 0;
                newPoint.y++;
                // automatic line rolling
                if (newPoint.y >= rows) {
                    lineRoller.rollLine();
                    newPoint.y = (rows - 1);
                }
            }
            return newPoint;
        });
    }

    void moveForwards() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x < (columns - 1)) {
                newPoint.x++;
            }
            return newPoint;
        });
    }

    void moveBackwards() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x > 0) {
                newPoint.x--;
            }
            return newPoint;
        });
    }

    void moveUp() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y > 0) {
                newPoint.y--;
            }
            return newPoint;
        });
    }

    void moveDown(LineRoller lineRoller) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y == (rows - 1)) {
                lineRoller.rollLine();
            } else {
                newPoint.y++;
            }
            return newPoint;
        });
    }

    void carriageReturn() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);
            newPoint.x = 0;
            return newPoint;
        });
    }

    Point getCursorPoint() {
        return cursorPoint.get();
    }

    private void setCursorPoint(Function<Point, Point> changer) {
        Point oldPoint = cursorPoint.get();
        Point newPoint;
        do {
            newPoint = changer.apply(oldPoint);
        } while (!cursorPoint.compareAndSet(oldPoint, newPoint));
    }

    public interface LineRoller {

        void rollLine();
    }
}
