/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class GetHostOSPathSeparatorTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        GetHostOSPathSeparator.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturnsPathSeparatorAndClearsCommand() {
        GetHostOSPathSeparator.INS.start(control);
        resetClearFlags();

        byte result = GetHostOSPathSeparator.INS.read(control);
        assertEquals((byte) File.separatorChar, result);
        assertTrue(isCommandCleared());
    }
}

