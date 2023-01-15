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
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.plugins.device.vt100.TerminalSettings;
import net.emustudio.plugins.device.vt100.Vt100StateMachine;
import net.emustudio.plugins.device.vt100.api.OutputProvider;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


// https://vt100.net/docs/vt220-rm/chapter4.html
@ThreadSafe
public class Display implements OutputProvider, net.emustudio.plugins.device.vt100.interaction.Cursor.LineRoller, Vt100StateMachine.Vt100Dispatcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(Display.class);

    public char[] videoMemory;
    public volatile int columns;
    public volatile int rows;

    private final TerminalSettings settings;
    private final Cursor cursor;
    private final Vt100StateMachine vt100;

    private FileWriter outputWriter = null;
    private Point savedCursorPosition = new Point();

    public Display(Cursor cursor, TerminalSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.cursor = Objects.requireNonNull(cursor);
        this.columns = cursor.columns;
        this.rows = cursor.rows;
        this.videoMemory = new char[rows * columns];

        fillWithSpaces();
        this.vt100 = new Vt100StateMachine(this);

        if (!settings.isGuiSupported()) {
            openOutputWriter();
        }
    }

    @Override
    public void reset() {
        clearScreen();
    }

    public synchronized void setSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.cursor.setSize(columns, rows);
        this.videoMemory = new char[rows * columns];
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
    public synchronized void rollUp() {
        System.arraycopy(videoMemory, columns, videoMemory, 0, columns * rows - columns);
        for (int i = columns * rows - columns; i < (columns * rows); i++) {
            videoMemory[i] = ' ';
        }
    }

    @Override
    public synchronized void rollDown() {
        System.arraycopy(videoMemory, 0, videoMemory, columns, columns * rows - columns);
        for (int i = 0; i < columns; i++) {
            videoMemory[i] = ' ';
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
                // Moves cursor to next tab stop, or to right margin if there are no more tab stops. Does not cause autowrap.
                cursor.moveForwards(4);
                break;
            case 0x0A: // Line feed, 0/10
            case 0x0B: // Vertical tabulation, 0/11
            case 0x0C: // Form feed, 0/12
                // Causes a linefeed or a new line operation, depending on the setting of new line mode.
                cursor.moveDown();
                cursor.carriageReturn(); // simulate CR/LF
                break;
            case 0x0D: // Carriage return, 0/13
                // 	Moves cursor to left margin on current line.
                cursor.carriageReturn();
                break;
            case 0x0E: // Shift out (Lock shift G1), 0/14
                // Invokes G1 character set into GL. G1 is designated by a select-character-set (SCS) sequence.
            case 0x0F: // Shift in (Lock shift G0), 0/15
                // Invoke G0 character set into GL. G0 is designated by a select-character-set sequence (SCS).
            case 0x11: // Device Control 1
                // Also referred to as XON. If XOFF support is enabled, DC1 clears DC3 (XOFF), causing the terminal
                // to continue transmitting characters (keyboard unlocks) unless KAM mode is currently set.
            case 0x13: // Device Control 3
                // Also referred to as XOFF. If XOFF support is enabled, DC3 causes the terminal to stop transmitting
                // characters until a DC1 control character is received.
                break;
            case 0x18: // Cancel
            case 0x1B: // Escape
                // Processed as escape sequence introducer. Terminates any escape, control or device control sequence
                // which is in progress.
                // If received during an escape or control sequence, terminates and cancels the sequence. No error
                // character is displayed. If received during a device control string, the DCS is terminated and no
                // error character is displayed.
                vt100.cancel();
                break;
            case 0x1A: // Substitute
                // If received during escape or control sequence, terminates and cancels the sequence. Causes a reverse
                // question mark to be displayed. If received during a device control sequence, the DCS is terminated
                // and reverse question mark is displayed.
                vt100.cancel();
                write("¿");
                break;
            case 0x1F: // US 1/15
            case 0x7F: // DEL 7/15
                break;
            case 0x84: // Index
                // 	Moves cursor down one line in same column. If cursor is at bottom margin, screen performs a scroll up.
                // https://vt100.net/docs/vt220-rm/chapter4.html
                cursorDown();
                break;
            case 0x85: // Next line
                // 	Moves cursor to first position on next line. If cursor is at bottom margin, screen performs a scroll up.
                cursorDown();
                cursor.carriageReturn();
                break;
            case 0x88: // Horizontal tab set
                // 	Sets one horizontal tab stop at the column where the cursor is.
                print('\t');
                break;
            case 0x8D: // Reverse index
                // 	Moves cursor up one line in same column. If cursor is at top margin, screen performs a scroll down.
                cursor.moveUpRolling(this);
                break;


        }

        // data >= 0 && data <= 0x17 || data == 0x19 || data >= 0x1C && data <= 0x1F
    }

    @Override
    public void print(int data) {
        Point point = cursor.getCursorPoint();
        synchronized (this) {
            videoMemory[point.y * columns + point.x] = (char) data;
        }
        cursor.moveForwards();
    }

    @Override
    public void escDispatch(int data, List<Integer> collected) {
        //data >= 0x30 && data <= 0x4F || data >= 0x51 && data <= 0x57 || data >= 0x60 && data <= 0x7E ||
        //                    data == 0x59 || data == 0x5A || data == 0x5C
        // data >= 0x30 && data <= 0x7E

        switch (data) {
            case 0x44: // Index (IND)
                // IND is an 8-bit control character (8/4). It can be expressed as an escape sequence for a
                // 7-bit environment. IND moves the cursor down one line in the same column. If the cursor is at the
                // bottom margin, the screen performs a scroll-up.
                cursor.moveDownRolling(this);
                break;
            case 0x4D: // Reverse Index (RI)
                // RI is an 8-bit control character (8/13). It can be expressed as an escape sequence for a
                // 7-bit environment. RI moves the cursor up one line in the same column. If the cursor is at the top
                // margin, the screen performs a scroll-down.
                cursor.moveUpRolling(this);
                break;
            case 0x45: // Next Line (NEL)
                // NEL is an 8-bit control character (8/5). It can be expressed as an escape sequence for a
                // 7-bit environment. NEL moves the cursor to the first position on the next line. If the cursor is at
                // the bottom margin, the screen performs a scroll-up.
                cursor.moveDownRolling(this);
                cursor.carriageReturn();
                break;
            case 0x37: // Save Cursor (DECSC)
                // Saves the following in terminal memory.
                // - cursor position
                // - graphic rendition
                // - character set shift state
                // - state of wrap flag
                // - state of origin mode
                // - state of selective erase
                savedCursorPosition = cursor.getCursorPoint();
                break;
            case 0x38: // Restore Cursor (DECRC)
                // Restores the states described for (DECSC) above. If none of these characteristics were saved, the
                // cursor moves to home position; origin mode is reset; no character attributes are assigned; and the
                // default character set mapping is established.
                cursor.set(savedCursorPosition.x, savedCursorPosition.y);
                break;
        }
    }

    @Override
    public void csiDispatch(int data, List<Integer> collected, List<Integer> params) {
        // data >= 0x40 && data <= 0x7E
        switch (data) {
            case 0x41: // Cursor Up
                // Moves the cursor up Pn lines in the same column. The cursor stops at the top margin.
                cursor.moveUp(params.get(0));
                break;
            case 0x42: // Cursor Down
                // Moves the cursor down Pn lines in the same column. The cursor stops at the bottom margin.
                cursor.moveDown(params.get(0));
                break;
            case 0x43: // Cursor Forward
                // Moves the cursor right Pn columns. The cursor stops at the right margin.
                cursor.moveForwards(params.get(0));
                break;
            case 0x44: // Cursor Backward
                // Moves the cursor left Pn columns. The cursor stops at the left margin.
                cursor.moveBackwards(params.get(0));
                break;
            case 0x48: // Cursor Position
            case 0x66: // Horizontal And Vertical Position
                // Moves the cursor to line Pl, column Pc. The numbering of the lines and columns depends on the state
                // (set/reset) of origin mode (DECOM). Digital recommends using CUP instead of HVP.
                if (params.size() == 0) {
                    cursor.set(0, 0);
                } else if (params.size() == 1) {
                    cursor.set(0, params.get(0));
                } else {
                    cursor.set(params.get(1), params.get(0));
                }
                break;
            case 0x4C: // Insert Line (IL)
                // Inserts Pn lines at the cursor. If fewer than Pn lines remain from the current line to the end of
                // the scrolling region, the number of lines inserted is the lesser number. Lines within the scrolling
                // region at and below the cursor move down. Lines moved past the bottom margin are lost. The cursor is
                // reset to the first column. This sequence is ignored when the cursor is outside the scrolling region.
                // TODO
                break;
            case 0x4D: // Delete Line (DL)
                // Deletes Pn lines starting at the line with the cursor. If fewer than Pn lines remain from the current
                // line to the end of the scrolling region, the number of lines deleted is the lesser number. As lines
                // are deleted, lines within the scrolling region and below the cursor move up, and blank lines are
                // added at the bottom of the scrolling region. The cursor is reset to the first column. This sequence
                // is ignored when the cursor is outside the scrolling region.
                // TODO
                break;
            case 0x40: // Insert Characters (ICH) (VT200 mode only)
                // Insert Pn blank characters at the cursor position, with the character attributes set to normal.
                // The cursor does not move and remains at the beginning of the inserted blank characters. A parameter
                // of 0 or 1 inserts one blank character. Data on the line is shifted forward as in character insertion.
                // TODO
                break;
            case 0x50: // Delete Character (DCH)
                // Deletes Pn characters starting with the character at the cursor position. When a character is deleted,
                // all characters to the right of the cursor move to the left. This creates a space character at the
                // right margin for each character deleted. Character attributes move with the characters. The spaces
                // created at the end of the line have all their character attributes off.
                // TODO
                break;
            case 0x58: // Erase Character (ECH) (VT200 mode only)
                // Erases characters at the cursor position and the next Pn-1 characters. A parameter of 0 or 1 erases
                // a single character. Character attributes are set to normal. No reformatting of data on the line
                // occurs. The cursor remains in the same position.
                // TODO
                break;
            case 0x4B: // Erase in Line (EL)
                // 9/11 4/11: Erases from the cursor to the end of the line, including the cursor position. Line
                // attribute is not affected.
                // 9/11 3/0  4/11: Same as above.
                // 9/11 3/1  4/11: Erases from the beginning of the line to the cursor, including the cursor position.
                // Line attribute is not affected.
                // 9/11 3/2  4/11: Erases the complete line.

                //Selective Erase In Line (DECSEL) (VT200 move only)
                // 9/11 3/15 4/11: Erases all erasable characters (DECSCA) from the cursor to the end of the line. Does
                // not affect video line attributes or video character attributes (SGR).
                // 9/11 3/15 3/0 4/11: Same as above.
                // 9/11 3/15 3/1 4/11: Erases all erasable characters (DECSCA) from the beginning of the line to and
                // including the cursor position. Does not affect video line attributes or video character attributes.
                // 9/11 3/15 3/2 4/11: Erases all erasable characters (DECSCA) on the line. Does not affect video line
                // attributes or video character attributes.

                // TODO
                break;
            case 0x4A: // Erase in Display (ED)
                // 9/11 4/10: Erases from the cursor to the end of the screen, including the cursor position. Line
                // attribute becomes single-height, single-width for all completely erased lines.
                // 9/11 3/0  4/10: Same as above.
                // 9/11 3/1  4/10: Erases from the beginning of the screen to the cursor, including the cursor position.
                // Line attribute becomes single-height, single-width for all completely erased lines.
                // 9/11 3/2  4/10: Erases the complete display. All lines are erased and changed to single-width.
                // The cursor does not move.

                // Selective Erase In Display (DECSED) (VT200 mode only)
                // 9/11 3/15 4/10: Erases all erasable characters (DECSCA) from and including the cursor to the end of
                // the screen. Does not affect video line attributes or video character attributes (SGR).
                // 9/11 3/15 3/0 4/10: Same as above.
                // 9/11 3/15 3/1 4/10: Erases all erasable characters (DECSCA) from the beginning of the screen to and
                // including the cursor. Does not affect video line attributes or video character attributes (SGR).
                // 9/11 3/15 3/2 4/10: Erases all erasable characters (DECSCA) in the entire display. Does not affect
                // video character attributes or video line attributes (SGR).
                // TODO
                break;
            case 0x72: // Set Top and Bottom Margins (DECSTBM)
                // This sequence selects top and bottom margins defining the scrolling region.
                // CSI  Pt  ;   Pb   r
                // TODO
                break;

        }
    }

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


    private void write(String string) {
        string.chars().forEach(vt100::accept);
    }

    private void cursorDown() {
        if (cursor.getCursorPoint().y == columns - 1) {
            rollUp();
        } else {
            cursor.moveDown();
        }
    }

    private synchronized void fillWithSpaces() {
        Arrays.fill(videoMemory, ' ');
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
                LOGGER.error("Could not write to file: {}", settings.getOutputPath(), e);
            }
        }
    }
}
