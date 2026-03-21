/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ShowTimerTest extends CommandTestBase {

    @Before
    public void setUp() {
        StartTimer.INS.reset(control);
    }

    @Test
    public void testShowTimerWithActiveTimerClearsCommand() {
        StartTimer.INS.start(control);
        resetClearFlags();

        ShowTimer.INS.start(control);
        assertTrue(isCommandCleared());
        // Timer should still be on the stack (not popped)
        assertEquals(1, StartTimer.INS.markTimeSP);
    }

    @Test
    public void testShowTimerWithNoActiveTimerClearsCommand() {
        ShowTimer.INS.start(control);
        assertTrue(isCommandCleared());
    }
}

