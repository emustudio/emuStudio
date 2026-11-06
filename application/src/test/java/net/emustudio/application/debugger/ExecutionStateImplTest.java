/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.ExecutionStateImpl;
import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ExecutionStateImplTest {
    @Test
    public void currentInstructionTracksActiveCpu() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[1][32], 32, false);
        ExecutionStateImpl state = new ExecutionStateImpl();
        when(machine.cpu.getInstructionLocation()).thenReturn(4, 20);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            assertEquals(-1, state.currentInstruction());
            state.setSession(session);

            assertEquals(4, state.currentInstruction());
            assertEquals(20, state.currentInstruction());

            state.clearSession(session);
            assertEquals(-1, state.currentInstruction());
        }
    }

    @Test
    public void listenersDetachFromPreviousSession() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[1][32], 32, false);
        ExecutionStateImpl state = new ExecutionStateImpl();
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        state.addListener(listener);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            state.setSession(session);
            changes.set(0);

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(1, changes.get());

            state.clearSession(session);
            changes.set(0);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(0, changes.get());
        }
        state.removeListener(listener);
    }
}
