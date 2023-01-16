package net.emustudio.plugins.device.adm3a.interaction;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_ROWS;
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
        assertEquals(new Point(), cursor.getCursorPoint());
    }

    @Test
    public void testMove() {
        Point expected = new Point(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1);
        cursor.move(expected.x, expected.y);
        assertEquals(expected, cursor.getCursorPoint());
    }

    @Test
    public void testMoveOutOfBounds() {
        Point point = new Point(DEFAULT_COLUMNS + 1, DEFAULT_ROWS + 1);
        cursor.move(point.x, point.y);
        assertEquals(new Point(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1), cursor.getCursorPoint());
    }

    @Test
    public void testMoveOutOfBounds2() {
        Point point = new Point(-1, -1);
        cursor.move(point.x, point.y);
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveForwards() {
        cursor.moveForwards();
        assertEquals(new Point(1, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveForwardsOutOfBounds() {
        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.moveForwards();
        assertEquals(new Point(DEFAULT_COLUMNS - 1, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveBackwards() {
        cursor.move(1, 0);
        cursor.moveBackwards();
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveBackwardsOutOfBounds() {
        cursor.moveBackwards();
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveForwardsNoRolling() {
        Cursor.LineRoller lineRoller = mock(Cursor.LineRoller.class);
        replay(lineRoller);

        cursor.moveForwardsRolling(lineRoller);
        assertEquals(new Point(1, 0), cursor.getCursorPoint());
        verify(lineRoller);
    }

    @Test
    public void testMoveForwardsNoRolling2() {
        Cursor.LineRoller lineRoller = mock(Cursor.LineRoller.class);
        replay(lineRoller);

        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.moveForwardsRolling(lineRoller);
        assertEquals(new Point(0, 1), cursor.getCursorPoint());
        verify(lineRoller);
    }

    @Test
    public void testMoveForwardsRolling() {
        Cursor.LineRoller lineRoller = mock(Cursor.LineRoller.class);
        lineRoller.rollLine();
        expectLastCall().once();
        replay(lineRoller);

        cursor.move(DEFAULT_COLUMNS - 1, DEFAULT_ROWS - 1);
        cursor.moveForwardsRolling(lineRoller);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getCursorPoint());
        verify(lineRoller);
    }

    @Test
    public void testMoveUp() {
        cursor.move(0, 1);
        cursor.moveUp();
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveUpOutOfBounds() {
        cursor.moveUp();
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }

    @Test
    public void testMoveDownNoRolling() {
        Cursor.LineRoller lineRoller = mock(Cursor.LineRoller.class);
        replay(lineRoller);

        cursor.moveDown(lineRoller);
        assertEquals(new Point(0, 1), cursor.getCursorPoint());
        verify(lineRoller);
    }

    @Test
    public void testMoveDownRolling() {
        Cursor.LineRoller lineRoller = mock(Cursor.LineRoller.class);
        lineRoller.rollLine();
        expectLastCall().once();
        replay(lineRoller);

        cursor.move(0, DEFAULT_ROWS - 1);
        cursor.moveDown(lineRoller);
        assertEquals(new Point(0, DEFAULT_ROWS - 1), cursor.getCursorPoint());
        verify(lineRoller);
    }

    @Test
    public void testCarriageReturn() {
        cursor.move(DEFAULT_COLUMNS - 1, 0);
        cursor.carriageReturn();
        assertEquals(new Point(0, 0), cursor.getCursorPoint());
    }
}
