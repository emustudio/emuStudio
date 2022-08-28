package net.emustudio.plugins.device.zxspectrum.display.io;

import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@ThreadSafe
public class Cursor {
    private final int columns;
    private final int rows;
    private final int tabs;

    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());

    public Cursor(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.tabs = columns / 4;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public void home() {
        cursorPoint.set(new Point());
    }

    public void set(int x, int y) {
        cursorPoint.set(new Point(x, y));
    }

    public void printComma(LineRoller lineRoller) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x < 16) {
                newPoint.x = 16;
            } else {
                newPoint.x = 0;
                newPoint.y++;
                if (newPoint.y > (rows - 1)) {
                    lineRoller.rollLine();
                    newPoint.y = (rows - 1);
                }
            }
            return newPoint;
        });
    }

    public void moveForwardsRolling(LineRoller lineRoller) {
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

    public void moveForwards() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x < (columns - 1)) {
                newPoint.x++;
            }
            return newPoint;
        });
    }

    public void moveForwardsTab() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x < (columns - 1)) {
                newPoint.x = ((newPoint.x + 4) / 4) * 4;
            }
            return newPoint;
        });
    }

    public void moveBackwards() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.x > 0) {
                newPoint.x--;
            }
            return newPoint;
        });
    }

    public void moveUp() {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y > 0) {
                newPoint.y--;
            }
            return newPoint;
        });
    }

    public void moveDown(LineRoller lineRoller) {
        setCursorPoint(oldPoint -> {
            Point newPoint = new Point(oldPoint);

            if (newPoint.y == (rows - 1)) {
                lineRoller.rollLine();
            } else {
                newPoint.y++;
            }
            lineRoller.clearLine(newPoint.x, newPoint.y);
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
