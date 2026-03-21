/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GetClockCPM3Test extends CommandTestBase {

    @Before
    public void setUp() {
        SetClockCPM3.INS.reset(control);
        GetClockCPM3.INS.reset(control);
    }

    @Test
    public void testStartClearsWriteCommand() {
        GetClockCPM3.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturns5Bytes() {
        GetClockCPM3.INS.start(control);
        resetClearFlags();

        // Read 5 bytes: days_low, days_high, HH, MM, SS
        for (int i = 0; i < 4; i++) {
            GetClockCPM3.INS.read(control);
            assertFalse("Command should not be cleared after byte " + i, isCommandCleared());
        }
        // 5th byte should clear command
        GetClockCPM3.INS.read(control);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testResetClearsState() {
        GetClockCPM3.INS.start(control);
        GetClockCPM3.INS.read(control); // read first byte

        GetClockCPM3.INS.reset(control);

        // After reset, reading without start should return 0 and clear command
        resetClearFlags();
        byte result = GetClockCPM3.INS.read(control);
        assertEquals(0, result);
        assertTrue(isCommandCleared());
    }
}

