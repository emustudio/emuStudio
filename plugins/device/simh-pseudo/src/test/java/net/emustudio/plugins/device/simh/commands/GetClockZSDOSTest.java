/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GetClockZSDOSTest extends CommandTestBase {

    @Before
    public void setUp() {
        SetClockZSDOS.INS.reset(control);
        GetClockZSDOS.INS.reset(control);
    }

    @Test
    public void testStartClearsWriteCommand() {
        GetClockZSDOS.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturns6Bytes() {
        GetClockZSDOS.INS.start(control);
        resetClearFlags();

        // Read 6 bytes: YY MM DD HH MM SS (BCD)
        for (int i = 0; i < 5; i++) {
            GetClockZSDOS.INS.read(control);
            assertFalse("Command should not be cleared after byte " + i, isCommandCleared());
        }
        // 6th byte should clear command
        GetClockZSDOS.INS.read(control);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testResetClearsState() {
        GetClockZSDOS.INS.start(control);
        GetClockZSDOS.INS.read(control); // read first byte

        GetClockZSDOS.INS.reset(control);

        // After reset, reading without start should return 0 and clear command
        resetClearFlags();
        byte result = GetClockZSDOS.INS.read(control);
        assertEquals(0, result);
        assertTrue(isCommandCleared());
    }
}

