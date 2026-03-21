/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetSimhVersionTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        GetSimhVersion.INS.start(control);
        assertTrue(isWriteCommandCleared());
        assertFalse(isCommandCleared());
    }

    @Test
    public void testReturnsVersionString() {
        GetSimhVersion.INS.start(control);

        StringBuilder version = new StringBuilder();
        byte b;
        while ((b = GetSimhVersion.INS.read(control)) != 0) {
            version.append((char) b);
        }
        assertEquals("SIMH004", version.toString());
    }

    @Test
    public void testClearsCommandAfterLastByte() {
        GetSimhVersion.INS.start(control);
        resetClearFlags();

        // Read all bytes until null terminator
        while (GetSimhVersion.INS.read(control) != 0) {
            // keep reading
        }
        // After reading null terminator, command should be cleared
        assertTrue(isCommandCleared());
    }

    @Test
    public void testResetResetsPosition() {
        GetSimhVersion.INS.start(control);
        GetSimhVersion.INS.read(control); // read 'S'
        GetSimhVersion.INS.read(control); // read 'I'

        GetSimhVersion.INS.reset(control);
        GetSimhVersion.INS.start(control);

        byte first = GetSimhVersion.INS.read(control);
        assertEquals('S', (char) first);
    }
}

