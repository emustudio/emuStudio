package net.sf.emustudio.devices.adm3a.impl;

import net.jcip.annotations.ThreadSafe;

import java.awt.Graphics;
import java.awt.Point;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@ThreadSafe
public class Cursor {
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final int colCount;
    private final int rowCount;
    private final AtomicReference<Point> cursorPoint = new AtomicReference<>(new Point());
    private volatile ScheduledFuture cursorPainter;

    public interface LineRoller {

        void rollLine();
    }

    // run by only one thread
    private class CursorPainter implements Runnable {
        private final Graphics graphics;
        private final int charHeight;
        private final int charWidth;
        private final int startY;

        private volatile boolean nowVisible;
        private volatile Point visiblePoint;

        private CursorPainter(Graphics graphics, int charWidth, int charHeight, int startY) {
            this.charHeight = charHeight;
            this.charWidth = charWidth;
            this.startY = startY;
            this.graphics = Objects.requireNonNull(graphics);
        }

        @Override
        public void run() {
            if (!nowVisible) {
                visiblePoint = cursorPoint.get();
            }

            graphics.setXORMode(Display.BACKGROUND);
            graphics.setColor(Display.FOREGROUND);
            graphics.fillRect(visiblePoint.x * charWidth, visiblePoint.y * charHeight + startY - charHeight, charWidth, charHeight);
            graphics.setPaintMode();
            nowVisible = !nowVisible;
        }

    }

    public Cursor(int colCount, int rowCount) {
        this.colCount = colCount;
        this.rowCount = rowCount;
    }

    public void home() {
        cursorPoint.set(new Point());
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

    public synchronized void start(Graphics graphics, int charWidth, int charHeight, int startY) {
        if (cursorPainter != null) {
            throw new IllegalStateException("Cursor painter has already started");
        }
        cursorPainter = executorService.scheduleWithFixedDelay(
                    new CursorPainter(graphics, charWidth, charHeight, startY), 0, 800, MILLISECONDS
        );
    }

    public synchronized void destroy() {
        if (cursorPainter != null) {
            cursorPainter.cancel(true);
        }
        cursorPainter = null;
        executorService.shutdownNow();
    }
}
