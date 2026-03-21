/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.device.vt100.interaction.Cursor;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.io.IOException;

import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_ROWS;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DisplayImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DisplayImpl display;
    private Cursor cursor;

    @Before
    public void setUp() throws IOException {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        PluginSettings pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), eq(false))).andReturn(true).anyTimes();
        expect(pluginSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(pluginSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(pluginSettings);

        TerminalSettings settings = new TerminalSettings(pluginSettings, dialogs);
        settings.setOutputPath(temporaryFolder.newFile(TerminalSettings.DEFAULT_OUTPUT_FILE_NAME).toPath());
        this.cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        this.display = new DisplayImpl(cursor, settings);
    }

    @After
    public void tearDown() {
        if (display != null) {
            display.close();
        }
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
    // C1 controls (0x80-0x8F) are now correctly dispatched via execute() by the state machine

    @Test
    public void testC1_0x84_Index() {
        // 0x84 = Index - moves cursor down one line (executed as C1 control)
        display.write((byte) 0x84);
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testC1_0x85_NextLine() {
        display.write((byte) 'X');
        // 0x85 = NEL - moves to first position on next line
        display.write((byte) 0x85);
        assertEquals(new Point(0, 1), display.getCursorPoint());
    }

    @Test
    public void testC1_0x8D_ReverseIndex() {
        // First move down
        display.write((byte) 0x84);
        // Then reverse index
        display.write((byte) 0x8D);
        assertEquals(new Point(0, 0), display.getCursorPoint());
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

    // ========== Attribute memory ==========

    @Test
    public void testAttributeMemoryNotNull() {
        assertNotNull(display.getAttributeMemory());
        assertEquals(DEFAULT_COLUMNS * DEFAULT_ROWS, display.getAttributeMemory().length);
    }

    @Test
    public void testAttributeMemoryInitiallyDefault() {
        for (int attr : display.getAttributeMemory()) {
            assertEquals(VideoAttribute.DEFAULT, attr);
        }
    }

    @Test
    public void testPrintStoresCurrentAttribute() {
        display.write((byte) 'A');
        assertEquals(VideoAttribute.DEFAULT, display.getAttributeMemory()[0]);
    }

    // ========== SGR (Select Graphic Rendition) ==========

    private void writeSgr(int... params) {
        display.write((byte) 0x1B);
        display.write((byte) 0x5B); // CSI
        for (int i = 0; i < params.length; i++) {
            if (i > 0) display.write((byte) ';');
            String s = String.valueOf(params[i]);
            for (char c : s.toCharArray()) display.write((byte) c);
        }
        display.write((byte) 'm'); // SGR final char
    }

    @Test
    public void testSgrReset() {
        writeSgr(1); // bold
        writeSgr(0); // reset
        assertEquals(VideoAttribute.DEFAULT, display.getCurrentAttribute());
    }

    @Test
    public void testSgrBold() {
        writeSgr(1);
        assertTrue(VideoAttribute.isBold(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrDim() {
        writeSgr(2);
        assertTrue(VideoAttribute.isDim(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrItalic() {
        writeSgr(3);
        assertTrue(VideoAttribute.isItalic(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrUnderline() {
        writeSgr(4);
        assertTrue(VideoAttribute.isUnderline(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrBlink() {
        writeSgr(5);
        assertTrue(VideoAttribute.isBlink(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrInverse() {
        writeSgr(7);
        assertTrue(VideoAttribute.isInverse(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrHidden() {
        writeSgr(8);
        assertTrue(VideoAttribute.isHidden(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrStrikethrough() {
        writeSgr(9);
        assertTrue(VideoAttribute.isStrikethrough(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNormalIntensity() {
        writeSgr(1); // bold
        writeSgr(22); // normal intensity
        assertFalse(VideoAttribute.isBold(display.getCurrentAttribute()));
        assertFalse(VideoAttribute.isDim(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNotItalic() {
        writeSgr(3);
        writeSgr(23);
        assertFalse(VideoAttribute.isItalic(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNotUnderlined() {
        writeSgr(4);
        writeSgr(24);
        assertFalse(VideoAttribute.isUnderline(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNotBlinking() {
        writeSgr(5);
        writeSgr(25);
        assertFalse(VideoAttribute.isBlink(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNotReversed() {
        writeSgr(7);
        writeSgr(27);
        assertFalse(VideoAttribute.isInverse(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrNotHidden() {
        writeSgr(8);
        writeSgr(28);
        assertFalse(VideoAttribute.isHidden(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrForegroundRed() {
        writeSgr(31); // red foreground
        assertEquals(1, VideoAttribute.getFg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrForegroundGreen() {
        writeSgr(32); // green foreground
        assertEquals(2, VideoAttribute.getFg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrBackgroundBlue() {
        writeSgr(44); // blue background
        assertEquals(4, VideoAttribute.getBg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrDefaultForeground() {
        writeSgr(31); // red
        writeSgr(39); // default fg
        assertEquals(VideoAttribute.DEFAULT_FG, VideoAttribute.getFg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrDefaultBackground() {
        writeSgr(44); // blue bg
        writeSgr(49); // default bg
        assertEquals(VideoAttribute.DEFAULT_BG, VideoAttribute.getBg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrBrightForeground() {
        writeSgr(91); // bright red foreground
        assertEquals(9, VideoAttribute.getFg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrBrightBackground() {
        writeSgr(104); // bright blue background
        assertEquals(12, VideoAttribute.getBg(display.getCurrentAttribute()));
    }

    @Test
    public void testSgrMultipleParamsInOneSequence() {
        // CSI 1;31;44 m = bold + red fg + blue bg
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '1');
        display.write((byte) ';');
        display.write((byte) '3');
        display.write((byte) '1');
        display.write((byte) ';');
        display.write((byte) '4');
        display.write((byte) '4');
        display.write((byte) 'm');
        int attr = display.getCurrentAttribute();
        assertTrue(VideoAttribute.isBold(attr));
        assertEquals(1, VideoAttribute.getFg(attr));
        assertEquals(4, VideoAttribute.getBg(attr));
    }

    @Test
    public void testSgrAppliedToCharacter() {
        writeSgr(31); // red foreground
        display.write((byte) 'R');
        int attr = display.getAttributeMemory()[0];
        assertEquals(1, VideoAttribute.getFg(attr));
    }

    @Test
    public void testSgrNoParamResets() {
        writeSgr(1); // bold
        // CSI m with no params = reset
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'm');
        assertEquals(VideoAttribute.DEFAULT, display.getCurrentAttribute());
    }

    @Test
    public void testSgrPreservedAcrossCharacters() {
        writeSgr(31); // red fg
        display.write((byte) 'A');
        display.write((byte) 'B');
        assertEquals(1, VideoAttribute.getFg(display.getAttributeMemory()[0]));
        assertEquals(1, VideoAttribute.getFg(display.getAttributeMemory()[1]));
    }

    @Test
    public void testSgrExtendedColor256() {
        // CSI 38;5;9 m = extended foreground color 9 (bright red)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        for (byte b : "38;5;9".getBytes()) display.write(b);
        display.write((byte) 'm');
        assertEquals(9, VideoAttribute.getFg(display.getCurrentAttribute()));
    }

    // ========== Erase in Display (ED) ==========

    @Test
    public void testEraseInDisplayFromCursorToEnd() {
        // Fill line 0
        for (int i = 0; i < DEFAULT_COLUMNS; i++) display.write((byte) 'A');
        // Fill line 1
        for (int i = 0; i < DEFAULT_COLUMNS; i++) display.write((byte) 'B');
        // Move to position column 5, row 0 (0-based CUP)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '0');
        display.write((byte) ';');
        display.write((byte) '5');
        display.write((byte) 'H');
        // ED 0 = erase from cursor to end of screen
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'J');
        // First 5 chars on line 0 should still be 'A'
        for (int i = 0; i < 5; i++) {
            assertEquals('A', display.getVideoMemory()[i]);
        }
        // Position 5 onwards should be spaces
        assertEquals(' ', display.getVideoMemory()[5]);
        // Line 1 should be erased
        assertEquals(' ', display.getVideoMemory()[DEFAULT_COLUMNS]);
    }

    @Test
    public void testEraseInDisplayFromBeginningToCursor() {
        for (int i = 0; i < DEFAULT_COLUMNS; i++) display.write((byte) 'A');
        // Move to column 5, row 0
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '0');
        display.write((byte) ';');
        display.write((byte) '5');
        display.write((byte) 'H');
        // ED 1 = erase from beginning to cursor
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '1');
        display.write((byte) 'J');
        // Positions 0-5 should be spaces
        for (int i = 0; i <= 5; i++) {
            assertEquals(' ', display.getVideoMemory()[i]);
        }
        // Position 6 onwards should still be 'A'
        assertEquals('A', display.getVideoMemory()[6]);
    }

    @Test
    public void testEraseInDisplayEntire() {
        for (int i = 0; i < 10; i++) display.write((byte) 'X');
        // ED 2 = erase entire screen
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '2');
        display.write((byte) 'J');
        for (char c : display.getVideoMemory()) {
            assertEquals(' ', c);
        }
    }

    // ========== Erase in Line (EL) ==========

    @Test
    public void testEraseInLineFromCursorToEnd() {
        for (int i = 0; i < 10; i++) display.write((byte) 'A');
        // Move to column 5, row 0 (0-based)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '0');
        display.write((byte) ';');
        display.write((byte) '5');
        display.write((byte) 'H');
        // EL 0 = erase from cursor to end of line
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'K');
        for (int i = 0; i < 5; i++) assertEquals('A', display.getVideoMemory()[i]);
        for (int i = 5; i < DEFAULT_COLUMNS; i++) assertEquals(' ', display.getVideoMemory()[i]);
    }

    @Test
    public void testEraseInLineFromBeginningToCursor() {
        for (int i = 0; i < 10; i++) display.write((byte) 'A');
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '0');
        display.write((byte) ';');
        display.write((byte) '5');
        display.write((byte) 'H');
        // EL 1
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '1');
        display.write((byte) 'K');
        for (int i = 0; i <= 5; i++) assertEquals(' ', display.getVideoMemory()[i]);
        assertEquals('A', display.getVideoMemory()[6]);
    }

    @Test
    public void testEraseInLineEntire() {
        for (int i = 0; i < 10; i++) display.write((byte) 'A');
        // Move to column 3, row 0 (0-based)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '0');
        display.write((byte) ';');
        display.write((byte) '3');
        display.write((byte) 'H');
        // EL 2 = erase entire line
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '2');
        display.write((byte) 'K');
        for (int i = 0; i < DEFAULT_COLUMNS; i++) assertEquals(' ', display.getVideoMemory()[i]);
    }

    // ========== CSI with no params uses defaults ==========

    @Test
    public void testCsiCursorUpNoParam() {
        // Move down 3, then CSI A (no params = move up 1)
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) '3');
        display.write((byte) 'B');
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'A');
        assertEquals(2, display.getCursorPoint().y);
    }

    @Test
    public void testCsiCursorDownNoParam() {
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'B');
        assertEquals(1, display.getCursorPoint().y);
    }

    @Test
    public void testCsiCursorForwardNoParam() {
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'C');
        assertEquals(1, display.getCursorPoint().x);
    }

    @Test
    public void testCsiCursorBackwardNoParam() {
        display.write((byte) 'X');
        display.write((byte) 'Y');
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'D');
        assertEquals(1, display.getCursorPoint().x);
    }

    // ========== DECSTBM (Set Top and Bottom Margins) ==========

    /**
     * Helper to send CSI Pt ; Pb r (DECSTBM).
     */
    private void writeDecstbm(int top, int bottom) {
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        for (char c : String.valueOf(top).toCharArray()) display.write((byte) c);
        display.write((byte) ';');
        for (char c : String.valueOf(bottom).toCharArray()) display.write((byte) c);
        display.write((byte) 'r');
    }

    @Test
    public void testDecstbmMovesCursorHome() {
        // Move cursor somewhere first
        display.write((byte) 'X');
        display.write((byte) 'Y');
        assertNotEquals(new Point(0, 0), display.getCursorPoint());

        // DECSTBM should reset cursor to home
        writeDecstbm(1, DEFAULT_ROWS);
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testDecstbmNoParamsResetsRegion() {
        // CSI r with no params should reset scrolling region to full screen
        display.write((byte) 0x1B);
        display.write((byte) 0x5B);
        display.write((byte) 'r');
        assertEquals(new Point(0, 0), display.getCursorPoint());
    }

    @Test
    public void testDecstbmRollUpDirectlyOnlyAffectsRegion() {
        // Set scrolling region to rows 2-4 (1-based) = rows 1-3 (0-based)
        writeDecstbm(2, 4);

        // Directly fill column 0 of rows 0-4 with distinct chars
        display.videoMemory[0] = 'A';                       // row 0
        display.videoMemory[DEFAULT_COLUMNS] = 'B';         // row 1
        display.videoMemory[2 * DEFAULT_COLUMNS] = 'C';     // row 2
        display.videoMemory[3 * DEFAULT_COLUMNS] = 'D';     // row 3
        display.videoMemory[4 * DEFAULT_COLUMNS] = 'E';     // row 4

        display.rollUp();

        assertEquals('A', display.videoMemory[0]);
        assertEquals('C', display.videoMemory[DEFAULT_COLUMNS]);
        assertEquals('D', display.videoMemory[2 * DEFAULT_COLUMNS]);
        assertEquals(' ', display.videoMemory[3 * DEFAULT_COLUMNS]);
        assertEquals('E', display.videoMemory[4 * DEFAULT_COLUMNS]);
    }

    @Test
    public void testDecstbmScrollUpOnlyAffectsRegion() {
        // Set scrolling region to rows 2-4 (1-based) = rows 1-3 (0-based)
        writeDecstbm(2, 4);

        // Directly fill column 0 of rows 0-4 with distinct chars
        display.videoMemory[0] = 'A';                       // row 0
        display.videoMemory[DEFAULT_COLUMNS] = 'B';         // row 1
        display.videoMemory[2 * DEFAULT_COLUMNS] = 'C';     // row 2
        display.videoMemory[3 * DEFAULT_COLUMNS] = 'D';     // row 3
        display.videoMemory[4 * DEFAULT_COLUMNS] = 'E';     // row 4

        // Move cursor to scrollBottom (row 3, 0-based)
        cursor.move(0, 3);

        // Line feed at bottom of scrolling region triggers rollUp within region
        display.write((byte) 0x0A); // LF

        assertEquals('A', display.videoMemory[0]);
        assertEquals('C', display.videoMemory[DEFAULT_COLUMNS]);
        assertEquals('D', display.videoMemory[2 * DEFAULT_COLUMNS]);
        assertEquals(' ', display.videoMemory[3 * DEFAULT_COLUMNS]);
        assertEquals('E', display.videoMemory[4 * DEFAULT_COLUMNS]);
    }


    @Test
    public void testDecstbmScrollDownOnlyAffectsRegion() {
        // Directly fill column 0 of rows 0-4 with distinct chars
        display.videoMemory[0] = 'A';                       // row 0
        display.videoMemory[DEFAULT_COLUMNS] = 'B';         // row 1
        display.videoMemory[2 * DEFAULT_COLUMNS] = 'C';     // row 2
        display.videoMemory[3 * DEFAULT_COLUMNS] = 'D';     // row 3
        display.videoMemory[4 * DEFAULT_COLUMNS] = 'E';     // row 4

        // Set scrolling region to rows 2-4 (1-based) = rows 1-3 (0-based)
        writeDecstbm(2, 4);

        // Move cursor to scrollTop (row 1, 0-based)
        cursor.move(0, 1);

        // Reverse index (ESC M) at top of scrolling region triggers rollDown within region
        display.write((byte) 0x1B);
        display.write((byte) 0x4D);

        // Row 0 (outside region) should be unchanged: 'A'
        assertEquals('A', display.videoMemory[0]);
        // Row 1 (scrollTop) should be cleared to space (new blank line scrolled in)
        assertEquals(' ', display.videoMemory[DEFAULT_COLUMNS]);
        // Row 2 should now contain what was in row 1 (old scrollTop): 'B'
        assertEquals('B', display.videoMemory[2 * DEFAULT_COLUMNS]);
        // Row 3 should now contain what was in row 2: 'C'
        assertEquals('C', display.videoMemory[3 * DEFAULT_COLUMNS]);
        // Row 4 (outside region) should be unchanged: 'E'
        assertEquals('E', display.videoMemory[4 * DEFAULT_COLUMNS]);
    }

    @Test
    public void testDecstbmInvalidTopGreaterThanBottomIsIgnored() {
        // Setting top >= bottom should be ignored (region unchanged)
        writeDecstbm(10, 5);
        // Cursor still goes home
        assertEquals(new Point(0, 0), display.getCursorPoint());

        // Put a char at last row to verify full-screen scrolling region is intact
        cursor.move(0, DEFAULT_ROWS - 1);
        display.write((byte) 'Z');
        assertEquals('Z', display.videoMemory[(DEFAULT_ROWS - 1) * DEFAULT_COLUMNS]);
    }

    // ========== DECSC/DECRC saves/restores attribute ==========

    @Test
    public void testSaveCursorAlsoSavesAttribute() {
        writeSgr(31); // red fg
        int savedAttr = display.getCurrentAttribute();
        // ESC 7 = save cursor
        display.write((byte) 0x1B);
        display.write((byte) '7');
        // Change attribute
        writeSgr(0); // reset
        assertNotEquals(savedAttr, display.getCurrentAttribute());
        // ESC 8 = restore cursor
        display.write((byte) 0x1B);
        display.write((byte) '8');
        assertEquals(savedAttr, display.getCurrentAttribute());
    }

    // ========== Reset clears attribute ==========

    @Test
    public void testResetClearsCurrentAttribute() {
        writeSgr(1, 31, 44); // bold + red fg + blue bg
        display.reset();
        assertEquals(VideoAttribute.DEFAULT, display.getCurrentAttribute());
    }
}
