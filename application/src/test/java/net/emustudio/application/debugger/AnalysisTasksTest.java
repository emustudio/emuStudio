/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;

public class AnalysisTasksTest {
    @Test(timeout = 5000)
    public void slowCaptureLeavesEdtAvailableAndAppliesResultOnEdt() throws Exception {
        Fixture f = new Fixture();
        CountDownLatch done = new CountDownLatch(1), edtAvailable = new CountDownLatch(1);
        AtomicBoolean appliedOnEdt = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AnalysisTasks tasks = new AnalysisTasks(f.session, busy -> { if (!busy) done.countDown(); }, failure::set);
        try {
            SwingUtilities.invokeAndWait(() -> tasks.submit(f.session::captureMemory,
                    image -> appliedOnEdt.set(SwingUtilities.isEventDispatchThread())));
            assertTrue(f.readStarted.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeLater(edtAvailable::countDown);
            assertTrue(edtAvailable.await(1, TimeUnit.SECONDS));
            assertFalse(f.readOnEdt.get());
            f.releaseRead.countDown();
            assertTrue(done.await(1, TimeUnit.SECONDS));
            assertTrue(appliedOnEdt.get());
            assertNull(failure.get());
        } finally {
            f.releaseRead.countDown();
            SwingUtilities.invokeAndWait(tasks::close);
            f.session.close();
        }
    }

    @Test(timeout = 5000)
    public void resumeAndPauseDuringCaptureDiscardsResultAndStagedImage() throws Exception {
        Fixture f = new Fixture();
        f.blockRead.set(false);
        MemoryAnalysis original = f.session.image();
        f.blockRead.set(true);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean applied = new AtomicBoolean();
        AnalysisTasks tasks = new AnalysisTasks(f.session, busy -> { if (!busy) done.countDown(); }, failure::set);
        try {
            SwingUtilities.invokeAndWait(() -> tasks.submit(f.session::captureImage, captured -> {
                f.session.installImage(captured);
                applied.set(true);
            }));
            assertTrue(f.readStarted.await(1, TimeUnit.SECONDS));
            f.listener.get().runStateChanged(CPU.RunState.STATE_RUNNING);
            f.listener.get().runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            f.releaseRead.countDown();
            assertTrue(done.await(1, TimeUnit.SECONDS));
            assertFalse(applied.get());
            assertSame(original, f.session.image());
            assertTrue(failure.get().getMessage().contains("machine changed"));
        } finally {
            f.releaseRead.countDown();
            SwingUtilities.invokeAndWait(tasks::close);
            f.session.close();
        }
    }

    @Test
    public void mutationRejectsOldCaptureDuringAndAfterOperationWithoutCpuEvents() throws Exception {
        Fixture f = new Fixture();
        f.blockRead.set(false);
        try {
            DebugSession.ImageCapture before = f.session.captureImage();
            f.session.runMutation(() -> {
                assertThrows(IllegalStateException.class, f.session::pausedGeneration);
                assertThrows(IllegalStateException.class, () -> f.session.installImage(before));
            });
            assertThrows(IllegalStateException.class, () -> f.session.installImage(before));
            assertThrows(IllegalArgumentException.class,
                    () -> f.session.runMutation(() -> { throw new IllegalArgumentException("edit failed"); }));
            f.session.installImage(f.session.captureImage());
            assertEquals(8, f.session.image().size());
        } finally {
            f.session.close();
        }
    }

    @Test
    public void pausedCpuAndMemoryNotificationsInvalidateAnalysisAndListenersAreRemoved() throws Exception {
        Fixture f = new Fixture();
        try {
            long before = f.session.pausedGeneration();
            f.listener.get().runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            long stepped = f.session.pausedGeneration();
            assertNotEquals(before, stepped);
            f.listener.get().internalStateChanged();
            long edited = f.session.pausedGeneration();
            assertNotEquals(stepped, edited);
            f.memoryListener.get().memoryContentChanged(0, 1);
            long written = f.session.pausedGeneration();
            assertNotEquals(edited, written);
            f.memoryListener.get().memorySizeChanged();
            assertNotEquals(written, f.session.pausedGeneration());
            assertThrows(IllegalStateException.class, () -> f.session.applyPaused(before, () -> {}));
        } finally {
            f.session.close();
        }
        verify(f.memory).removeMemoryListener(f.memoryListener.get());
    }

    @Test(timeout = 5000)
    public void controllerResetInvalidatesBeforeMemoryChangesWithoutCpuNotification() throws Exception {
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.empty());
        List<CPU.CPUListener> listeners = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> { listeners.add(invocation.getArgument(0)); return null; })
                .when(cpu).addCPUListener(any());
        doAnswer(invocation -> {
            listeners.forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));
            return null;
        }).when(cpu).reset();
        MemoryContext<?> context = mock(MemoryContext.class);
        Memory memory = mock(Memory.class);
        AtomicBoolean rejected = new AtomicBoolean();
        try (DebugSession session = new DebugSession(cpu, context, null)) {
            doAnswer(invocation -> {
                assertThrows(IllegalStateException.class, session::pausedGeneration);
                assertThrows(IllegalStateException.class, session::captureMemory);
                verify(context, never()).readBank(anyInt(), anyInt());
                rejected.set(true);
                return null;
            }).when(memory).reset();
            try (EmulationController controller = new EmulationController(cpu, memory, List.of(), session)) {
                controller.reset();
            }
            assertTrue(rejected.get());
        }
    }

    @Test(timeout = 5000)
    public void cancellationKeepsJobBusyUntilProviderActuallyExits() throws Exception {
        Fixture f = new Fixture();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean applied = new AtomicBoolean();
        AnalysisTasks tasks = new AnalysisTasks(f.session, busy -> { if (!busy) done.countDown(); }, ignored -> {});
        try {
            SwingUtilities.invokeAndWait(() -> tasks.submit(f.session::captureMemory, image -> applied.set(true)));
            assertTrue(f.readStarted.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                tasks.cancel();
                assertTrue(tasks.isBusy());
                assertThrows(IllegalStateException.class, () -> tasks.submit(() -> 1, ignored -> {}));
            });
            f.releaseRead.countDown();
            assertTrue(done.await(1, TimeUnit.SECONDS));
            assertFalse(applied.get());
        } finally {
            f.releaseRead.countDown();
            SwingUtilities.invokeAndWait(tasks::close);
            f.session.close();
        }
    }

    @Test(timeout = 5000)
    public void disposingCancelsAndSuppressesLateCallbacks() throws Exception {
        Fixture f = new Fixture();
        AtomicBoolean applied = new AtomicBoolean(), reported = new AtomicBoolean();
        CountDownLatch operationExited = new CountDownLatch(1);
        AnalysisTasks tasks = new AnalysisTasks(f.session, ignored -> {}, ignored -> reported.set(true));
        try {
            SwingUtilities.invokeAndWait(() -> tasks.submit(() -> {
                try { return f.session.captureMemory(); }
                finally { operationExited.countDown(); }
            }, image -> applied.set(true)));
            assertTrue(f.readStarted.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(tasks::close);
            f.releaseRead.countDown();
            assertTrue(operationExited.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> assertFalse(tasks.isBusy()));
            assertFalse(applied.get());
            assertFalse(reported.get());
        } finally {
            f.releaseRead.countDown();
            f.session.close();
        }
    }

    @Test
    public void binaryReadChecksSizeAndBoundsActualReads() throws Exception {
        Path file = Files.createTempFile("debugger-binary", ".bin");
        try {
            Files.write(file, new byte[] {1, 2, 3});
            assertArrayEquals(new byte[] {1, 2, 3}, MemoryAnalysis.readBinary(file, 3));
            assertThrows(IOException.class, () -> MemoryAnalysis.readBinary(file, 2));
            assertThrows(IOException.class, () -> MemoryAnalysis.readBinary(file, 4));
            try (RandomAccessFile sparse = new RandomAccessFile(file.toFile(), "rw")) {
                sparse.setLength(1L << 32);
            }
            assertThrows(IOException.class, () -> MemoryAnalysis.readBinary(file, 3));
            assertThrows(IOException.class,
                    () -> MemoryAnalysis.readBinary(new ByteArrayInputStream(new byte[] {1}), 2));
            assertThrows(IOException.class,
                    () -> MemoryAnalysis.readBinary(new ByteArrayInputStream(new byte[] {1, 2, 3}), 2));
        } finally {
            Files.delete(file);
        }
    }

    private static final class Fixture {
        final AtomicReference<CPU.CPUListener> listener = new AtomicReference<>();
        final AtomicReference<MemoryContext.MemoryListener> memoryListener = new AtomicReference<>();
        final CountDownLatch readStarted = new CountDownLatch(1), releaseRead = new CountDownLatch(1);
        final AtomicBoolean blockRead = new AtomicBoolean(true), readOnEdt = new AtomicBoolean();
        final DebugSession session;
        final MemoryContext<Byte> memory;

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            CPU cpu = mock(CPU.class);
            when(cpu.getDebugger()).thenReturn(Optional.empty());
            doAnswer(invocation -> { listener.set(invocation.getArgument(0)); return null; })
                    .when(cpu).addCPUListener(any());
            memory = mock(MemoryContext.class);
            when(memory.getCellTypeClass()).thenReturn(Byte.class);
            when(memory.getSize()).thenReturn(8);
            when(memory.getBanksCount()).thenReturn(1);
            doAnswer(invocation -> { memoryListener.set(invocation.getArgument(0)); return null; })
                    .when(memory).addMemoryListener(any());
            when(memory.readBank(anyInt(), anyInt())).thenAnswer(invocation -> {
                if (blockRead.get()) {
                    readOnEdt.set(SwingUtilities.isEventDispatchThread());
                    readStarted.countDown();
                    boolean interrupted = false;
                    while (true) {
                        try { releaseRead.await(); break; }
                        catch (InterruptedException e) { interrupted = true; }
                    }
                    if (interrupted) Thread.currentThread().interrupt();
                }
                return (byte) 1;
            });
            session = new DebugSession(cpu, memory, null);
        }
    }
}
