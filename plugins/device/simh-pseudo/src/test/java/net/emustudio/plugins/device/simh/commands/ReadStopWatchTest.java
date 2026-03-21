/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadStopWatchTest extends CommandTestBase {

    @Before
    public void setUp() {
        ReadStopWatch.INS.reset(control);
        ReadStopWatch.INS.stopWatchNow = 0;
    }

    @Test
    public void testStartClearsWriteCommand() {
        ReadStopWatch.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturns4ByteDelta() {
        // Set up known delta
        ReadStopWatch.INS.stopWatchNow = System.currentTimeMillis() - 1000; // 1 second ago
        ReadStopWatch.INS.start(control);
        resetClearFlags();

        // Read 4 bytes (32-bit value)
        ReadStopWatch.INS.read(control);
        assertFalse(isCommandCleared());
        ReadStopWatch.INS.read(control);
        assertFalse(isCommandCleared());
        ReadStopWatch.INS.read(control);
        assertFalse(isCommandCleared());
        ReadStopWatch.INS.read(control);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testReturnsCorrectDeltaValue() {
        long expectedDelta = 0x12345678L;
        // Set stopWatchNow so that delta = current - now = expectedDelta
        long fakeNow = System.currentTimeMillis();
        ReadStopWatch.INS.stopWatchNow = fakeNow - expectedDelta;

        // Manually compute the delta at start time
        ReadStopWatch.INS.start(control);
        resetClearFlags();

        byte b0 = ReadStopWatch.INS.read(control);
        byte b1 = ReadStopWatch.INS.read(control);
        byte b2 = ReadStopWatch.INS.read(control);
        byte b3 = ReadStopWatch.INS.read(control);

        long result = (b0 & 0xFFL) | ((b1 & 0xFFL) << 8) | ((b2 & 0xFFL) << 16) | ((b3 & 0xFFL) << 24);
        // Allow some tolerance for timing
        assertTrue("Expected delta around " + expectedDelta + " but got " + result,
                Math.abs(result - expectedDelta) < 100);
    }

    @Test
    public void testResetResetsPosition() {
        ReadStopWatch.INS.stopWatchNow = System.currentTimeMillis();
        ReadStopWatch.INS.start(control);
        ReadStopWatch.INS.read(control); // read first byte

        ReadStopWatch.INS.reset(control);
        ReadStopWatch.INS.start(control);
        resetClearFlags();

        // Should start from byte 0 again
        ReadStopWatch.INS.read(control);
        assertFalse(isCommandCleared());
    }
}

