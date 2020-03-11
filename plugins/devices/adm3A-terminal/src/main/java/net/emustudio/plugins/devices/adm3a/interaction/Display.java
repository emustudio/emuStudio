/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.devices.adm3a.interaction;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.devices.adm3a.TerminalSettings;
import net.emustudio.plugins.devices.adm3a.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
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
@PluginContext(id = "LSI ADM-3A Terminal")
public class Display extends JPanel implements DeviceContext<Short>, TerminalSettings.ChangedObserver, Cursor.LineRoller {
    private static final Logger LOGGER = LoggerFactory.getLogger(Display.class);

    private static final String HERE_IS_CONSTANT = Display.class.getAnnotation(PluginContext.class).id();
    static final Color FOREGROUND = new Color(0, 255, 0);
    static final Color BACKGROUND = Color.BLACK;
    private static final String TERMINAL_FONT_PATH = "/net/emustudio/plugins/devices/adm3a/gui/terminal.ttf";

    private final char[] videoMemory;
    private final int colCount;
    private final int rowCount;

    private final TerminalSettings settings;

    private final Cursor cursor;
    private final LoadCursorPosition loadCursorPosition;
    private volatile DisplayParameters displayParameters;
    private volatile Dimension size;

    private FileWriter outputWriter = null;

    public Display(Cursor cursor, LoadCursorPosition loadCursorPosition, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.loadCursorPosition = Objects.requireNonNull(loadCursorPosition);
        this.colCount = cursor.getColCount();
        this.rowCount = cursor.getRowCount();
        this.videoMemory = new char[rowCount * colCount];
        refillWithSpaces();

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
            font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        }
        return font;
    }

    /**
     * Input from the display is always 0, because the input is captured by an input provider, not by the display.
     *
     * @return 0
     */
    @Override
    public Short readData() {
        return 0;
    }

    public void start() {
        cursor.start(getGraphics(), displayParameters);
    }

    private DisplayParameters measure() {
        Font font = getFont();
        Rectangle2D metrics = font.getStringBounds("W", Utils.getDefaultFrc());
        LineMetrics lineMetrics = font.getLineMetrics("W", Utils.getDefaultFrc());

        int charWidth = (int) metrics.getWidth();
        int charHeight = (int) lineMetrics.getHeight();

        int lineAscent = (int) lineMetrics.getAscent();

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
        StringBuilder sLine = new StringBuilder();

        Graphics2D g2d = (Graphics2D) graphics;
        for (y = 0; y < rowCount; y++) {
            t_y = displayParameters.startY + y * displayParameters.charHeight;
            temp = y * colCount;
            for (x = 0; x < colCount; x++) {
                synchronized (videoMemory) {
                    sLine.append(videoMemory[temp + x]);
                }
            }
            g2d.drawString(sLine.toString(), 1, t_y);
            sLine = new StringBuilder();
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
            } catch (IOException ignored) {
            }
        }
        outputWriter = null;
    }

    @Override
    public void settingsChanged() {
        if (!settings.isGuiSupported()) {
            closeOutputWriter();
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

    private void refillWithSpaces() {
        synchronized (videoMemory) {
            for (int i = 0; i < (rowCount * colCount); i++) {
                videoMemory[i] = ' ';
            }
        }
    }

    public final void clearScreen() {
        refillWithSpaces();
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

    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void writeData(Short data) {
        writeToOutput(data);
        /*
         * if it is special char, interpret it. else just add
         * to "video memory"
         */
        switch (data) {
            case 5: // HERE IS
                insertHereIs();
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

        if (!loadCursorPosition.accept(data) && data >= 32) {
            insertChar((char) (data & 0xFF));
            cursor.move(this);
        }
        repaint();
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    private void insertHereIs() {
        for (char c : Display.HERE_IS_CONSTANT.toCharArray()) {
            insertChar(c);
            cursor.move(this);
        }
    }
}
