/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GetCommonTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        GetCommon.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturns16BitCommonBoundary() {
        int boundary = 0xC000; // typical common boundary

        expect(memory.getCommonBoundary()).andReturn(boundary).anyTimes();
        replay(memory);

        GetCommon.INS.start(control);
        resetClearFlags();

        // First byte - low
        byte low = GetCommon.INS.read(control);
        assertEquals((byte) (boundary & 0xFF), low);
        assertFalse(isCommandCleared());

        // Second byte - high
        byte high = GetCommon.INS.read(control);
        assertEquals((byte) ((boundary >> 8) & 0xFF), high);
        assertTrue(isCommandCleared());

        // Reconstruct the value
        int result = (low & 0xFF) | ((high & 0xFF) << 8);
        assertEquals(boundary, result);

        verify(memory);
    }

    @Test
    public void testResetResetsPosition() {
        expect(memory.getCommonBoundary()).andReturn(0xC000).anyTimes();
        replay(memory);

        GetCommon.INS.start(control);
        GetCommon.INS.read(control); // Read first byte

        GetCommon.INS.reset(control);
        GetCommon.INS.start(control);
        resetClearFlags();

        // Should read low byte again (not high)
        byte low = GetCommon.INS.read(control);
        assertEquals((byte) 0x00, low);
        assertFalse(isCommandCleared());
    }
}

