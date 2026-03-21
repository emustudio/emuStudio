/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SetBankSelectTest extends CommandTestBase {

    @Test
    public void testStartClearsReadCommand() {
        SetBankSelect.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteSelectsBank() {
        memory.selectBank(5);
        expectLastCall();
        replay(memory);

        SetBankSelect.INS.start(control);
        resetClearFlags();

        SetBankSelect.INS.write((byte) 5, control);
        assertTrue(isCommandCleared());

        verify(memory);
    }
}

