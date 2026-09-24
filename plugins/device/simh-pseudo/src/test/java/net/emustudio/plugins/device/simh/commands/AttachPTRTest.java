/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import java.nio.file.Paths;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AttachPTRTest extends CommandTestBase {

    @Test
    public void testStartAttachesReaderAndClearsWriteCommand() throws Exception {
        String path = "reader.pt";
        expectCommandLine(path);
        paperTape.attachReader(Paths.get(path));
        expectLastCall().once();
        replay(memory, paperTape);

        AttachPTR.INS.start(control);
        assertTrue(isWriteCommandCleared());
        assertFalse(isCommandCleared());
        verify(memory, paperTape);
    }

    @Test
    public void testReadReturnsZeroByDefault() {
        AttachPTR.INS.reset(control);
        byte result = AttachPTR.INS.read(control);
        assertEquals(0, result);
        assertTrue(isCommandCleared());
    }
}
