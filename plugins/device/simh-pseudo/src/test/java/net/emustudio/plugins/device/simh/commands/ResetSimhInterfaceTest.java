/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResetSimhInterfaceTest extends CommandTestBase {

    @Before
    public void setUp() {
        StartTimer.INS.reset(control);
    }

    @Test
    public void testStartClearsCommand() {
        ResetSimhInterface.INS.start(control);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testStartResetsTimerStack() {
        StartTimer.INS.start(control);
        StartTimer.INS.start(control);
        assertEquals(2, StartTimer.INS.markTimeSP);

        ResetSimhInterface.INS.start(control);
        assertEquals(0, StartTimer.INS.markTimeSP);
    }
}

