/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SetCPUClockFrequencyTest extends CommandTestBase {

    @Before
    public void setUp() {
        SetCPUClockFrequency.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        SetCPUClockFrequency.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWrite4ByteFrequency() {
        int frequency = 4000; // 4000 kHz

        cpu.setCPUFrequency(frequency);
        expectLastCall();
        replay(cpu);

        SetCPUClockFrequency.INS.start(control);
        resetClearFlags();

        // Write 4 bytes (32-bit little-endian)
        SetCPUClockFrequency.INS.write((byte) (frequency & 0xFF), control);
        assertFalse(isCommandCleared());
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 8) & 0xFF), control);
        assertFalse(isCommandCleared());
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 16) & 0xFF), control);
        assertFalse(isCommandCleared());
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 24) & 0xFF), control);
        assertTrue(isCommandCleared());

        verify(cpu);
    }

    @Test
    public void testWriteLargeFrequency() {
        int frequency = 0x00ABCDEF;

        cpu.setCPUFrequency(frequency);
        expectLastCall();
        replay(cpu);

        SetCPUClockFrequency.INS.start(control);
        resetClearFlags();

        SetCPUClockFrequency.INS.write((byte) (frequency & 0xFF), control);
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 8) & 0xFF), control);
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 16) & 0xFF), control);
        SetCPUClockFrequency.INS.write((byte) ((frequency >> 24) & 0xFF), control);

        verify(cpu);
    }

    @Test
    public void testResetResetsPosition() {
        SetCPUClockFrequency.INS.start(control);
        SetCPUClockFrequency.INS.write((byte) 0x01, control);

        SetCPUClockFrequency.INS.reset(control);
        SetCPUClockFrequency.INS.start(control);
        resetClearFlags();

        // Should accept low byte again
        SetCPUClockFrequency.INS.write((byte) 0x10, control);
        assertFalse(isCommandCleared());
    }
}

