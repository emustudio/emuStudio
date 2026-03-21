/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StartTimerTest extends CommandTestBase {

    @Before
    public void setUp() {
        StartTimer.INS.reset(control);
    }

    @Test
    public void testStartPushesTimerOntoStack() {
        StartTimer.INS.start(control);
        assertEquals(1, StartTimer.INS.markTimeSP);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testMultipleStartsPushMultipleTimers() {
        StartTimer.INS.start(control);
        StartTimer.INS.start(control);
        StartTimer.INS.start(control);
        assertEquals(3, StartTimer.INS.markTimeSP);
    }

    @Test
    public void testStackOverflowIsHandledGracefully() {
        // push 10 timers (stack limit)
        for (int i = 0; i < 10; i++) {
            StartTimer.INS.start(control);
        }
        assertEquals(10, StartTimer.INS.markTimeSP);

        // 11th push should not increase the stack pointer
        StartTimer.INS.start(control);
        assertEquals(10, StartTimer.INS.markTimeSP);
    }

    @Test
    public void testResetClearsStackPointer() {
        StartTimer.INS.start(control);
        StartTimer.INS.start(control);
        StartTimer.INS.reset(control);
        assertEquals(0, StartTimer.INS.markTimeSP);
    }
}

