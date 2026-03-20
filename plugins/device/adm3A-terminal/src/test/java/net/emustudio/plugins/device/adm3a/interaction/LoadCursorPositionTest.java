/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_ROWS;
import static org.junit.Assert.*;

public class LoadCursorPositionTest {
    private Cursor cursor;
    private LoadCursorPosition loadCursorPosition;

    @Before
    public void setUp() {
        cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        loadCursorPosition = new LoadCursorPosition(cursor);
    }

    @Test
    public void testNormalCharIsNotAccepted() {
        assertTrue(loadCursorPosition.notAccepted((byte) 'A'));
    }

    @Test
    public void testEscapeStartsSequence() {
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B));
    }

    @Test
    public void testEscEqualsYXSequenceMoveCursor() {
        // ESC = Y X
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B)); // ESC
        assertFalse(loadCursorPosition.notAccepted((byte) '='));   // =
        assertFalse(loadCursorPosition.notAccepted((byte) (5 + 32)));  // Y = 5
        assertFalse(loadCursorPosition.notAccepted((byte) (10 + 32))); // X = 10

        assertEquals(new Point(10, 5), cursor.getCursorPoint());
    }

    @Test
    public void testEscFollowedByNonEqualsResetsSequence() {
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B)); // ESC
        // Something other than '=' should reset to ESCAPE state
        assertTrue(loadCursorPosition.notAccepted((byte) 'A'));

        // Next normal char should be not accepted normally
        assertTrue(loadCursorPosition.notAccepted((byte) 'B'));
    }

    @Test
    public void testYOutOfBoundsResetsSequence() {
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B)); // ESC
        assertFalse(loadCursorPosition.notAccepted((byte) '='));   // =
        // Y out of bounds (less than ' ')
        assertTrue(loadCursorPosition.notAccepted((byte) 0x10));

        // Should be back in ESCAPE state
        assertTrue(loadCursorPosition.notAccepted((byte) 'A'));
    }

    @Test
    public void testXOutOfBoundsResetsSequence() {
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B)); // ESC
        assertFalse(loadCursorPosition.notAccepted((byte) '='));   // =
        assertFalse(loadCursorPosition.notAccepted((byte) (5 + 32)));  // Y = 5
        // X out of bounds
        assertTrue(loadCursorPosition.notAccepted((byte) 0x10));

        // Cursor should not have moved
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMultipleSequences() {
        // First sequence: move to (10, 5)
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B));
        assertFalse(loadCursorPosition.notAccepted((byte) '='));
        assertFalse(loadCursorPosition.notAccepted((byte) (5 + 32)));
        assertFalse(loadCursorPosition.notAccepted((byte) (10 + 32)));
        assertEquals(new Point(10, 5), cursor.getCursorPoint());

        // Second sequence: move to (3, 2)
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B));
        assertFalse(loadCursorPosition.notAccepted((byte) '='));
        assertFalse(loadCursorPosition.notAccepted((byte) (2 + 32)));
        assertFalse(loadCursorPosition.notAccepted((byte) (3 + 32)));
        assertEquals(new Point(3, 2), cursor.getCursorPoint());
    }

    @Test
    public void testMoveToOrigin() {
        // ESC = <space> <space> -> move to (0, 0)
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B));
        assertFalse(loadCursorPosition.notAccepted((byte) '='));
        assertFalse(loadCursorPosition.notAccepted((byte) ' '));
        assertFalse(loadCursorPosition.notAccepted((byte) ' '));
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMaxBoundsCoordinate() {
        // 'o' is the maximum valid coordinate char
        assertFalse(loadCursorPosition.notAccepted((byte) 0x1B));
        assertFalse(loadCursorPosition.notAccepted((byte) '='));
        assertFalse(loadCursorPosition.notAccepted((byte) 'o'));  // Y = 79
        assertFalse(loadCursorPosition.notAccepted((byte) 'o'));  // X = 79

        // Cursor should be clamped to max display bounds
        Point p = cursor.getCursorPoint();
        assertTrue(p.x <= DEFAULT_COLUMNS - 1);
        assertTrue(p.y <= DEFAULT_ROWS - 1);
    }
}

