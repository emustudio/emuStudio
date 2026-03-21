/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GetCPUClockFrequencyTest extends CommandTestBase {

    @Before
    public void setUp() {
        GetCPUClockFrequency.INS.reset(control);
    }

    @Test
    public void testStartClearsWriteCommand() {
        GetCPUClockFrequency.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturns4ByteFrequency() {
        int frequency = 2000; // 2000 kHz

        expect(cpu.getCPUFrequency()).andReturn(frequency).anyTimes();
        replay(cpu);

        GetCPUClockFrequency.INS.start(control);
        resetClearFlags();

        // Read 4 bytes (32-bit little-endian)
        byte b0 = GetCPUClockFrequency.INS.read(control);
        assertFalse(isCommandCleared());
        byte b1 = GetCPUClockFrequency.INS.read(control);
        assertFalse(isCommandCleared());
        byte b2 = GetCPUClockFrequency.INS.read(control);
        assertFalse(isCommandCleared());
        byte b3 = GetCPUClockFrequency.INS.read(control);
        assertTrue(isCommandCleared());

        int result = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
        assertEquals(frequency, result);

        verify(cpu);
    }

    @Test
    public void testReturnsLargeFrequency() {
        int frequency = 0x00ABCDEF;

        expect(cpu.getCPUFrequency()).andReturn(frequency).anyTimes();
        replay(cpu);

        GetCPUClockFrequency.INS.start(control);

        byte b0 = GetCPUClockFrequency.INS.read(control);
        byte b1 = GetCPUClockFrequency.INS.read(control);
        byte b2 = GetCPUClockFrequency.INS.read(control);
        byte b3 = GetCPUClockFrequency.INS.read(control);

        int result = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
        assertEquals(frequency, result);

        verify(cpu);
    }

    @Test
    public void testResetResetsPosition() {
        expect(cpu.getCPUFrequency()).andReturn(2000).anyTimes();
        replay(cpu);

        GetCPUClockFrequency.INS.start(control);
        GetCPUClockFrequency.INS.read(control); // read byte 0
        GetCPUClockFrequency.INS.read(control); // read byte 1

        GetCPUClockFrequency.INS.reset(control);
        GetCPUClockFrequency.INS.start(control);
        resetClearFlags();

        // Should start from byte 0 again
        byte b0 = GetCPUClockFrequency.INS.read(control);
        assertFalse(isCommandCleared());
        assertEquals((byte) (2000 & 0xFF), b0);
    }
}

