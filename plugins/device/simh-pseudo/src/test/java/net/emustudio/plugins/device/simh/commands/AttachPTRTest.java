/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class AttachPTRTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        AttachPTR.INS.start(control);
        assertTrue(isWriteCommandCleared());
        assertFalse(isCommandCleared());
    }

    @Test
    public void testReadReturnsZeroByDefault() {
        AttachPTR.INS.reset(control);
        byte result = AttachPTR.INS.read(control);
        assertEquals(0, result);
    }
}

