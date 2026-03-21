/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.plugins.device.vt100.TerminalSettings;
import net.emustudio.plugins.device.vt100.VideoAttribute;
import net.emustudio.plugins.device.vt100.Vt100StateMachine;
import net.emustudio.plugins.device.vt100.api.Display;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


// https://vt100.net/docs/vt220-rm/chapter4.html
@ThreadSafe
public class DisplayImpl implements Display, Vt100StateMachine.Vt100Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(DisplayImpl.class);

    public char[] videoMemory;
    public int[] attributeMemory;

    private final TerminalSettings settings;
    private final Cursor cursor;
    private final Vt100StateMachine vt100;

    private final FileWriter outputWriter;
    private final AtomicReference<Point> savedCursorPosition = new AtomicReference<>(new Point());
    private volatile int currentAttribute = VideoAttribute.DEFAULT;
    private int savedAttribute = VideoAttribute.DEFAULT;

    public DisplayImpl(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        Rectangle rect = cursor.getRect();
        this.videoMemory = new char[rect.height * rect.width];
        this.attributeMemory = new int[rect.height * rect.width];

        fillWithSpaces();
        this.vt100 = new Vt100StateMachine(this);

        if (!settings.isGuiSupported()) {
            FileWriter fw = null;
            try {
                fw = new FileWriter(settings.getOutputPath().toFile());
            } catch (IOException e) {
                LOGGER.error("Could not open file for writing output: {}", settings.getOutputPath(), e);
            }
            this.outputWriter = fw;
        } else {
            this.outputWriter = null;
        }
    }

    @Override
    public void reset() {
        currentAttribute = VideoAttribute.DEFAULT;
        clearScreen();
    }

    public synchronized void setSize(int columns, int rows) {
        this.cursor.setSize(columns, rows);
        this.videoMemory = new char[rows * columns];
        this.attributeMemory = new int[rows * columns];
    }

    @Override
    public void close() {
        FileWriter fw = outputWriter;
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                LOGGER.error("Could not close output file", e);
            }
        }
    }

    @Override
    public Point getCursorPoint() {
        return cursor.getRect().getLocation();
    }

    @Override
    public int getRows() {
        return cursor.getRect().height;
    }

    @Override
    public int getColumns() {
        return cursor.getRect().width;
    }

    @Override
    public char[] getVideoMemory() {
        return videoMemory;
    }

    @Override
    public int[] getAttributeMemory() {
        return attributeMemory;
    }

    public void clearScreen() {
        fillWithSpaces();
        cursor.home();
    }

    @Override
    public void rollUp() {
        Rectangle rect = cursor.getRect();
        int scrollTop = cursor.getScrollTop();
        int scrollBottom = cursor.getScrollBottom();
        synchronized (this) {
            int lineSize = rect.width;
            int regionStart = scrollTop * lineSize;
            int regionEnd = (scrollBottom + 1) * lineSize;
            System.arraycopy(videoMemory, regionStart + lineSize, videoMemory, regionStart, regionEnd - regionStart - lineSize);
            System.arraycopy(attributeMemory, regionStart + lineSize, attributeMemory, regionStart, regionEnd - regionStart - lineSize);
            for (int i = regionEnd - lineSize; i < regionEnd; i++) {
                videoMemory[i] = ' ';
                attributeMemory[i] = VideoAttribute.DEFAULT;
            }
        }
    }

    @Override
    public void rollDown() {
        Rectangle rect = cursor.getRect();
        int scrollTop = cursor.getScrollTop();
        int scrollBottom = cursor.getScrollBottom();
        synchronized (this) {
            int lineSize = rect.width;
            int regionStart = scrollTop * lineSize;
            int regionEnd = (scrollBottom + 1) * lineSize;
            System.arraycopy(videoMemory, regionStart, videoMemory, regionStart + lineSize, regionEnd - regionStart - lineSize);
            System.arraycopy(attributeMemory, regionStart, attributeMemory, regionStart + lineSize, regionEnd - regionStart - lineSize);
            for (int i = regionStart; i < regionStart + lineSize; i++) {
                videoMemory[i] = ' ';
                attributeMemory[i] = VideoAttribute.DEFAULT;
            }
        }
    }

    @Override
    public void write(byte data) {
        try {
            writeToOutput(data);
            vt100.accept(data & 0xFF);
        } catch (Exception e) {
            LOGGER.error("Could not write data to display", e);
        }
    }

    @Override
    public void execute(int data) {
        switch (data) {
            case 5: // Enquiry, 0/5
                write("answerback");
                break;
            case 7: // Bell, 0/7
                break;
            case 8: // Backspace, 0/8
                cursor.moveBackwards();
                break;
            case 9: // Horizontal tabulation, 0/9
                cursor.moveForwards(4);
                break;
            case 0x0A: // Line feed, 0/10
            case 0x0B: // Vertical tabulation, 0/11
            case 0x0C: // Form feed, 0/12
                cursor.moveDownRolling(this);
                cursor.carriageReturn(); // simulate CR/LF
                break;
            case 0x0D: // Carriage return, 0/13
                cursor.carriageReturn();
                break;
            case 0x0E: // Shift out (Lock shift G1), 0/14
            case 0x0F: // Shift in (Lock shift G0), 0/15
            case 0x11: // Device Control 1 (XON)
            case 0x13: // Device Control 3 (XOFF)
                break;
            case 0x18: // Cancel
            case 0x1B: // Escape
                vt100.cancel();
                break;
            case 0x1A: // Substitute
                vt100.cancel();
                write("¿");
                break;
            case 0x1F: // US 1/15
            case 0x7F: // DEL 7/15
                break;
            // C1 8-bit control codes
            case 0x84: // Index (IND)
                cursor.moveDownRolling(this);
                break;
            case 0x85: // Next line (NEL)
                cursor.moveDownRolling(this);
                cursor.carriageReturn();
                break;
            case 0x88: // Horizontal tab set (HTS)
                print('\t');
                break;
            case 0x8D: // Reverse index (RI)
                cursor.moveUpRolling(this);
                break;
        }
    }

    @Override
    public void print(int data) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int offset = rect.y * rect.width + rect.x;
            videoMemory[offset] = (char) data;
            attributeMemory[offset] = currentAttribute;
        }
        cursor.moveForwardsRolling(this);
    }

    @Override
    public void escDispatch(int data, List<Integer> collected) {
        switch (data) {
            case 0x44: // Index (IND)
                cursor.moveDownRolling(this);
                break;
            case 0x4D: // Reverse Index (RI)
                cursor.moveUpRolling(this);
                break;
            case 0x45: // Next Line (NEL)
                cursor.moveDownRolling(this);
                cursor.carriageReturn();
                break;
            case 0x37: // Save Cursor (DECSC) - also saves graphic rendition
                savedCursorPosition.set(cursor.getRect().getLocation());
                savedAttribute = currentAttribute;
                break;
            case 0x38: // Restore Cursor (DECRC)
                Point point = savedCursorPosition.get();
                cursor.move(point);
                currentAttribute = savedAttribute;
                break;
        }
    }

    @Override
    public void csiDispatch(int data, List<Integer> collected, List<Integer> params) {
        switch (data) {
            case 0x41: // Cursor Up (CUU)
                cursor.moveUp(param(params, 0, 1));
                break;
            case 0x42: // Cursor Down (CUD)
                cursor.moveDown(param(params, 0, 1));
                break;
            case 0x43: // Cursor Forward (CUF)
                cursor.moveForwards(param(params, 0, 1));
                break;
            case 0x44: // Cursor Backward (CUB)
                cursor.moveBackwards(param(params, 0, 1));
                break;
            case 0x48: // Cursor Position (CUP)
            case 0x66: // Horizontal And Vertical Position (HVP)
                if (params.isEmpty()) {
                    cursor.move(0, 0);
                } else if (params.size() == 1) {
                    cursor.move(0, params.get(0));
                } else {
                    cursor.move(params.get(1), params.get(0));
                }
                break;
            case 0x4A: // Erase in Display (ED)
                eraseInDisplay(param(params, 0, 0));
                break;
            case 0x4B: // Erase in Line (EL)
                eraseInLine(param(params, 0, 0));
                break;
            case 0x4C: // Insert Line (IL)
                insertLines(param(params, 0, 1));
                break;
            case 0x4D: // Delete Line (DL)
                deleteLines(param(params, 0, 1));
                break;
            case 0x40: // Insert Characters (ICH)
                insertCharacters(param(params, 0, 1));
                break;
            case 0x50: // Delete Character (DCH)
                deleteCharacters(param(params, 0, 1));
                break;
            case 0x58: // Erase Character (ECH)
                eraseCharacters(param(params, 0, 1));
                break;
            case 0x6D: // Select Graphic Rendition (SGR)
                selectGraphicRendition(params);
                break;
            case 0x72: // Set Top and Bottom Margins (DECSTBM)
                // CSI Pt ; Pb r
                // Pt = top margin (1-based, default 1), Pb = bottom margin (1-based, default last row)
                // Sets the scrolling region; cursor moves to home position.
                int top = param(params, 0, 1);
                int bottom = param(params, 1, getRows());
                cursor.setScrollingRegion(top - 1, bottom - 1);
                break;
        }
    }

    // ========== SGR (Select Graphic Rendition) ==========

    private void selectGraphicRendition(List<Integer> params) {
        if (params.isEmpty()) {
            currentAttribute = VideoAttribute.DEFAULT;
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            int p = params.get(i);
            switch (p) {
                case 0: // Reset
                    currentAttribute = VideoAttribute.DEFAULT;
                    break;
                case 1: // Bold
                    currentAttribute = VideoAttribute.withBold(currentAttribute, true);
                    break;
                case 2: // Dim/Faint
                    currentAttribute = VideoAttribute.withDim(currentAttribute, true);
                    break;
                case 3: // Italic
                    currentAttribute = VideoAttribute.withItalic(currentAttribute, true);
                    break;
                case 4: // Underline
                    currentAttribute = VideoAttribute.withUnderline(currentAttribute, true);
                    break;
                case 5: // Blink (slow)
                case 6: // Blink (rapid) - treated same as slow
                    currentAttribute = VideoAttribute.withBlink(currentAttribute, true);
                    break;
                case 7: // Inverse/Reverse
                    currentAttribute = VideoAttribute.withInverse(currentAttribute, true);
                    break;
                case 8: // Hidden/Invisible
                    currentAttribute = VideoAttribute.withHidden(currentAttribute, true);
                    break;
                case 9: // Strikethrough
                    currentAttribute = VideoAttribute.withStrikethrough(currentAttribute, true);
                    break;
                case 22: // Normal intensity (not bold, not dim)
                    currentAttribute = VideoAttribute.withBold(currentAttribute, false);
                    currentAttribute = VideoAttribute.withDim(currentAttribute, false);
                    break;
                case 23: // Not italic
                    currentAttribute = VideoAttribute.withItalic(currentAttribute, false);
                    break;
                case 24: // Not underlined
                    currentAttribute = VideoAttribute.withUnderline(currentAttribute, false);
                    break;
                case 25: // Not blinking
                    currentAttribute = VideoAttribute.withBlink(currentAttribute, false);
                    break;
                case 27: // Not reversed
                    currentAttribute = VideoAttribute.withInverse(currentAttribute, false);
                    break;
                case 28: // Not hidden
                    currentAttribute = VideoAttribute.withHidden(currentAttribute, false);
                    break;
                case 29: // Not strikethrough
                    currentAttribute = VideoAttribute.withStrikethrough(currentAttribute, false);
                    break;
                case 30: case 31: case 32: case 33:
                case 34: case 35: case 36: case 37: // Standard foreground colors
                    currentAttribute = VideoAttribute.withFg(currentAttribute, p - 30);
                    break;
                case 38: // Extended foreground color
                    i = handleExtendedColor(params, i, true);
                    break;
                case 39: // Default foreground
                    currentAttribute = VideoAttribute.withFg(currentAttribute, VideoAttribute.DEFAULT_FG);
                    break;
                case 40: case 41: case 42: case 43:
                case 44: case 45: case 46: case 47: // Standard background colors
                    currentAttribute = VideoAttribute.withBg(currentAttribute, p - 40);
                    break;
                case 48: // Extended background color
                    i = handleExtendedColor(params, i, false);
                    break;
                case 49: // Default background
                    currentAttribute = VideoAttribute.withBg(currentAttribute, VideoAttribute.DEFAULT_BG);
                    break;
                case 90: case 91: case 92: case 93:
                case 94: case 95: case 96: case 97: // Bright foreground colors
                    currentAttribute = VideoAttribute.withFg(currentAttribute, p - 90 + 8);
                    break;
                case 100: case 101: case 102: case 103:
                case 104: case 105: case 106: case 107: // Bright background colors
                    currentAttribute = VideoAttribute.withBg(currentAttribute, p - 100 + 8);
                    break;
                default:
                    // Unknown SGR parameter, ignore
                    break;
            }
        }
    }

    /**
     * Handle extended color sequences: 38;5;n (256-color) or 38;2;r;g;b (truecolor).
     * Returns the new parameter index after consuming sub-parameters.
     */
    private int handleExtendedColor(List<Integer> params, int i, boolean foreground) {
        if (i + 1 < params.size()) {
            int mode = params.get(i + 1);
            if (mode == 5 && i + 2 < params.size()) {
                // 256-color: 38;5;n or 48;5;n
                int colorIndex = params.get(i + 2);
                if (colorIndex >= 0 && colorIndex <= 15) {
                    if (foreground) {
                        currentAttribute = VideoAttribute.withFg(currentAttribute, colorIndex);
                    } else {
                        currentAttribute = VideoAttribute.withBg(currentAttribute, colorIndex);
                    }
                }
                // Colors 16-255 not supported in 16-color palette, silently ignore
                return i + 2;
            } else if (mode == 2 && i + 4 < params.size()) {
                // Truecolor: 38;2;r;g;b - not supported, skip params
                return i + 4;
            }
        }
        return i;
    }

    // ========== Erase operations ==========

    private void eraseInDisplay(int mode) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int cursorOffset = rect.y * rect.width + rect.x;
            int totalSize = rect.width * rect.height;
            switch (mode) {
                case 0: // Erase from cursor to end of screen
                    eraseRange(cursorOffset, totalSize);
                    break;
                case 1: // Erase from beginning of screen to cursor
                    eraseRange(0, cursorOffset + 1);
                    break;
                case 2: // Erase entire screen
                    eraseRange(0, totalSize);
                    break;
            }
        }
    }

    private void eraseInLine(int mode) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int lineStart = rect.y * rect.width;
            int lineEnd = lineStart + rect.width;
            switch (mode) {
                case 0: // Erase from cursor to end of line
                    eraseRange(lineStart + rect.x, lineEnd);
                    break;
                case 1: // Erase from beginning of line to cursor
                    eraseRange(lineStart, lineStart + rect.x + 1);
                    break;
                case 2: // Erase entire line
                    eraseRange(lineStart, lineEnd);
                    break;
            }
        }
    }

    private void eraseCharacters(int count) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int offset = rect.y * rect.width + rect.x;
            int lineEnd = (rect.y + 1) * rect.width;
            int end = Math.min(offset + count, lineEnd);
            eraseRange(offset, end);
        }
    }

    private void eraseRange(int from, int to) {
        for (int i = from; i < to; i++) {
            videoMemory[i] = ' ';
            attributeMemory[i] = VideoAttribute.DEFAULT;
        }
    }

    // ========== Insert/Delete operations ==========

    private void insertLines(int count) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int lineStart = rect.y * rect.width;
            int totalSize = rect.width * rect.height;
            int shiftSize = totalSize - lineStart - count * rect.width;
            if (shiftSize > 0) {
                System.arraycopy(videoMemory, lineStart, videoMemory, lineStart + count * rect.width, shiftSize);
                System.arraycopy(attributeMemory, lineStart, attributeMemory, lineStart + count * rect.width, shiftSize);
            }
            eraseRange(lineStart, Math.min(lineStart + count * rect.width, totalSize));
        }
        cursor.carriageReturn();
    }

    private void deleteLines(int count) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int lineStart = rect.y * rect.width;
            int totalSize = rect.width * rect.height;
            int srcStart = lineStart + count * rect.width;
            if (srcStart < totalSize) {
                int shiftSize = totalSize - srcStart;
                System.arraycopy(videoMemory, srcStart, videoMemory, lineStart, shiftSize);
                System.arraycopy(attributeMemory, srcStart, attributeMemory, lineStart, shiftSize);
                eraseRange(totalSize - count * rect.width, totalSize);
            } else {
                eraseRange(lineStart, totalSize);
            }
        }
        cursor.carriageReturn();
    }

    private void insertCharacters(int count) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int offset = rect.y * rect.width + rect.x;
            int lineEnd = (rect.y + 1) * rect.width;
            int shiftSize = lineEnd - offset - count;
            if (shiftSize > 0) {
                System.arraycopy(videoMemory, offset, videoMemory, offset + count, shiftSize);
                System.arraycopy(attributeMemory, offset, attributeMemory, offset + count, shiftSize);
            }
            eraseRange(offset, Math.min(offset + count, lineEnd));
        }
    }

    private void deleteCharacters(int count) {
        Rectangle rect = cursor.getRect();
        synchronized (this) {
            int offset = rect.y * rect.width + rect.x;
            int lineEnd = (rect.y + 1) * rect.width;
            int srcStart = offset + count;
            if (srcStart < lineEnd) {
                int shiftSize = lineEnd - srcStart;
                System.arraycopy(videoMemory, srcStart, videoMemory, offset, shiftSize);
                System.arraycopy(attributeMemory, srcStart, attributeMemory, offset, shiftSize);
                eraseRange(lineEnd - count, lineEnd);
            } else {
                eraseRange(offset, lineEnd);
            }
        }
    }

    // ========== DCS / OSC stubs ==========

    @Override
    public Consumer<Integer> hook(int data, List<Integer> collected, List<Integer> params) {
        return null;
    }

    @Override
    public void unhook(int data) {
    }

    @Override
    public Consumer<Integer> oscStart(int data) {
        return null;
    }

    @Override
    public void oscEnd(int data) {
    }

    // ========== Helpers ==========

    /**
     * Get a parameter value with a default if the index is out of bounds.
     */
    private static int param(List<Integer> params, int index, int defaultValue) {
        if (index < params.size()) {
            int value = params.get(index);
            return value == 0 ? defaultValue : value; // 0 is treated as default for most commands
        }
        return defaultValue;
    }

    /**
     * Get current SGR attribute (for testing).
     */
    public int getCurrentAttribute() {
        return currentAttribute;
    }

    private void write(String string) {
        string.chars().forEach(vt100::accept);
    }

    private synchronized void fillWithSpaces() {
        Arrays.fill(videoMemory, ' ');
        Arrays.fill(attributeMemory, VideoAttribute.DEFAULT);
    }


    private void writeToOutput(byte data) {
        FileWriter fw = outputWriter;
        if (fw != null) {
            try {
                fw.write((char) data);
                fw.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write to file: {}", settings.getOutputPath(), e);
            }
        }
    }
}
