/*
 * Display.java
 *
 * Created on Utorok, 2007, november 20, 20:15
 *
 * Copyright (C) 2007-2013 Peter Jakubčo
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
 *
 */
package net.sf.emustudio.devices.adm3a.impl;

import emulib.plugins.device.DeviceContext;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class provides emulation of CRT display. It supports double buffering
 * and offers anti-aliasing. For painting it uses Graphics2D class.
 *
 * This class is rewritten example from the book _Java in a Nutshell_
 * by David Flanagan.
 * Written by David Flanagan. Copyright (c) 1996 O'Reilly & Associates.
 *
 * Terminal can interpret ASCII codes from 0-127. Some have special
 * functionality (0-31)
 *
 * @author Peter Jakubčo
 */
public class Display extends Canvas implements DeviceContext<Short>, TerminalSettings.ChangedObserver {
    private final char[] videoMemory;
    private int col_count; // column count in CRT
    private int row_count; // row count in CRT
    private int line_height; // height of the font = height of the line
    private int line_ascent; // Font height above baseline
    private int max_width;   // The width of the terminal
    private int max_height;  // The height of the terminal
    private Timer cursorTimer;
    private CursorPainter cursorPainter;
    private int cursor_x = 0, cursor_y = 0; // position of cursor
    private int char_width = 0;
    private int start_y;

    private Graphics2D dbg;  // graphics for double buffering
    private Image dbImage;   // second buffer
    private FileWriter outputWriter = null;
    private TerminalSettings settings;

    public Display(int cols, int rows, TerminalSettings settings) {
        this.settings = settings;
        this.col_count = cols;
        this.row_count = rows;
        videoMemory = new char[rows * cols];
        clearScreen();
        cursorTimer = new Timer();
        cursorPainter = new CursorPainter();
        cursorTimer.scheduleAtFixedRate(cursorPainter, 0, 800);
        settings.addChangedObserver(this);
    }

    public void setCursorPos(int x, int y) {
        cursor_x = x;
        cursor_y = y;
        repaint();
    }

    protected void measure() {
        FontMetrics fm;
        try {
            fm = getFontMetrics(getFont());
        } catch (Exception e) {
            return;
        }
        if (fm == null) {
            return;
        }
        line_height = fm.getHeight();
        line_ascent = fm.getAscent();
        char_width = fm.stringWidth("W");

        max_width = col_count * char_width;
        max_height = row_count * line_height;

        Dimension d = getSize();
        start_y = 2 * line_ascent + (d.height - max_height) / 2;
    }

    public void destroy() {
        settings.removeChangedObserver(this);
        cursorPainter.stop();
        cursorTimer.cancel();
        closeOutputWriter();
    }

    // Methods to set the various attributes of the component
    @Override
    public void setFont(Font f) {
        super.setFont(f);
        measure();
        repaint();
    }

    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        measure();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(max_width, max_height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(max_width, max_height);
    }

    /**
     * Method clears screen of emulated CRT
     */
    public final void clearScreen() {
        synchronized (videoMemory) {
            for (int i = 0; i < (row_count * col_count); i++) {
                videoMemory[i] = ' ';
            }
        }
        cursor_x = 0;
        cursor_y = 0;
        repaint();
    }

    /**
     * Method inserts char to cursor position. Doesn't move cursor.
     * @param c char to insert
     */
    private void insertChar(char c) {
        synchronized (videoMemory) {
            videoMemory[cursor_y * col_count + cursor_x] = c;
        }
    }

    /**
     * Moves cursor backward in one position. Don't move cursor vertically.
     */
    private void cursorBack() {
        if (cursor_x <= 0) {
            return;
        }
        cursor_x--;
    }

    /**
     * Move cursor foreward in one position, also vertically if needed.
     */
    private void moveCursor() {
        cursor_x++;
        if (cursor_x > (col_count - 1)) {
            cursor_x = 0;
            cursor_y++;
            // automatic line rolling
            if (cursor_y > (row_count - 1)) {
                rollLine();
                cursor_y = (row_count - 1);
            }
        }
    }

