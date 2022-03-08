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
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.emustudio.plugins.device.adm3a.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Terminal can interpret ASCII codes from 0-127. Some have special purpose (0-31).
 */
@PluginContext(id = "LSI ADM-3A Terminal")
public class Display extends JPanel implements DeviceContext<Byte>, TerminalSettings.ChangedObserver, Cursor.LineRoller, ActionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Display.class);

    private static final String HERE_IS_CONSTANT = Display.class.getAnnotation(PluginContext.class).id();
    static final Color FOREGROUND = new Color(0, 255, 0);
    static final Color BACKGROUND = Color.BLACK;
    private static final String TERMINAL_FONT_PATH = "/net/emustudio/plugins/device/adm3a/gui/terminal.ttf";
    private final Font terminalFont;

    private final char[] videoMemory;
    private final int columns;
    private final int rows;

    private final TerminalSettings settings;

    private final Cursor cursor;
    private volatile boolean cursorShouldBePainted;

    private final LoadCursorPosition loadCursorPosition;
    private final Timer cursorTimer = new Timer(800, this);

    private volatile DisplayParameters displayParameters;
    private volatile Dimension size;

    private FileWriter outputWriter = null;

    public Display(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.loadCursorPosition = new LoadCursorPosition(cursor);
        this.columns = cursor.getColumns();
        this.rows = cursor.getRows();
        this.videoMemory = new char[rows * columns];

        this.terminalFont = loadFont();
        fillWithSpaces();

        setForeground(FOREGROUND);
        setBackground(BACKGROUND);
        setDoubleBuffered(true);
        setOpaque(true);
        setFont(terminalFont);
        this.displayParameters = measure();
        this.size = new Dimension(displayParameters.maxWidth, displayParameters.maxHeight);

        if (!settings.isGuiSupported()) {
            openOutputWriter();
        }

        settings.addChangedObserver(this);
    }

    /**
     * Input from the display is always 0, because the input is captured by an input provider, not by the display.
     *
     * @return 0
     */
    @Override
    public Byte readData() {
        return 0;
    }

    public synchronized void startCursor() {
        cursorTimer.restart();
    }

    public synchronized void destroy() {
        cursorTimer.stop();
        settings.removeChangedObserver(this);
        closeOutputWriter();
    }

    public final void clearScreen() {
        fillWithSpaces();
        cursor.home();
        repaint();
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

    @Override
    public void paintComponent(Graphics graphics) {
        Dimension dimension = size;
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        int t_y;
        int x, y;
        int temp;
        StringBuilder sLine = new StringBuilder();

        int lineHeight = graphics.getFontMetrics().getHeight();
        graphics.setColor(FOREGROUND);
        Graphics2D g2d = (Graphics2D) graphics;
        for (y = 0; y < rows; y++) {
            t_y = (y + 1) * lineHeight;
            temp = y * columns;
            for (x = 0; x < columns; x++) {
                synchronized (videoMemory) {
                    sLine.append(videoMemory[temp + x]);
                }
            }
            g2d.drawString(sLine.toString(), 1, t_y);
            sLine = new StringBuilder();
        }

        paintCursor(graphics);
    }

    @Override
    public void settingsChanged() {
        if (settings.isAntiAliasing()) {
            repaint();
        }
    }

    @Override
    public void rollLine() {
        synchronized (videoMemory) {
            System.arraycopy(videoMemory, columns, videoMemory, 0, columns * rows - columns);
            for (int i = columns * rows - columns; i < (columns * rows); i++) {
                videoMemory[i] = ' ';
            }
        }
        repaint();
    }

    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void writeData(Byte data) {
        writeToOutput(data);
        /*
         * if it is special char, interpret it. else just add to "video memory"
         */
        switch (data) {
            case 5: // HERE IS
                insertHereIs();
                break;
            case 7: // BELL
                return;
            case 8: // BACKSPACE
                cursor.moveBackwards();
                break;
            case 0x0A: // line feed
                cursor.moveDown(this);
                break;
            case 0x0B: // VT
                cursor.moveUp();
                break;
            case 0x0C: // FF
                cursor.moveForwards();
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

        if (loadCursorPosition.notAccepted(data) && data >= 32) {
            drawChar((char) (data & 0xFF));
            cursor.moveForwardsRolling(this);
        }
        repaint();
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    private void insertHereIs() {
        for (char c : Display.HERE_IS_CONSTANT.toCharArray()) {
            drawChar(c);
            cursor.moveForwardsRolling(this);
        }
    }

    private void writeToOutput(short val) {
        if (outputWriter != null) {
            try {
                outputWriter.write((char) val);
                outputWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write to file: " + settings.getOutputPath(), e);
            }
        }
    }

    private void drawChar(char c) {
        Point cursorPoint = cursor.getCursorPoint();
        synchronized (videoMemory) {
            videoMemory[cursorPoint.y * columns + cursorPoint.x] = c;
        }
    }

    private void fillWithSpaces() {
        synchronized (videoMemory) {
            Arrays.fill(videoMemory, ' ');
        }
    }

    private void openOutputWriter() {
        try {
            outputWriter = new FileWriter(settings.getOutputPath().toFile());
        } catch (IOException e) {
            LOGGER.error("Could not open file for writing output: {}", settings.getOutputPath(), e);
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

    private Font loadFont() {
        Font font;
        try (InputStream fin = getClass().getResourceAsStream(TERMINAL_FONT_PATH)) {
            font = Font.createFont(Font.TRUETYPE_FONT, fin).deriveFont(Font.PLAIN, 15f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            LOGGER.error("Could not load custom font, using default monospaced font", e);
            font = new Font(Font.MONOSPACED, Font.PLAIN, 15);
        }
        return font;
    }

    private void paintCursor(Graphics graphics) {
        if (!cursorShouldBePainted) {
            Point paintPoint = cursor.getCursorPoint();

            graphics.setXORMode(Display.BACKGROUND);
            graphics.setColor(Display.FOREGROUND);

            Rectangle2D fontRectangle = terminalFont.getMaxCharBounds(graphics.getFontMetrics().getFontRenderContext());
            int lineHeight = graphics.getFontMetrics().getHeight();

            int x = 2 + (int)(paintPoint.x * fontRectangle.getWidth());
            int y = 3 + (paintPoint.y * lineHeight);

            graphics.fillRect(x, y, (int)fontRectangle.getWidth(), (int)fontRectangle.getHeight());
            graphics.setPaintMode();

            cursorShouldBePainted = true;
        } else {
            cursorShouldBePainted = false;
        }
    }

    private DisplayParameters measure() {
        Font font = getFont();
        Rectangle2D metrics = font.getStringBounds("W", Utils.getDefaultFrc());
        LineMetrics lineMetrics = font.getLineMetrics("W", Utils.getDefaultFrc());

        int charWidth = (int) metrics.getWidth();
        int charHeight = (int) lineMetrics.getHeight();

        int maxWidth = columns * charWidth;
        int maxHeight = rows * charHeight;

        return new DisplayParameters(maxWidth, maxHeight);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() != null && e.getSource() == cursorTimer) {
            repaint();
        }
    }
}
