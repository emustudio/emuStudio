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
    @Test
    public void inspectionFollowsNotificationsWithoutCreatingAnExecutionController() throws Exception {
        CPU cpu = mockCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        Set<Thread> before = commandThreads();
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null)) {
            session.requirePaused();
            session.executionChanged(CPU.RunState.STATE_STOPPED_BREAK, CPU.RunState.STATE_RUNNING);

            assertEquals(CPU.RunState.STATE_STOPPED_BREAK, session.cpuContext.getRunState());
            assertTrue(session.isRunning());
            assertThrows(IllegalStateException.class, session::requirePaused);

            session.executionChanged(CPU.RunState.STATE_RUNNING, CPU.RunState.STATE_STOPPED_BREAK);
            session.requirePaused();
            assertFalse(session.isRunning());
        }
        Set<Thread> after = commandThreads();
        after.removeAll(before);
        assertTrue(after.isEmpty());
    }

    @Test
    public void eachCaptureWaitsForItsOwnPublicationAndRejectsSupersededWork() throws Exception {
        CPU cpu = mockCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null)) {
            java.util.concurrent.CompletableFuture<Void> firstPublished = new java.util.concurrent.CompletableFuture<>();
            java.util.concurrent.CompletableFuture<Void> secondPublished = new java.util.concurrent.CompletableFuture<>();
            java.util.concurrent.atomic.AtomicReference<DebuggerSession.SourceCaptureGuard> first = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<DebuggerSession.SourceCaptureGuard> second = new java.util.concurrent.atomic.AtomicReference<>();
            session.runCompilation(() -> first.set(session.captureSourceGuard()), firstPublished);
            session.runCompilation(() -> second.set(session.captureSourceGuard()), secondPublished);

            firstPublished.complete(null);

            assertTrue(first.get().whenReady().isDone());
            assertFalse(second.get().whenReady().isDone());
            assertThrows(IllegalStateException.class,
                    () -> session.applySourceCapture(first.get(), () -> fail("Applied superseded compilation")));

            session.executionChanged(CPU.RunState.STATE_STOPPED_BREAK, CPU.RunState.STATE_STOPPED_BREAK);
            secondPublished.complete(null);

            assertTrue(second.get().whenReady().isDone());
            java.util.concurrent.atomic.AtomicBoolean applied = new java.util.concurrent.atomic.AtomicBoolean();
            session.applySourceCapture(second.get(), () -> applied.set(true));
            assertTrue(applied.get());
        }
    }

    @Test(timeout = 5000)
    public void closingDebuggerDuringAPausedMachineEditIsRejected() throws Exception {
        CPU cpu = mockCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null);
             net.emustudio.application.emulation.EmulationController execution = net.emustudio.application.TestCpu.execution(session)) {
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
            DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), memory, null);

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

        assertSame(failure, assertThrows(IllegalStateException.class, () -> new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), memory, null)));

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
