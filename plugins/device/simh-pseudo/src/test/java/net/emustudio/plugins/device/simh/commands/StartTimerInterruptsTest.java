/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class StartTimerInterruptsTest extends CommandTestBase {

    @Before
    public void setUp() {
        replay(cpu);
        StartTimerInterrupts.INS.reset(control);
        reset(cpu);
    }

    @Test
    public void testStartClearsCommand() {
        cpu.addPassedCyclesListener(anyObject());
        expectLastCall();
        replay(cpu);

        StartTimerInterrupts.INS.start(control);
        assertTrue(isCommandCleared());
        assertNotNull(StartTimerInterrupts.INS.callback.get());

        verify(cpu);
    }

    @Test
    public void testResetRemovesCallback() {
        cpu.addPassedCyclesListener(anyObject());
        expectLastCall();
        cpu.removePassedCyclesListener(anyObject());
        expectLastCall();
        replay(cpu);

        StartTimerInterrupts.INS.start(control);
        assertNotNull(StartTimerInterrupts.INS.callback.get());

        StartTimerInterrupts.INS.reset(control);
        assertNull(StartTimerInterrupts.INS.callback.get());

        verify(cpu);
    }
}

