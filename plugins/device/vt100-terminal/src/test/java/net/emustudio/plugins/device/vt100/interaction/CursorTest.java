/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.plugins.device.vt100.api.Display;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.vt100.TerminalSettings.DEFAULT_ROWS;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class CursorTest {
    private Cursor cursor;

    @Before
    public void setUp() {
        this.cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    @Test
    public void testHome() {
        cursor.home();
        assertEquals(new Point(), cursor.getRect().getLocation());
    }

    @Test
    public void testMove() {
        Point expected = new Point(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1);
        cursor.move(expected.x, expected.y);
        assertEquals(expected, cursor.getRect().getLocation());
    }

    @Test
    public void testMoveOutOfBounds() {
        Point point = new Point(DEFAULT_COLUMNS + 1, DEFAULT_ROWS + 1);
        cursor.move(point.x, point.y);
        assertEquals(new Point(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveOutOfBounds2() {
        Point point = new Point(-1, -1);
        cursor.move(point.x, point.y);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveForwards() {
        cursor.moveForwards();
        assertEquals(new Point(1, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveForwardsOutOfBounds() {
        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.moveForwards();
        assertEquals(new Point(DEFAULT_COLUMNS - 1, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveBackwards() {
        cursor.move(1, 0);
        cursor.moveBackwards();
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveBackwardsOutOfBounds() {
        cursor.moveBackwards();
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveForwardsNoRolling() {
        Display display = mock(Display.class);
        replay(display);

        cursor.moveForwardsRolling(display);
        assertEquals(new Point(1, 0), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveForwardsNoRolling2() {
        Display display = mock(Display.class);
        replay(display);

        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.moveForwardsRolling(display);
        assertEquals(new Point(0, 1), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveForwardsRolling() {
        Display display = mock(Display.class);
        display.rollUp();
        expectLastCall().once();
        replay(display);

        cursor.move(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1);
        cursor.moveForwardsRolling(display);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveUp() {
        cursor.move(0, 1);
        cursor.moveUp(1);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveUpOutOfBounds() {
        cursor.moveUp(1);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveDownNoRolling() {
        Display display = mock(Display.class);
        replay(display);

        cursor.moveDownRolling(display);
        assertEquals(new Point(0, 1), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveDownRolling() {
        Display display = mock(Display.class);
        display.rollUp();
        expectLastCall().once();
        replay(display);

        cursor.move(0, DEFAULT_ROWS - 1);
        cursor.moveDownRolling(display);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testCarriageReturn() {
        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.carriageReturn();
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    // ========== Additional tests ==========

    @Test
    public void testMoveDown() {
        cursor.moveDown();
        assertEquals(new Point(0, 1), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveDownMultipleLines() {
        cursor.moveDown(5);
        assertEquals(new Point(0, 5), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveDownOutOfBounds() {
        cursor.moveDown(DEFAULT_ROWS + 10);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveUpRolling() {
        Display display = mock(Display.class);
        display.rollDown();
        expectLastCall().once();
        replay(display);

        // At row 0, moveUpRolling should trigger rollDown
        cursor.moveUpRolling(display);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveUpRollingNoRolling() {
        Display display = mock(Display.class);
        replay(display);

        cursor.move(0, 5);
        cursor.moveUpRolling(display);
        assertEquals(new Point(0, 4), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testMoveForwardsMultiple() {
        cursor.moveForwards(10);
        assertEquals(new Point(10, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveForwardsMultipleOutOfBounds() {
        cursor.moveForwards(DEFAULT_COLUMNS + 10);
        assertEquals(new Point(DEFAULT_COLUMNS - 1, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveBackwardsMultiple() {
        cursor.move(10, 0);
        cursor.moveBackwards(5);
        assertEquals(new Point(5, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveBackwardsMultipleOutOfBounds() {
        cursor.move(3, 0);
        cursor.moveBackwards(10);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testSetSize() {
        cursor.setSize(40, 12);
        Rectangle rect = cursor.getRect();
        assertEquals(40, rect.width);
        assertEquals(12, rect.height);
    }

    @Test
    public void testMovePoint() {
        Point p = new Point(5, 3);
        cursor.move(p);
        assertEquals(new Point(5, 3), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveUpMultipleLinesLimitedToZero() {
        cursor.move(0, 2);
        cursor.moveUp(10);
        assertEquals(new Point(0, 0), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveDownExactlyToLastRow() {
        cursor.moveDown(DEFAULT_ROWS - 1);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getRect().getLocation());
    }

    @Test
    public void testMoveForwardsRollingMultipleTimes() {
        Display display = mock(Display.class);
        replay(display);

        for (int i = 0; i < 5; i++) {
            cursor.moveForwardsRolling(display);
        }
        assertEquals(new Point(5, 0), cursor.getRect().getLocation());
        verify(display);
    }

    @Test
    public void testGetRectReturnsCorrectDimensions() {
        Rectangle rect = cursor.getRect();
        assertEquals(DEFAULT_COLUMNS, rect.width);
        assertEquals(DEFAULT_ROWS, rect.height);
        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
    }
}
