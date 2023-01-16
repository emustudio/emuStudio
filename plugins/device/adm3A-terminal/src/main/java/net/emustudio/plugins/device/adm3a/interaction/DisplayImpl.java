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
import net.emustudio.plugins.device.adm3a.api.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Display implementation.
 * A display is not a canvas! It's a visual "controller" of the terminal, but does not perform rendering.
 * <p>
 * It can interpret ASCII codes from 0-127. Some have special purpose (0-31).
 */
@PluginContext(id = "LSI ADM-3A Terminal")
public class DisplayImpl implements Display, Cursor.LineRoller {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayImpl.class);
    private static final String HERE_IS_CONSTANT = DisplayImpl.class.getAnnotation(PluginContext.class).id();

    // must be synchronized on this object
    private final char[] videoMemory;

    private final TerminalSettings settings;
    private final Cursor cursor;
    private final LoadCursorPosition loadCursorPosition;
    private final int lastRowStartIndex;
    private final int lastRowEndIndex;


    private FileWriter outputWriter = null;

    public DisplayImpl(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.loadCursorPosition = new LoadCursorPosition(cursor);
        this.videoMemory = new char[cursor.rows * cursor.columns];
        this.lastRowStartIndex = cursor.columns * cursor.rows - cursor.columns;
        this.lastRowEndIndex = cursor.columns * cursor.rows;

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

    @Override
    public Point getCursorPoint() {
        return cursor.getCursorPoint();
    }

    @Override
    public int getRows() {
        return cursor.rows;
    }

    @Override
    public int getColumns() {
        return cursor.columns;
    }

    @Override
    public char[] getVideoMemory() {
        return videoMemory; // I should be punished for this
    }

    public void clearScreen() {
        fillWithSpaces();
        cursor.home();
    }

    @Override
    public void rollLine() {
        synchronized (videoMemory) {
            System.arraycopy(videoMemory, cursor.columns, videoMemory, 0, lastRowStartIndex);
            for (int i = lastRowStartIndex; i < lastRowEndIndex; i++) {
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
        for (char c : DisplayImpl.HERE_IS_CONSTANT.toCharArray()) {
            drawChar(c);
            cursor.moveForwardsRolling(this);
        }
    }

    private void drawChar(char c) {
        Point cursorPoint = cursor.getCursorPoint();
        synchronized (videoMemory) {
            videoMemory[cursorPoint.y * cursor.columns + cursorPoint.x] = c;
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
