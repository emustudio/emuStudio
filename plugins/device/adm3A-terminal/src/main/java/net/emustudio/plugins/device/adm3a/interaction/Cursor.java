/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.adm3a.interaction;

import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@ThreadSafe
public class Cursor {
    private final int columns;
    private final int rows;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    interface LineRoller {

        void rollLine();
    }

    public Cursor(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    int getColumns() {
        return columns;
    }

    int getRows() {
        return rows;
    }

    void home() {
        cursorPoint.set(new Point());
    }

    void set(int x, int y) {
        cursorPoint.set(new Point(x, y));
    }

    void moveForwardsRolling(LineRoller lineRoller) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            newPoint.x++;
            if (newPoint.x > (columns - 1)) {
                newPoint.x = 0;
                newPoint.y++;
                // automatic line rolling
                if (newPoint.y > (rows - 1)) {
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
}
