/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class DisplayDummyTest {

    @Test
    public void testDummyWriteDoesNotThrow() {
        Display.DUMMY.write((byte) 0x42);
    }

    @Test
    public void testDummyResetDoesNotThrow() {
        Display.DUMMY.reset();
    }

    @Test
    public void testDummyCloseDoesNotThrow() throws Exception {
        Display.DUMMY.close();
    }

    @Test
    public void testDummyGetRowsReturnsZero() {
        assertEquals(0, Display.DUMMY.getRows());
    }

    @Test
    public void testDummyGetColumnsReturnsZero() {
        assertEquals(0, Display.DUMMY.getColumns());
    }

    @Test
    public void testDummyGetCursorPointReturnsNull() {
        assertNull(Display.DUMMY.getCursorPoint());
    }

    @Test
    public void testDummyGetVideoMemoryReturnsEmpty() {
        assertEquals(0, Display.DUMMY.getVideoMemory().length);
    }

    @Test
    public void testDummyRollUpDoesNotThrow() {
        Display.DUMMY.rollUp();
    }

    @Test
    public void testDummyRollDownDoesNotThrow() {
        Display.DUMMY.rollDown();
    }
}

