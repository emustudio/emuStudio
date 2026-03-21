/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class AttachPTPTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        AttachPTP.INS.start(control);
        assertTrue(isWriteCommandCleared());
        assertFalse(isCommandCleared());
    }

    @Test
    public void testReadReturnsLastCPMStatus() {
        AttachPTP.INS.reset(control);
        byte result = AttachPTP.INS.read(control);
        assertEquals(0, result);
        assertTrue(isCommandCleared());
    }

    @Test
    public void testResetClearsStatus() {
        AttachPTP.INS.reset(control);
        byte result = AttachPTP.INS.read(control);
        assertEquals(0, result);
    }
}

