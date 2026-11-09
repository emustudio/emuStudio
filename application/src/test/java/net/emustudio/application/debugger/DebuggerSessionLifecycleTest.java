/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebuggerSessionLifecycleTest {
    @Test
    public void failedMemoryListenerRegistrationDoesNotStartAnOrphanScheduler() {
        CPU cpu = mock(CPU.class);
        MemoryContext<?> memory = mock(MemoryContext.class);
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        IllegalStateException failure = new IllegalStateException("Memory listener rejected");
        doThrow(failure).when(memory).addMemoryListener(any());
        Set<Thread> before = commandThreads();

        assertSame(failure, assertThrows(IllegalStateException.class, () -> new DebuggerSession(cpu, memory, null)));

        Set<Thread> after = commandThreads();
        after.removeAll(before);
        assertTrue("Failed initialization must not create a live command scheduler", after.isEmpty());
        verify(cpu, never()).sampleFrequency();
    }

    private static Set<Thread> commandThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isAlive() && thread.getName().equals("machine-commands"))
                .collect(Collectors.toSet());
    }
}
