package net.sf.emustudio.devices.adm3a.impl;

import java.awt.Graphics;
import java.awt.Point;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Cursor {
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture cursorPainter;

    private volatile int cursorX = 0, cursorY = 0; // position of cursor
    private final int colCount; // column count in CRT
    private final int rowCount; // row count in CRT

    public interface LineRoller {

        void rollLine();
    }

    private class CursorPainter implements Runnable {
        private final Graphics graphics;
        private final int charHeight; // height of the font = height of the line
        private final int charWidth;
        private final int startY;

        private CursorPainter(Graphics graphics, int charWidth, int charHeight, int startY) {
            this.charHeight = charHeight;
            this.charWidth = charWidth;
            this.startY = startY;
            this.graphics = Objects.requireNonNull(graphics);
        }

        @Override
        public void run() {
            graphics.setXORMode(Display.BACKGROUND);
            graphics.setColor(Display.FOREGROUND);
            graphics.fillRect(cursorX * charWidth, cursorY * charHeight + startY - charHeight, charWidth, charHeight);
            graphics.setPaintMode();
        }

    }

    public Cursor(int colCount, int rowCount) {
        this.colCount = colCount;
        this.rowCount = rowCount;
    }

    public void home() {
        cursorX = 0;
        cursorY = 0;
    }

    public Point getPoint(){
        return new Point(cursorX, cursorY);
    }

    public void set(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    public void move(LineRoller lineRoller) {
        cursorX++;
        if (cursorX > (colCount - 1)) {
            cursorX = 0;
            cursorY++;
            // automatic line rolling
            if (cursorY > (rowCount - 1)) {
                lineRoller.rollLine();
                cursorY = (rowCount - 1);
            }
        }
    }

    public void back() {
        if (cursorX > 0) {
            cursorX--;
        }
    }

    public void up() {
        if (cursorY > 0) {
            cursorY--;
        }
    }

    public void down(LineRoller lineRoller) {
        if (cursorY == (rowCount - 1)) {
            lineRoller.rollLine();
        } else {
            cursorY++;
        }
    }

    public void forward() {
        if (cursorX < (colCount - 1)) {
            cursorX++;
        }
    }

    public void carriageReturn() {
        cursorX = 0;
    }

    public void start(Graphics graphics, int charWidth, int charHeight, int startY) {
        if (cursorPainter != null) {
            cursorPainter.cancel(true);
        }
        cursorPainter = executorService.scheduleWithFixedDelay(
                new CursorPainter(graphics, charWidth, charHeight, startY), 0, 800, MILLISECONDS
        );
    }

    public void destroy() {
        cursorPainter.cancel(true);
        cursorPainter = null;
        executorService.shutdownNow();
    }
}
