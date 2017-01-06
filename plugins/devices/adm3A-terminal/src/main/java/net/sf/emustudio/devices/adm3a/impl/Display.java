/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Terminal can interpret ASCII codes from 0-127. Some have special purpose (0-31).
 */
@ContextType(id = "LSI ADM-3A Terminal")
public class Display extends JPanel implements DeviceContext<Short>, TerminalSettings.ChangedObserver, Cursor.LineRoller {
    private static final Logger LOGGER = LoggerFactory.getLogger(Display.class);

    private static final String HERE_IS_CONSTANT = Display.class.getAnnotation(ContextType.class).id();
    public static final Color FOREGROUND = new Color(0, 255, 0);
    public static final Color BACKGROUND = Color.BLACK;
    public static final String TERMINAL_FONT_PATH = "/net/sf/emustudio/devices/adm3a/gui/terminal.ttf";

    private final char[] videoMemory;
    private final int colCount;
    private final int rowCount;

    private final TerminalSettings settings;

    private final Cursor cursor;
    private volatile DisplayParameters displayParameters;
    private volatile Dimension size;

    private FileWriter outputWriter = null;

    public Display(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.colCount = cursor.getColCount();
        this.rowCount = cursor.getRowCount();
        this.videoMemory = new char[rowCount * colCount];

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setDoubleBuffered(true);
        setFont(loadFont());
        this.displayParameters = measure();
        this.size = new Dimension(displayParameters.maxWidth, displayParameters.maxHeight);

        settings.addChangedObserver(this);
    }

    private Font loadFont() {
        Font font;
        try (InputStream fin = getClass().getResourceAsStream(TERMINAL_FONT_PATH)) {
            font = Font.createFont(Font.TRUETYPE_FONT, fin).deriveFont(Font.PLAIN, 14f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            LOGGER.error("Could not load custom font, using default monospaced font", e);
            font = new Font(Font.MONOSPACED, 0, 14);
        }
        return font;
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    /**
     * Input from the display is always 0, because the input is captured by an input provider, not by the display.
     *
     * @return 0
     */
    @Override
    public Short read() {
        return 0;
    }

    public void start() {
        cursor.start(getGraphics(), displayParameters);
    }

    private DisplayParameters measure() {
        Font font = getFont();
        Rectangle2D metrics = font.getStringBounds("W", Utils.getDefaultFrc());
        LineMetrics lineMetrics = font.getLineMetrics("W", Utils.getDefaultFrc());

        int charWidth = (int)metrics.getWidth();
        int charHeight = (int)lineMetrics.getHeight();

        int lineAscent = (int)lineMetrics.getAscent();

        int maxWidth = colCount * charWidth;
        int maxHeight = rowCount * charHeight;

        int startY;
        if (size == null) {
            startY = 2 * lineAscent;
        } else {
            startY = (size.height - maxHeight) / 2;
            if (startY < lineAscent) {
                startY = lineAscent;
            }
        }
        return new DisplayParameters(charHeight, charWidth, startY, maxWidth, maxHeight);
    }

    public void destroy() {
        settings.removeChangedObserver(this);
        closeOutputWriter();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.size = getSize();
        this.displayParameters = measure();
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        this.size = getSize();
        this.displayParameters = measure();
    }

    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

    @Override
    public Dimension getMinimumSize() {
        return this.size;
    }

    /**
     * This overridement paints video memory into screen. Every char
     * in videomemory represents ASCII form of the char.
     */
    @Override
    public void paintComponent(Graphics graphics) {
        Dimension dimension = size;
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, dimension.width, dimension.height);
        graphics.setColor(FOREGROUND);

        cursor.reset();

        int t_y;
        int x, y;
        int temp;
        String sLine = "";

        Graphics2D g2d = (Graphics2D) graphics;
        for (y = 0; y < rowCount; y++) {
            t_y = displayParameters.startY + y * displayParameters.charHeight;
            temp = y * colCount;
            for (x = 0; x < colCount; x++) {
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
            LOGGER.error("Could not open file for writing output: {}", settings.getOutputFileName(), e);
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

    private void writeToOutput(short val) {
        if (outputWriter != null) {
            try {
                outputWriter.write((char) val);
                outputWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write to file: " + settings.getOutputFileName(), e);
            }
        }
    }

    private void insertChar(char c) {
        Point cursorPoint = cursor.getPoint();
        synchronized (videoMemory) {
            videoMemory[cursorPoint.y * colCount + cursorPoint.x] = c;
        }
    }

    public final void clearScreen() {
        synchronized (videoMemory) {
            for (int i = 0; i < (rowCount * colCount); i++) {
                videoMemory[i] = ' ';
            }
        }
        cursor.home();
        repaint();
    }

    @Override
    public void rollLine() {
        synchronized (videoMemory) {
            System.arraycopy(videoMemory, colCount, videoMemory, 0, colCount * rowCount - colCount);
            for (int i = colCount * rowCount - colCount; i < (colCount * rowCount); i++) {
                videoMemory[i] = ' ';
            }
        }
        repaint();
    }

    private void insertString(String string) {
        for (char c : string.toCharArray()) {
            insertChar(c);
            cursor.move(this);
        }
    }

    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void write(Short data) {
        writeToOutput(data);
        /*
         * if it is special char, interpret it. else just add
         * to "video memory"
         */
        switch (data) {
            case 5: // HERE IS
                insertString(HERE_IS_CONSTANT);
                break;
            case 7: // BELL
                return;
            case 8: // BACKSPACE
                cursor.back();
                break;
            case 0x0A: // line feed
                cursor.down(this);
                break;
            case 0x0B: // VT
                cursor.up();
                break;
            case 0x0C: // FF
                cursor.forward();
                break;
            case 0x0D: // CARRIAGE RETURN
                cursor.carriageReturn();
                break;
            case 0x0E: // SO
            case 0x0F: // SI
                return;
            case 0x1A: // clear screen
                clearScreen();
                return;
            case 0x1B: // initiates load cursor operation
            case 0x1E: // homes cursor
                cursor.home();
                break;
        }

        if (data >= 32) {
            insertChar((char) (data & 0xFF));
            cursor.move(this);
        }
        repaint();
    }

}
