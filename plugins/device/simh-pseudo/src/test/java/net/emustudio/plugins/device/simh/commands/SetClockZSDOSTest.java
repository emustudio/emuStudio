/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SetClockZSDOSTest extends CommandTestBase {

    @Before
    public void setUp() {
        SetClockZSDOS.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        SetClockZSDOS.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteTwoByteAddress() {
        // Address is sent as 2 bytes (low, high)
        // The address points to a 6-byte BCD block: YY MM DD HH MM SS
        int address = 0x1234;

        // Set up memory at address to have date bytes
        // Year=26 (2026), Month=3, Day=21, Hour=12, Min=30, Sec=45
        expect(memory.read(address)).andReturn((byte) bin2bcd(26)).anyTimes();
        expect(memory.read(address + 1)).andReturn((byte) bin2bcd(3)).anyTimes();
        expect(memory.read(address + 2)).andReturn((byte) bin2bcd(21)).anyTimes();
        expect(memory.read(address + 3)).andReturn((byte) bin2bcd(12)).anyTimes();
        expect(memory.read(address + 4)).andReturn((byte) bin2bcd(30)).anyTimes();
        expect(memory.read(address + 5)).andReturn((byte) bin2bcd(45)).anyTimes();
        replay(memory);

        SetClockZSDOS.INS.start(control);
        resetClearFlags();

        // Write low byte of address
        SetClockZSDOS.INS.write((byte) (address & 0xFF), control);
        assertFalse(isCommandCleared());

        // Write high byte of address
        SetClockZSDOS.INS.write((byte) ((address >> 8) & 0xFF), control);
        assertTrue(isCommandCleared());

        verify(memory);
    }

    @Test
    public void testResetClearsDelta() {
        SetClockZSDOS.INS.ClockZSDOSDelta = 100;
        SetClockZSDOS.INS.reset(control);
        assertEquals(0, SetClockZSDOS.INS.ClockZSDOSDelta);
    }
}

