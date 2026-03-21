/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SetTimerInterruptAdrTest extends CommandTestBase {

    @Before
    public void setUp() {
        SetTimerInterruptAdr.INS.timerInterruptHandler = 0xFC00; // restore default
        SetTimerInterruptAdr.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        SetTimerInterruptAdr.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteTwoByteAddress() {
        int address = 0xFC00;

        SetTimerInterruptAdr.INS.start(control);
        resetClearFlags();

        // Write low byte
        SetTimerInterruptAdr.INS.write((byte) (address & 0xFF), control);
        assertFalse(isCommandCleared());

        // Write high byte
        SetTimerInterruptAdr.INS.write((byte) ((address >> 8) & 0xFF), control);
        assertTrue(isCommandCleared());

        assertEquals(address, SetTimerInterruptAdr.INS.timerInterruptHandler);
    }

    @Test
    public void testWriteAddressWithHighBitsSet() {
        // Test that sign extension is handled correctly
        int address = 0xFF80;

        SetTimerInterruptAdr.INS.start(control);
        resetClearFlags();

        SetTimerInterruptAdr.INS.write((byte) (address & 0xFF), control);
        SetTimerInterruptAdr.INS.write((byte) ((address >> 8) & 0xFF), control);

        assertEquals(address, SetTimerInterruptAdr.INS.timerInterruptHandler);
    }

    @Test
    public void testResetResetsPosition() {
        SetTimerInterruptAdr.INS.start(control);
        SetTimerInterruptAdr.INS.write((byte) 0x10, control);

        SetTimerInterruptAdr.INS.reset(control);
        SetTimerInterruptAdr.INS.start(control);
        resetClearFlags();

        // Should start from low byte again
        SetTimerInterruptAdr.INS.write((byte) 0x00, control);
        assertFalse(isCommandCleared());
        SetTimerInterruptAdr.INS.write((byte) 0x10, control);
        assertTrue(isCommandCleared());

        assertEquals(0x1000, SetTimerInterruptAdr.INS.timerInterruptHandler);
    }
}

