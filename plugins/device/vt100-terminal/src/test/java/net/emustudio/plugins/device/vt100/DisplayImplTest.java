/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.device.vt100.interaction.Cursor;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_ROWS;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DisplayImplTest {

    private DisplayImpl display;
    private Cursor cursor;

    @Before
    public void setUp() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        PluginSettings pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), eq(false))).andReturn(true).anyTimes();
        expect(pluginSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(pluginSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(pluginSettings);

        TerminalSettings settings = new TerminalSettings(pluginSettings, dialogs);
        this.cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        this.display = new DisplayImpl(cursor, settings);
    }

    // ========== Basic display properties ==========

    @Test
    public void testGetRows() {
        assertEquals(DEFAULT_ROWS, display.getRows());
    }

    @Test
    public void testGetColumns() {
        assertEquals(DEFAULT_COLUMNS, display.getColumns());
    }

    @Test
    public void testGetCursorPointInitiallyAtOrigin() {
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testGetVideoMemoryNotNull() {
        assertNotNull(display.getVideoMemory());
        assertEquals(DEFAULT_COLUMNS * DEFAULT_ROWS, display.getVideoMemory().length);
    }

    @Test
    public void testVideoMemoryInitiallyFilledWithSpaces() {
        char[] mem = display.getVideoMemory();
        for (char c : mem) {
            assertEquals(' ', c);
        }
    }

    // ========== print / write tests ==========

    @Test
    public void testPrintCharacterAppearsInVideoMemory() {
        display.write((byte) 'A');
        assertEquals('A', display.getVideoMemory()[0]);
    }

    @Test
    public void testPrintMultipleCharacters() {
        display.write((byte) 'H');
        display.write((byte) 'i');
        assertEquals('H', display.getVideoMemory()[0]);
        assertEquals('i', display.getVideoMemory()[1]);
    }

    @Test
    public void testCursorAdvancesAfterPrint() {
        display.write((byte) 'X');
        assertEquals(new Point(1, 0), display.getCursorPoint());
    }

    // ========== Control character tests ==========

    @Test
    public void testCarriageReturn() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 0x0D); // CR
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testBackspace() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 0x08); // BS
        assertEquals(new Point(1, 0), display.getCursorPoint());
    }

    @Test
    public void testLineFeed() {
        display.write((byte) 'A');
        // LF moves down and does CR
        display.write((byte) 0x0A);
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testFormFeed() {
        display.write((byte) 'A');
        display.write((byte) 0x0C); // FF
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testVerticalTab() {
        display.write((byte) 'A');
        display.write((byte) 0x0B); // VT
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testHorizontalTab() {
        display.write((byte) 0x09); // HT
        assertEquals(new Point(4, 0), display.getCursorPoint());
    }

    // ========== ESC sequence tests ==========

    @Test
    public void testEscD_Index() {
        // ESC D = Index - move cursor down one line
        display.write((byte) 0x1B);
        display.write((byte) 0x44);
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testEscM_ReverseIndex() {
        // Move cursor down first, then ESC M = Reverse Index
        display.write((byte) 0x1B);
        display.write((byte) 0x44); // down to row 1
        display.write((byte) 0x1B);
        display.write((byte) 0x4D); // reverse index back to row 0
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testEscE_NextLine() {
        // ESC E = NEL - move to first position on next line
        display.write((byte) 'X');
        display.write((byte) 0x1B);
        display.write((byte) 0x45);
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testEsc7_SaveCursorAndEsc8_RestoreCursor() {
        // Use CSI to move to row 4, col 6: ESC [ 4 ; 6 H
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '4');
        display.write((byte) ';');
        display.write((byte) '6');
        display.write((byte) 'H');
        Point saved = display.getCursorPoint();

        // ESC 7 = Save cursor
        display.write((byte) 0x1B);
        display.write((byte) '7');

        // Move cursor somewhere else
        display.write((byte) 'Z');
        display.write((byte) 'Z');
        assertNotEquals(saved, display.getCursorPoint());

        // ESC 8 = Restore cursor
        display.write((byte) 0x1B);
        display.write((byte) '8');
        assertEquals(saved, display.getCursorPoint());
    }

    // ========== CSI sequence tests ==========

    @Test
    public void testCsiCursorUp() {
        // Move down first
        display.write((byte) 0x0A); // LF
        display.write((byte) 0x0A); // LF
        // CSI 1 A = Cursor Up 1
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '1');
        display.write((byte) 'A');
        assertEquals(1, display.getCursorPoint().y);
    }

    @Test
    public void testCsiCursorDown() {
        // CSI 3 B = Cursor Down 3
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '3');
        display.write((byte) 'B');
        assertEquals(3, display.getCursorPoint().y);
    }

    @Test
    public void testCsiCursorForward() {
        // CSI 5 C = Cursor Forward 5
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '5');
        display.write((byte) 'C');
        assertEquals(5, display.getCursorPoint().x);
    }

    @Test
    public void testCsiCursorBackward() {
        // Move forward first
        for (int i = 0; i < 10; i++) display.write((byte) 'A');
        // CSI 3 D = Cursor Backward 3
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '3');
        display.write((byte) 'D');
        assertEquals(7, display.getCursorPoint().x);
    }

    @Test
    public void testCsiCursorPositionWithTwoParams() {
        // CSI 5 ; 10 H = Move cursor to row 5, column 10
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '5');
        display.write((byte) ';');
        display.write((byte) '1');
        display.write((byte) '0');
        display.write((byte) 'H');
        // Parameters are: cursor.move(params[1]=10, params[0]=5)
        assertEquals(new Point(10, 5), display.getCursorPoint());
    }

    @Test
    public void testCsiCursorHomeNoParams() {
        // Move somewhere first
        display.write((byte) 'X');
        display.write((byte) 0x0A);
        // CSI H = Cursor Home (0, 0)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'H');
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testCsiHvp() {
        // CSI 3 ; 7 f = Horizontal And Vertical Position
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '3');
        display.write((byte) ';');
        display.write((byte) '7');
        display.write((byte) 'f');
        assertEquals(new Point(7, 3), display.getCursorPoint());
    }

    // ========== Reset test ==========

    @Test
    public void testResetClearsScreenAndHomesCursor() {
        display.write((byte) 'A');
        display.write((byte) 'B');
        display.write((byte) 'C');
        display.reset();
        assertEquals(new Point(0, 0), display.getCursorPoint());
        // Video memory should be cleared
        for (char c : display.getVideoMemory()) {
            assertEquals(' ', c);
        }
    }

    // ========== Roll up / Roll down tests ==========

    @Test
    public void testRollUp() {
        // Write characters on first two lines
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            display.getVideoMemory()[i] = 'A';
        }
        for (int i = DEFAULT_COLUMNS; i < 2 * DEFAULT_COLUMNS; i++) {
            display.getVideoMemory()[i] = 'B';
        }

        display.rollUp();

        // First line should now be 'B's
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            assertEquals('B', display.getVideoMemory()[i]);
        }
        // Last line should be spaces
        int lastLineStart = DEFAULT_COLUMNS * (DEFAULT_ROWS - 1);
        for (int i = lastLineStart; i < lastLineStart + DEFAULT_COLUMNS; i++) {
            assertEquals(' ', display.getVideoMemory()[i]);
        }
    }

    @Test
    public void testRollDown() {
        // Write characters on first line
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            display.getVideoMemory()[i] = 'X';
        }

        display.rollDown();

        // First line should be spaces
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            assertEquals(' ', display.getVideoMemory()[i]);
        }
        // Second line should now be 'X's
        for (int i = DEFAULT_COLUMNS; i < 2 * DEFAULT_COLUMNS; i++) {
            assertEquals('X', display.getVideoMemory()[i]);
        }
    }

    // ========== Line wrapping / auto-scroll ==========

    @Test
    public void testLineWrappingAfterLastColumn() {
        // Write DEFAULT_COLUMNS characters to fill line 0
        for (int i = 0; i < DEFAULT_COLUMNS; i++) {
            display.write((byte) 'A');
        }
        // Next character should be on line 1, column 0
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testAutoScrollAtBottomOfScreen() {
        // Fill the entire screen to force scrolling
        for (int i = 0; i < DEFAULT_COLUMNS * DEFAULT_ROWS; i++) {
            display.write((byte) 'Z');
        }
        // Cursor should be at the start of last line after rolling
        assertEquals(DEFAULT_ROWS - 1, display.getCursorPoint().y);
        assertEquals(0, display.getCursorPoint().x);
    }

    // ========== 8-bit C1 control tests ==========
    // Note: 0x80-0x8F are routed through the state machine's print() dispatch, not execute().
    // The 7-bit ESC equivalents (ESC D, ESC E, ESC M) use escDispatch and are tested above.

    @Test
    public void testC1_0x84_IsPrintedAsCharacter() {
        // 0x84 in 0x80-0x8F range goes through print(), printed as character
        display.write((byte) 0x84);
        assertEquals((char) 0x84, display.getVideoMemory()[0]);
    }

    @Test
    public void testC1_0x85_IsPrintedAsCharacter() {
        display.write((byte) 0x85);
        assertEquals((char) 0x85, display.getVideoMemory()[0]);
    }

    @Test
    public void testC1_0x8D_IsPrintedAsCharacter() {
        display.write((byte) 0x8D);
        assertEquals((char) 0x8D, display.getVideoMemory()[0]);
    }

    // ========== Cancel / substitute during escape ==========

    @Test
    public void testCancelDuringEscSequence() {
        // Start ESC sequence, then cancel
        display.write((byte) 0x1B); // ESC
        display.write((byte) 0x18); // CAN
        // Should be back in ground, next char is printable
        display.write((byte) 'Q');
        assertEquals('Q', display.getVideoMemory()[0]);
    }

    @Test
    public void testSubstituteDuringEscPrintsReversedQuestionMark() {
        display.write((byte) 0x1B); // ESC
        display.write((byte) 0x1A); // SUB
        // SUB should print reversed question mark "¿"
        // Verify we're back in ground after that
        display.write((byte) 'R');
        // 'R' should be printed after ¿ characters
        assertTrue(display.getCursorPoint().x > 0);
    }

    // ========== setSize test ==========

    @Test
    public void testSetSizeChangesVideoMemory() {
        display.setSize(40, 12);
        assertEquals(40, display.getColumns());
        assertEquals(12, display.getRows());
        assertEquals(40 * 12, display.getVideoMemory().length);
    }

    // ========== Close test ==========

    @Test
    public void testCloseDoesNotThrow() {
        display.close();
    }

    // ========== Enquiry test ==========

    @Test
    public void testEnquiryWritesAnswerback() {
        display.write((byte) 5); // Enquiry
        // "answerback" is 10 chars, should be in video memory
        char[] mem = display.getVideoMemory();
        assertEquals('a', mem[0]);
        assertEquals('n', mem[1]);
        assertEquals('s', mem[2]);
        assertEquals('w', mem[3]);
        assertEquals('e', mem[4]);
        assertEquals('r', mem[5]);
        assertEquals('b', mem[6]);
        assertEquals('a', mem[7]);
        assertEquals('c', mem[8]);
        assertEquals('k', mem[9]);
    }

    // ========== CSI with single param ==========

    @Test
    public void testCsiCursorPositionWithSingleParam() {
        // CSI 5 H = Move cursor to row 5, column 0
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '5');
        display.write((byte) 'H');
        assertEquals(0, display.getCursorPoint().x);
        assertEquals(5, display.getCursorPoint().y);
    }

    // ========== Overwrite character ==========

    @Test
    public void testOverwriteCharacterAtSamePosition() {
        display.write((byte) 'A');
        // Move back
        display.write((byte) 0x08); // BS
        display.write((byte) 'B');
        assertEquals('B', display.getVideoMemory()[0]);
    }
}

