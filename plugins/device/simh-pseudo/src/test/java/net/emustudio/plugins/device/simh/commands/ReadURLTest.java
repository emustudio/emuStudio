/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadURLTest extends CommandTestBase {

    @Before
    public void setUp() {
        ReadURL.INS.reset(control);
    }

    @Test
    public void testStartResetsState() {
        ReadURL.INS.start(control);
        // After start, reading should return 0 (not in read phase)
        byte result = ReadURL.INS.read(control);
        assertEquals(0, result);
    }

    @Test
    public void testResetResetsState() {
        ReadURL.INS.reset(control);
        byte result = ReadURL.INS.read(control);
        assertEquals(0, result);
    }

    @Test
    public void testWriteNullTerminatorTriggersURLRead() {
        ReadURL.INS.start(control);

        // Write URL characters (invalid URL to test error handling)
        ReadURL.INS.write((byte) 'x', control);
        // Write null terminator to finish URL
        ReadURL.INS.write((byte) 0, control);

        // Now we're in read phase. First read returns availability (1 or 0)
        byte avail = ReadURL.INS.read(control);
        // Should have some content (error message at minimum)
        assertEquals(1, avail);
    }

    @Test
    public void testWriteInReadPhaseClearsCommand() {
        ReadURL.INS.start(control);
        ReadURL.INS.write((byte) 'x', control);
        ReadURL.INS.write((byte) 0, control); // trigger read

        resetClearFlags();
        // Writing when in read phase should clear command
        ReadURL.INS.write((byte) 'a', control);
        assertTrue(isCommandCleared());
    }
}

