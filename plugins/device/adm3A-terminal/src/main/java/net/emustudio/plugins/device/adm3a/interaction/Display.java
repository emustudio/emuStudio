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
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.emustudio.plugins.device.adm3a.api.OutputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Terminal can interpret ASCII codes from 0-127. Some have special purpose (0-31).
 */
@PluginContext(id = "LSI ADM-3A Terminal")
public class Display implements OutputProvider, Cursor.LineRoller {
    private static final Logger LOGGER = LoggerFactory.getLogger(Display.class);
    private static final String HERE_IS_CONSTANT = Display.class.getAnnotation(PluginContext.class).id();

    // must be synchronized on this object
    public final char[] videoMemory;
    public final int columns;
    public final int rows;

    private final TerminalSettings settings;
    private final Cursor cursor;
    private final LoadCursorPosition loadCursorPosition;

    private FileWriter outputWriter = null;

    public Display(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.loadCursorPosition = new LoadCursorPosition(cursor);
        this.columns = cursor.getColumns();
        this.rows = cursor.getRows();
        this.videoMemory = new char[rows * columns];

        fillWithSpaces();
        if (!settings.isGuiSupported()) {
            openOutputWriter();
        }
    }

    @Override
    public void reset() {
        clearScreen();
    }

    @Override
    public synchronized void close() {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ignored) {
            }
        }
        outputWriter = null;
    }

    public Point getCursorPoint() {
        return cursor.getCursorPoint();
    }

    public void clearScreen() {
        fillWithSpaces();
        cursor.home();
    }

    @Override
    public void rollLine() {
        synchronized (videoMemory) {
            System.arraycopy(videoMemory, columns, videoMemory, 0, columns * rows - columns);
            for (int i = columns * rows - columns; i < (columns * rows); i++) {
                videoMemory[i] = ' ';
            }
        }
    }

    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void write(byte data) {
        writeToOutput(data);

        int d = data & 0xFF;

        /*
         * if it is special char, interpret it. else just add to "video memory"
         */
        switch (d) {
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

        if (loadCursorPosition.notAccepted(data) && d >= 32) {
            drawChar((char) d);
            cursor.moveForwardsRolling(this);
        }
    }

    private void insertHereIs() {
        for (char c : Display.HERE_IS_CONSTANT.toCharArray()) {
            drawChar(c);
            cursor.moveForwardsRolling(this);
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

    private void writeToOutput(byte data) {
        if (outputWriter != null) {
            try {
                outputWriter.write((char) data);
                outputWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write to file: " + settings.getOutputPath(), e);
            }
        }
    }
}
