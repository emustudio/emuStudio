/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import java.nio.file.Paths;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AttachPTPTest extends CommandTestBase {

    @Test
    public void testStartAttachesPunchAndClearsWriteCommand() throws Exception {
        String path = "punch.pt";
        expectCommandLine(path);
        paperTape.attachPunch(Paths.get(path));
        expectLastCall().once();
        replay(memory, paperTape);

        AttachPTP.INS.start(control);
        assertTrue(isWriteCommandCleared());
        assertFalse(isCommandCleared());
        verify(memory, paperTape);
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
