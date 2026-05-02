/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class MouseHandlerTest {
    private MemoryTableModel tableModel;

    @Before
    public void setUp() {
        tableModel = new MemoryTableModel(TestMemoryContextFactory.create(65536, 1, 0));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new MouseHandler(null, () -> {
        });
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullRunnableThrows() {
        new MouseHandler(tableModel, null);
    }

    @Test
    public void testMouseWheelMovedForward() {
        MouseHandler handler = new MouseHandler(tableModel, () -> {
        });
        assertEquals(0, tableModel.getPage());
        Component dummyComponent = new Component() {
        };
        MouseWheelEvent event = new MouseWheelEvent(
                dummyComponent, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                0, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1
        );
        handler.mouseWheelMoved(event);
        assertEquals(1, tableModel.getPage());
    }

    @Test
    public void testMouseWheelMovedBackward() {
        MouseHandler handler = new MouseHandler(tableModel, () -> {
        });
        tableModel.setPage(5);
        Component dummyComponent = new Component() {
        };
        MouseWheelEvent event = new MouseWheelEvent(
                dummyComponent, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                0, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1
        );
        handler.mouseWheelMoved(event);
        assertEquals(4, tableModel.getPage());
    }

    @Test
    public void testMouseWheelMovedOverflowWrapsToZero() {
        MouseHandler handler = new MouseHandler(tableModel, () -> {
        });
        tableModel.setPage(tableModel.getPageCount() - 1);
        Component dummyComponent = new Component() {
        };
        MouseWheelEvent event = new MouseWheelEvent(
                dummyComponent, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                0, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1
        );
        handler.mouseWheelMoved(event);
        assertEquals(0, tableModel.getPage());
    }

    @Test
    public void testMouseWheelMovedUnderflowWrapsToMax() {
        MouseHandler handler = new MouseHandler(tableModel, () -> {
        });
        tableModel.setPage(0);
        Component dummyComponent = new Component() {
        };
        MouseWheelEvent event = new MouseWheelEvent(
                dummyComponent, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                0, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1
        );
        handler.mouseWheelMoved(event);
        assertEquals(tableModel.getPageCount() - 1, tableModel.getPage());
    }

    @Test
    public void testMouseClickedCallsUpdateMemoryValue() {
        AtomicBoolean called = new AtomicBoolean(false);
        MouseHandler handler = new MouseHandler(tableModel, () -> called.set(true));
        Component dummyComponent = new Component() {
        };
        MouseEvent event = new MouseEvent(
                dummyComponent, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 0, 0, 1, false
        );
        handler.mouseClicked(event);
        assertTrue(called.get());
    }
}