    /**
     * Rolls screen by 1 row vertically up.
     * The principle: moves all lines beginning from 1 in videomemory into
     * line 0, and previous value of line 0 will be lost.
     */
    public void rollLine() {
        synchronized (videoMemory) {
            for (int i = col_count; i < (col_count * row_count); i++) {
                videoMemory[i - col_count] = videoMemory[i];
            }
            for (int i = col_count * row_count - col_count; i < (col_count * row_count); i++) {
                videoMemory[i] = ' ';
            }
        }
        repaint();
    }

    /**
     * Override previous update method in order to implement double-buffering.
     * As a second buffer is used Image object.
     */
    @Override
    public void update(Graphics g) {
        // initialize buffer if needed
        if (dbImage == null) {
            dbImage = createImage(this.getSize().width, this.getSize().height);
            dbg = (Graphics2D) dbImage.getGraphics();
        }
        // for antialiasing text (hope it wont be turned on if antiAliasing
        // = false)
        if (settings.isAntiAliasing()) {
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            dbg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            dbg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        // clear screen in background
        dbg.setColor(getBackground());
        dbg.fillRect(0, 0, this.getSize().width, this.getSize().height);

        // draw elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // draw image on the screen
        g.drawImage(dbImage, 0, 0, this);
    }

    /**
     * This overridement paints video memory into screen. Every char
     * in videomemory represents ASCII form of the char.
     */
    @Override
    public void paint(Graphics g) {
        int t_y;
        int x, y;
        int temp;
        String sLine = "";

        Graphics2D g2d = (Graphics2D) g;
        for (y = 0; y < row_count; y++) {
            t_y = start_y + y * line_height;
            temp = y * col_count;
            for (x = 0; x < col_count; x++) {
                synchronized (videoMemory) {
                    sLine += videoMemory[temp + x];
                }
            }
            g2d.drawString(sLine, 1, t_y);
            sLine = "";
        }
    }
    
    private void openOutputWriter() {
        try {
            File tmpFile = new File(settings.getOutputFileName());
            outputWriter = new FileWriter(tmpFile);
        } catch (IOException e) {
        }
    }

    private void closeOutputWriter() {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException e) {
            }
        }
        outputWriter = null;
    }

    @Override
    public void settingsChanged() {
        if (settings.isNoGUI()) {
            openOutputWriter();
        } else {
            closeOutputWriter();
        }
        if (settings.isAntiAliasing()) {
            repaint();
        }
    }

    private class CursorPainter extends TimerTask {

        @Override
        public void run() {
            Graphics displayGraphics = getGraphics();
            if (displayGraphics == null) {
                return;
            }
            displayGraphics.setXORMode(Color.BLACK);
            displayGraphics.fillRect(cursor_x * char_width, cursor_y * line_height
                    + start_y - line_height, char_width, line_height);
            displayGraphics.setPaintMode();
        }

        public void stop() {
            this.cancel();
        }
    }

    /**
     * Input from the display is always 0, because the input is captured
     * by an input provider, not by the display.
     * 
     * @return 0
     */
    @Override
    public Short read() {
        return 0;
    }

    private void writeToOutput(short val) {
        if (outputWriter != null) {
            try {
                outputWriter.write((char) val);
                outputWriter.flush();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void write(Short data) {
        measure();

        writeToOutput(data);
        /*
         * if it is special char, interpret it. else just add
         * to "video memory"
         */
        switch (data) {
            case 7:
                return; /* bell */
            case 8:
                cursorBack();
                repaint();
                return; /* backspace*/
            case 0x0A: /* line feed */
                cursor_y++;
                cursor_x = 0;
                if (cursor_y > (row_count - 1)) {
                    cursor_y = (row_count - 1);
                    rollLine();
                }
                repaint(); // to be sure for erasing cursor
                return;
            case 0x0D:
                cursor_x = 0;
                repaint(); // to be sure for erasing cursor
                return; /* carriage return */
        }
        insertChar((char)(data & 0xFF));
        moveCursor();
        repaint();
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

}
