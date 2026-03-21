/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StopTimerTest extends CommandTestBase {

    @Before
    public void setUp() {
        StartTimer.INS.reset(control);
    }

    @Test
    public void testStopTimerWithActiveTimer() {
        StartTimer.INS.start(control);
        resetClearFlags();

        StopTimer.INS.start(control);

        assertEquals(0, StartTimer.INS.markTimeSP);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testStopTimerWithNoActiveTimer() {
        StopTimer.INS.start(control);
        assertEquals(0, StartTimer.INS.markTimeSP);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testStopTimerPopsFromStack() {
        StartTimer.INS.start(control);
        StartTimer.INS.start(control);
        resetClearFlags();

        StopTimer.INS.start(control);
        assertEquals(1, StartTimer.INS.markTimeSP);
    }
}

