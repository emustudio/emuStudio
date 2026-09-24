/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DetachPTPTest extends CommandTestBase {

    @Test
    public void testStartClearsCommand() {
        paperTape.detachPunch();
        expectLastCall().once();
        replay(paperTape);
        DetachPTP.INS.start(control);
        assertTrue(isCommandCleared());
        verify(paperTape);
    }
}
