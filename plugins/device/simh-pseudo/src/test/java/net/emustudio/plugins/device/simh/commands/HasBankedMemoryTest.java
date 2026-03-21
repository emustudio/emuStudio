/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class HasBankedMemoryTest extends CommandTestBase {

    @Test
    public void testStartClearsWriteCommand() {
        HasBankedMemory.INS.start(control);
        assertTrue(isWriteCommandCleared());
    }

    @Test
    public void testReturnsBanksCount() {
        expect(memory.getBanksCount()).andReturn(4);
        replay(memory);

        HasBankedMemory.INS.start(control);
        resetClearFlags();

        byte result = HasBankedMemory.INS.read(control);
        assertEquals(4, result);
        assertTrue(isCommandCleared());

        verify(memory);
    }

    @Test
    public void testReturnsSingleBank() {
        expect(memory.getBanksCount()).andReturn(1);
        replay(memory);

        HasBankedMemory.INS.start(control);
        resetClearFlags();

        byte result = HasBankedMemory.INS.read(control);
        assertEquals(1, result);
        assertTrue(isCommandCleared());

        verify(memory);
    }
}

