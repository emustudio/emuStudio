/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import org.junit.Test;

import static org.junit.Assert.*;

public class BreakpointTest {

    @Test
    public void testBreakpointIsException() {
        Breakpoint breakpoint = new Breakpoint();
        assertTrue(breakpoint instanceof Exception);
    }

    @Test
    public void testBreakpointCanBeThrown() {
        try {
            throw new Breakpoint();
        } catch (Breakpoint b) {
            assertNotNull(b);
        }
    }

    @Test
    public void testBreakpointMessageIsNull() {
        Breakpoint breakpoint = new Breakpoint();
        assertNull(breakpoint.getMessage());
    }

    @Test
    public void testBreakpointCauseIsNull() {
        Breakpoint breakpoint = new Breakpoint();
        assertNull(breakpoint.getCause());
    }
}

