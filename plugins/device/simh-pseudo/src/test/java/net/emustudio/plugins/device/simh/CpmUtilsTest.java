/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CpmUtilsTest {
    private MemoryContext<Byte> memory;

    @Before
    public void setUp() {
        memory = createNiceMock(MemoryContext.class);
    }

    @Test
    public void testReadCPMCommandLineBasic() {
        // length at 0x80, first char discarded, command starts at 0x82
        expect(memory.read(0x80)).andReturn((byte) 6); // length=6, we read 6-1=5 chars
        expect(memory.read(0x82)).andReturn((byte) 'H');
        expect(memory.read(0x83)).andReturn((byte) 'E');
        expect(memory.read(0x84)).andReturn((byte) 'L');
        expect(memory.read(0x85)).andReturn((byte) 'L');
        expect(memory.read(0x86)).andReturn((byte) 'O');
        replay(memory);

        CpmUtils.readCPMCommandLine(memory);

        assertEquals('H', CpmUtils.cpmCommandLine[0]);
        assertEquals('E', CpmUtils.cpmCommandLine[1]);
        assertEquals('L', CpmUtils.cpmCommandLine[2]);
        assertEquals('L', CpmUtils.cpmCommandLine[3]);
        assertEquals('O', CpmUtils.cpmCommandLine[4]);
        assertEquals(0, CpmUtils.cpmCommandLine[5]); // null terminated
    }

    @Test
    public void testReadCPMCommandLineEmptyLength() {
        // length=0 means we read 0-1=-1 chars, so loop doesn't execute
        expect(memory.read(0x80)).andReturn((byte) 0);
        replay(memory);

        CpmUtils.readCPMCommandLine(memory);

        assertEquals(0, CpmUtils.cpmCommandLine[0]); // should be null terminated at index 0
    }

    @Test
    public void testReadCPMCommandLineSingleChar() {
        // length=2, read 2-1=1 char
        expect(memory.read(0x80)).andReturn((byte) 2);
        expect(memory.read(0x82)).andReturn((byte) 'A');
        replay(memory);

        CpmUtils.readCPMCommandLine(memory);

        assertEquals('A', CpmUtils.cpmCommandLine[0]);
        assertEquals(0, CpmUtils.cpmCommandLine[1]);
    }

    @Test
    public void testReadCPMCommandLineLengthMaskedTo7Bits() {
        // bit 7 should be masked (0x80 | 3 = 0x83 -> masked to 3)
        expect(memory.read(0x80)).andReturn((byte) 0x83); // 0x83 & 0x7F = 3
        expect(memory.read(0x82)).andReturn((byte) 'X');
        expect(memory.read(0x83)).andReturn((byte) 'Y');
        replay(memory);

        CpmUtils.readCPMCommandLine(memory);

        assertEquals('X', CpmUtils.cpmCommandLine[0]);
        assertEquals('Y', CpmUtils.cpmCommandLine[1]);
        assertEquals(0, CpmUtils.cpmCommandLine[2]);
    }
}
