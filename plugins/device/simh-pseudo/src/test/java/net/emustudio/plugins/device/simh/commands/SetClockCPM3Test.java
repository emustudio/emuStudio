/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SetClockCPM3Test extends CommandTestBase {

    @Before
    public void setUp() {
        SetClockCPM3.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        SetClockCPM3.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteTwoByteAddress() {
        int address = 0x2000;

        // Set up memory: 5-byte block: days_low, days_high, HH(BCD), MM(BCD), SS(BCD)
        // Days since 31 Dec 1977 = e.g. 100 days = 0x0064
        expect(memory.read(address)).andReturn((byte) 0x64).anyTimes();      // days low
        expect(memory.read(address + 1)).andReturn((byte) 0x00).anyTimes();  // days high
        expect(memory.read(address + 2)).andReturn((byte) bin2bcd(10)).anyTimes();  // 10:00:00
        expect(memory.read(address + 3)).andReturn((byte) bin2bcd(0)).anyTimes();
        expect(memory.read(address + 4)).andReturn((byte) bin2bcd(0)).anyTimes();
        replay(memory);

        SetClockCPM3.INS.start(control);
        resetClearFlags();

        // Write low byte of address
        SetClockCPM3.INS.write((byte) (address & 0xFF), control);
        assertFalse(isCommandCleared());

        // Write high byte of address
        SetClockCPM3.INS.write((byte) ((address >> 8) & 0xFF), control);
        assertTrue(isCommandCleared());

        verify(memory);
    }

    @Test
    public void testResetClearsDelta() {
        SetClockCPM3.INS.ClockCPM3Delta = 100;
        SetClockCPM3.INS.reset(control);
        assertEquals(0, SetClockCPM3.INS.ClockCPM3Delta);
    }
}

