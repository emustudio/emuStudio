/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebuggerSessionLifecycleTest {
    @Test(timeout = 5000)
    public void closingDebuggerDuringAPausedMachineEditIsRejected() throws Exception {
        CPU cpu = mockCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try (DebuggerSession session = new DebuggerSession(net.emustudio.application.TestCpu.execution(cpu), cpu, null, null);
             net.emustudio.application.emulation.EmulationController execution = session.execution()) {
            java.util.concurrent.CompletableFuture<Void> edit = execution.mutate(() -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                }
            });
            try {
                assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals(CPU.RunState.STATE_STOPPED_BREAK, execution.getRunState());
                assertThrows(IllegalStateException.class, session::close);
                session.requirePaused();
            } finally {
                release.countDown();
            }
            edit.join();
        }
    }

    @Test
    public void closingDebuggerTwiceReleasesItsResourcesWithoutClosingExecution() throws Exception {
        CPU cpu = mockCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        MemoryContext<?> memory = mock(MemoryContext.class);
        try (net.emustudio.application.emulation.EmulationController execution = net.emustudio.application.TestCpu.execution(cpu)) {
            DebuggerSession session = new DebuggerSession(execution, cpu, memory, null);

            session.close();
            session.close();

            assertFalse("Only the machine owner may close execution", execution.isBusy());
            verify(memory, times(1)).removeMemoryListener(any());
            assertThrows(IllegalStateException.class, session::requirePaused);
        }
    }

    @Test
    public void failedMemoryListenerRegistrationDoesNotStartAnOrphanScheduler() {
        CPU cpu = mockCpu();
        MemoryContext<?> memory = mock(MemoryContext.class);
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        IllegalStateException failure = new IllegalStateException("Memory listener rejected");
        doThrow(failure).when(memory).addMemoryListener(any());
        Set<Thread> before = commandThreads();

        assertSame(failure, assertThrows(IllegalStateException.class, () -> new DebuggerSession(net.emustudio.application.TestCpu.execution(cpu), cpu, memory, null)));

        Set<Thread> after = commandThreads();
        after.removeAll(before);
        assertTrue("Failed initialization must not create a live command scheduler", after.isEmpty());
        verify(net.emustudio.application.TestCpu.context(cpu), never()).getCpuFrequencyKhz();
    }

    private static Set<Thread> commandThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isAlive() && thread.getName().equals("machine-commands"))
                .collect(Collectors.toSet());
    }
}
