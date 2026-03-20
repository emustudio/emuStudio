/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.emustudio.plugins.device.adm3a.TerminalSettingsTestHelper;

import java.awt.*;

import org.junit.Before;
import org.junit.Test;

import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_ROWS;
import static org.junit.Assert.*;

public class DisplayImplTest {
    private DisplayImpl display;
    private Cursor cursor;

    @Before
    public void setUp() {
        cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings();
        display = new DisplayImpl(cursor, settings);
    }

    @Test
    public void testInitialVideoMemoryIsSpaces() {
        char[] memory = display.getVideoMemory();
        for (char c : memory) {
            assertEquals(' ', c);
        }
    }

    @Test
    public void testWritePrintableCharacter() {
        display.write((byte) 'A');
        char[] memory = display.getVideoMemory();
        assertEquals('A', memory[0]);
        // cursor should have moved forward
        assertEquals(new Point(1, 0), display.getCursorPoint());
    }

    @Test
    public void testWriteMultipleCharacters() {
        display.write((byte) 'H');
        display.write((byte) 'i');
        char[] memory = display.getVideoMemory();
        assertEquals('H', memory[0]);
        assertEquals('i', memory[1]);
        assertEquals(new Point(2, 0), display.getCursorPoint());
    }

    @Test
    public void testClearScreen() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.clearScreen();

        char[] memory = display.getVideoMemory();
        for (char c : memory) {
            assertEquals(' ', c);
        }
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testClearScreenControl() {
        display.write((byte) 'A');
        display.write((byte) 0x1A); // clear screen control code

        char[] memory = display.getVideoMemory();
        for (char c : memory) {
            assertEquals(' ', c);
        }
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testBackspace() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 8); // backspace

        assertEquals(new Point(1, 0), display.getCursorPoint());
    }

    @Test
    public void testBackspaceAtBeginning() {
        display.write((byte) 8); // backspace at (0,0)
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testLineFeed() {
        display.write((byte) 0x0A); // line feed
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testCarriageReturn() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 0x0D); // carriage return
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testCursorUp() {
        display.write((byte) 0x0A); // line feed to row 1
        display.write((byte) 0x0B); // cursor up (VT)
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testCursorForward() {
        display.write((byte) 0x0C); // FF = cursor forward
        assertEquals(new Point(1, 0), display.getCursorPoint());
    }

    @Test
    public void testHomeCursor() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 0x1E); // home cursor
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testEscAlsoHomesCursor() {
        display.write((byte) 'A');
        display.write((byte) 0x1B); // ESC - homes cursor (and starts load cursor position)
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testBellDoesNotMoveCursor() {
        display.write((byte) 7); // BELL
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testSODoesNotMoveCursor() {
        display.write((byte) 0x0E); // SO
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testSIDoesNotMoveCursor() {
        display.write((byte) 0x0F); // SI
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testRollLine() {
        // Write on first row
        display.write((byte) 'A');
        cursor.home();

        // Write on second row
        cursor.move(0, 1);
        display.write((byte) 'B');
        cursor.move(0, 1);

        display.rollLine();

        char[] memory = display.getVideoMemory();
        // First row should now have 'B' (from second row)
        assertEquals('B', memory[0]);
        // Last row should be spaces
        for (int i = DEFAULT_COLUMNS * (DEFAULT_ROWS - 1); i < DEFAULT_COLUMNS * DEFAULT_ROWS; i++) {
            assertEquals(' ', memory[i]);
        }
    }

    @Test
    public void testReset() {
        display.write((byte) 'A');
        display.reset();

        char[] memory = display.getVideoMemory();
        for (char c : memory) {
            assertEquals(' ', c);
        }
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testGetRowsAndColumns() {
        assertEquals(DEFAULT_ROWS, display.getRows());
        assertEquals(DEFAULT_COLUMNS, display.getColumns());
    }

    @Test
    public void testHereIs() {
        // Control code 5 should insert "LSI ADM-3A Terminal" string
        display.write((byte) 5);

        char[] memory = display.getVideoMemory();
        String hereIs = "LSI ADM-3A Terminal";
        for (int i = 0; i < hereIs.length(); i++) {
            assertEquals(hereIs.charAt(i), memory[i]);
        }
    }

    @Test
    public void testEscEqualsMoveCursor() {
        // ESC = Y X sequence
        display.write((byte) 0x1B); // ESC
        display.write((byte) '=');
        display.write((byte) (3 + 32)); // Y = 3
        display.write((byte) (5 + 32)); // X = 5

        assertEquals(new Point(5, 3), display.getCursorPoint());
    }

    @Test
    public void testAutoLineRollingAtEndOfDisplay() {
        // Move cursor to last column of last row
        cursor.move(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1);
        display.write((byte) 'Z');

        // After writing at the last position, cursor should roll
        // cursor should be on the last row
        Point p = display.getCursorPoint();
        assertEquals(DEFAULT_ROWS - 1, p.y);
    }

    @Test
    public void testWriteWrapsToNextLine() {
        // Fill the first row
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            display.write((byte) 'X');
        }
        // Cursor should be on the next line
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testCloseDoesNotThrow() {
        display.close();
    }
}
