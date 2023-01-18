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
}
