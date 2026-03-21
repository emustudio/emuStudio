/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SetTimerDeltaTest extends CommandTestBase {

    @Before
    public void setUp() {
        SetTimerDelta.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        SetTimerDelta.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteTwoByteDelta() {
        int delta = 500; // 500ms

        SetTimerDelta.INS.start(control);
        resetClearFlags();

        // Write low byte
        SetTimerDelta.INS.write((byte) (delta & 0xFF), control);
        assertFalse(isCommandCleared());

        // Write high byte
        SetTimerDelta.INS.write((byte) ((delta >> 8) & 0xFF), control);
        assertTrue(isCommandCleared());

        assertEquals(delta, SetTimerDelta.INS.timerDelta);
    }

    @Test
    public void testWriteZeroDeltaFallsBackToDefault() {
        SetTimerDelta.INS.start(control);
        resetClearFlags();

        // Write 0
        SetTimerDelta.INS.write((byte) 0, control);
        SetTimerDelta.INS.write((byte) 0, control);

        // Should fall back to default (100ms)
        assertEquals(100, SetTimerDelta.INS.timerDelta);
    }

    @Test
    public void testResetResetsPosition() {
        SetTimerDelta.INS.start(control);
        // Write only first byte
        SetTimerDelta.INS.write((byte) 0x10, control);
        // Reset before second byte
        SetTimerDelta.INS.reset(control);

        SetTimerDelta.INS.start(control);
        resetClearFlags();

        // Should accept low byte again (position reset to 0)
        SetTimerDelta.INS.write((byte) (200 & 0xFF), control);
        assertFalse(isCommandCleared()); // expecting high byte next
    }
}

