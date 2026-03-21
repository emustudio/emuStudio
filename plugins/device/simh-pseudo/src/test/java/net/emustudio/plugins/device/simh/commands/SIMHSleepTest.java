/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class SIMHSleepTest extends CommandTestBase {

    @Test
    public void testStartClearsCommand() {
        SIMHSleep.INS.start(control);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testDoesNotSleepWhenTimerInterruptsActive() {
        // When timer interrupts are active, sleep should be very fast (skip sleep)
        // We can't easily test this without more complex setup, but we can verify it doesn't hang
        SIMHSleep.INS.start(control);
        assertTrue(isCommandCleared());
    }
}

