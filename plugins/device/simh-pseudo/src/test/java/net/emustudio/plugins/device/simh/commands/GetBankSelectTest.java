/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GetBankSelectTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        GetBankSelect.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturnsSelectedBank() {
        expect(memory.getSelectedBank()).andReturn(3);
        replay(memory);

        GetBankSelect.INS.start(control);
        resetClearFlags();

        byte result = GetBankSelect.INS.read(control);
        assertEquals(3, result);
        assertTrue(isCommandCleared());

        verify(memory);
    }

    @Test
    public void testReturnsBank0ByDefault() {
        expect(memory.getSelectedBank()).andReturn(0);
        replay(memory);

        GetBankSelect.INS.start(control);
        resetClearFlags();

        byte result = GetBankSelect.INS.read(control);
        assertEquals(0, result);
        assertTrue(isCommandCleared());

        verify(memory);
    }
}

