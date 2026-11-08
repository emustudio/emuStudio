/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.debugger.StepView;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebugSessionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void cpuWithoutExecutionHooksRejectsBreakpointChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = machine.session(null)) {
            IllegalStateException rejected = assertThrows(IllegalStateException.class,
                    () -> session.setBreakpoint(1, true));

            assertEquals("Breakpoints unavailable for this CPU", rejected.getMessage());
            assertTrue(session.breakpoints.isEmpty());
            assertFalse(session.programIndex().snapshot().hasExecutionBreakpoint(1));
        }
    }

    @Test
    public void pauseAtBreakpointAddressDoesNotInventAnExecutionHit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(machine.cpu.getInstructionLocation()).thenReturn(1);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();

            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void compilationWithoutWritesPreservesMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebugSession session = machine.session(null)) {
            long before = session.programIndex().snapshot().memoryRevision();

            session.runCompilation(() -> {});

            assertEquals(before, session.programIndex().snapshot().memoryRevision());
        }
    }

    @Test
    public void compilationWritesInvalidateMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebugSession session = machine.session(null)) {
            long before = session.programIndex().snapshot().memoryRevision();

            session.runCompilation(() -> machine.memoryChanged(1));

            assertTrue(session.programIndex().snapshot().memoryRevision() > before);
        }
    }

    @Test
    public void compilationRestoresDisabledNotificationsAfterFailure() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        when(machine.memory.areMemoryNotificationsEnabled()).thenReturn(false);
        try (DebugSession session = machine.session(null)) {
            IllegalStateException failure = new IllegalStateException("compile failed");

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> session.runCompilation(() -> { throw failure; })));

            org.mockito.InOrder notifications = inOrder(machine.memory);
            notifications.verify(machine.memory).setMemoryNotificationsEnabled(true);
            notifications.verify(machine.memory).setMemoryNotificationsEnabled(false);
        }
    }

    @Test
    public void uncapturedSourceMappingIsNotUsedAfterMemoryChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebugSession session = machine.session(null)) {
            SourceCodePosition position = SourceCodePosition.of(1, 0, "program.asm");
            session.annotationEngine.put(new MemoryAnnotations.Entry(7, 0, 1, 2, "source", "", position));
            assertEquals(Optional.of(position), session.sourceAt(1));

            machine.memoryChanged(1);

            assertTrue(session.sourceAt(1).isEmpty());
            assertNotNull(session.annotationEngine.get(7, 0));
        }
    }

    @Test
    public void executionInvalidatesUncapturedSourceEvenWithoutMemoryNotifications() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebugSession session = machine.session(null)) {
            SourceCodePosition position = SourceCodePosition.of(1, 0, "program.asm");
            session.annotationEngine.put(new MemoryAnnotations.Entry(7, 0, 1, 2, "source", "", position));
            assertEquals(Optional.of(position), session.sourceAt(1));

            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertTrue(session.sourceAt(1).isEmpty());
            assertNotNull(session.annotationEngine.get(7, 0));
        }
    }

    @Test
    public void memorylessPortableCpuUsesIndexedAddressBreakpointsAndResumeSkipping() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebugSession session = machine.session(null, false)) {
            assertNull(session.memory);
            ProgramIndex index = session.programIndex();
            assertEquals(32, index.snapshot().layout().addressSpaceSize());
            session.setBreakpoint(7, true).join();
            assertTrue(index.snapshot().hasExecutionBreakpoint(7));
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(machine.beforeStep(6));
            assertTrue(machine.beforeStep(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(7, hit.address);
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(machine.beforeStep(7));
            assertTrue(machine.beforeStep(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(7, false).join();
            assertFalse(index.snapshot().hasExecutionBreakpoint(7));
            assertThrows(IllegalStateException.class, session::liveMemory);
        }
        verifyNoInteractions(machine.memory);
    }

    @Test
    public void executionUsesApplicationBreakpointStore() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();
            machine.state(CPU.RunState.STATE_RUNNING);
            assertTrue(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(1, false).join();
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void deferredSourceCaptureBelongsToCompletedCompileMutationOnly() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(null)) {
            java.util.concurrent.atomic.AtomicReference<DebugSession.SourceCaptureGuard> guard = new java.util.concurrent.atomic.AtomicReference<>();
            session.runMutation(() -> {
                guard.set(session.captureSourceGuard());
                assertThrows(IllegalStateException.class, () -> session.applySourceCapture(guard.get(), () -> fail("unfinished")));
                machine.memoryChanged(0);
            });
            AtomicBoolean applied = new AtomicBoolean();
            session.applySourceCapture(guard.get(), () -> applied.set(true));
            assertTrue(applied.get());
            session.runMutation(() -> {});
            assertThrows(IllegalStateException.class, () -> session.applySourceCapture(guard.get(), () -> fail("stale")));
        }
    }

    @Test
    public void executionInvalidatesFlowEvenWithoutMemoryNotificationsAndOldSnapshotStaysImmutable() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(null)) {
            ProgramIndex index = session.programIndex();
            try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
                update.replaceFlow(false).putInstruction(new ProgramIndex.Range(0, 2), ProgramIndex.InstructionKind.LINEAR);
                update.commit();
            }
            ProgramIndex.Snapshot old = index.snapshot();
            session.runExecution(() -> machine.state(CPU.RunState.STATE_RUNNING));
            assertEquals(ProgramIndex.FlowState.STALE, index.snapshot().flowState());
            assertNull(index.snapshot().instructionAt(0));
            assertEquals(ProgramIndex.FlowState.CURRENT, old.flowState());
            assertNotNull(old.instructionAt(0));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            ProgramIndex.Snapshot beforeWrite = index.snapshot();
            machine.memoryChanged(1);
            assertTrue(index.snapshot().memoryRevision() > beforeWrite.memoryRevision());
        }
    }

    @Test
    public void annotationsUseSharedMetadataIndexWithoutReadingMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebugSession session = machine.session(null)) {
            ProgramIndex initial = session.programIndex();
            assertSame(initial, session.annotations().programIndex());
            session.annotations().add(new AnnotationIndex.Entry(1, "user", "DATA"));
            machine.memoryChanged(1);
            assertEquals("user", session.annotations().get(1).label);
            assertSame(initial, session.programIndex());
            verify(machine.memory, never()).read(anyInt());
            verify(machine.memory, never()).read(anyInt());
        }
    }

    @Test
    public void liveCellReadDoesNotHoldSessionMonitorAndRejectsAnObservedWrite() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (DebugSession session = machine.session(null)) {
            LiveMemory live = session.liveMemory();
            when(machine.memory.read(0)).thenAnswer(call -> {
                entered.countDown(); await(release); return (byte) 1;
            });
            Future<Integer> read = executor.submit(() -> live.value(0));
            await(entered);
            executor.submit(session::requirePaused).get(2, TimeUnit.SECONDS);
            machine.memoryChanged(0);
            release.countDown();
            ExecutionException rejected = assertThrows(ExecutionException.class, () -> read.get(2, TimeUnit.SECONDS));
            assertTrue(rejected.getCause() instanceof IllegalStateException);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    public void liveFlowDecodeDoesNotHoldSessionMonitorAndRejectsLateMetadata() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        net.emustudio.emulib.plugins.cpu.Disassembler decoder = mock(net.emustudio.emulib.plugins.cpu.Disassembler.class);
        when(machine.cpu.getDisassembler()).thenReturn(decoder);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(decoder.getFlow(0)).thenAnswer(call -> {
            entered.countDown(); await(release);
            return Optional.of(new net.emustudio.emulib.plugins.cpu.InstructionFlow(
                    net.emustudio.emulib.plugins.cpu.InstructionFlow.Kind.STOP, 1, false, List.of()));
        });
        when(decoder.getNextInstructionPosition(0)).thenReturn(1);
        when(decoder.disassemble(0)).thenReturn(new net.emustudio.emulib.plugins.cpu.DisassembledInstruction(0, "STOP", "00"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (DebugSession session = machine.session(null)) {
            Future<FlowAnalysis> result = executor.submit(() -> session.analyzeFlow(0, 10));
            await(entered);
            executor.submit(session::requirePaused).get(2, TimeUnit.SECONDS);
            session.annotations().add(new AnnotationIndex.Entry(1, "new", "DATA"));
            release.countDown();
            ExecutionException rejected = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
            assertTrue(rejected.getCause() instanceof IllegalStateException);
            assertEquals(ProgramIndex.FlowState.NOT_BUILT, session.programIndex().snapshot().flowState());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    public void flowUsesCpuWiredDecoderAndLiveOperands() throws Exception {
        byte[][] cells = {{1, 0}, {7, 0}};
        DebuggerTestMachine machine = new DebuggerTestMachine(cells, 1);
        Disassembler decoder = mock(Disassembler.class);
        when(machine.cpu.getDisassembler()).thenReturn(decoder);
        stopAtZero(decoder);
        when(decoder.disassemble(0)).thenAnswer(call -> new DisassembledInstruction(0,
                "VALUE " + cells[1][0], ""));

        try (DebugSession session = machine.session(null)) {
            assertEquals("VALUE 7", session.analyzeFlow(0, 10).mnemonic(0));

            cells[1][0] = 8;
            machine.memoryChanged(0);

            assertEquals("VALUE 8", session.analyzeFlow(0, 10).mnemonic(0));
        }
    }

    @Test
    public void lateFlowCannotPublishAfterMemoryExecutionOrDisposalChanges() throws Exception {
        for (int change = 0; change < 3; change++) {
            DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
            Disassembler decoder = mock(Disassembler.class);
            when(machine.cpu.getDisassembler()).thenReturn(decoder);
            stopAtZero(decoder);
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            when(decoder.getFlow(0)).thenAnswer(call -> {
                entered.countDown(); await(release);
                return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of()));
            });
            ExecutorService executor = Executors.newSingleThreadExecutor();
            DebugSession session = machine.session(null);
            boolean disposed = false;
            try {
                ProgramIndex index = session.programIndex();
                Future<FlowAnalysis> result = executor.submit(() -> session.analyzeFlow(0, 10));
                await(entered);
                if (change == 0) machine.memoryChanged(1);
                else if (change == 1) {
                    machine.state(CPU.RunState.STATE_RUNNING);
                    machine.state(CPU.RunState.STATE_STOPPED_BREAK);
                } else {
                    session.close();
                    disposed = true;
                }
                release.countDown();
                ExecutionException rejected = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
                assertTrue(rejected.getCause() instanceof IllegalStateException);
                assertNull(index.snapshot().instructionAt(0));
                verify(decoder, never()).getNextInstructionPosition(anyInt());
                verify(decoder, never()).disassemble(anyInt());
            } finally {
                release.countDown();
                if (!disposed) session.close();
                executor.shutdownNow();
            }
        }
    }

    private static void stopAtZero(Disassembler decoder) throws Exception {
        when(decoder.getFlow(0)).thenReturn(Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of())));
        when(decoder.getNextInstructionPosition(0)).thenReturn(1);
        when(decoder.disassemble(0)).thenReturn(new DisassembledInstruction(0, "STOP", "00"));
    }

    @Test
    public void decoderRejectsNotifiedMappingChangesAndInterruptedWorkBeforeAnotherRead() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {1, 0}}, 1);
        Disassembler decoder = mock(Disassembler.class);
        when(machine.cpu.getDisassembler()).thenReturn(decoder);
        stopAtZero(decoder);
        when(decoder.getFlow(0)).thenAnswer(call -> {
            machine.memoryChanged(0);
            return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of()));
        });
        try (DebugSession session = machine.session(null)) {
            IllegalStateException changed = assertThrows(IllegalStateException.class,
                    () -> session.analyzeFlow(0, 10));
            assertTrue(changed.getMessage().contains("changed"));
            verify(decoder, never()).getNextInstructionPosition(anyInt());
            verify(decoder, never()).disassemble(anyInt());
            Thread.currentThread().interrupt();
            try {
                assertThrows(java.util.concurrent.CancellationException.class,
                        () -> session.analyzeFlow(0, 10));
            } finally { Thread.interrupted(); }
            assertEquals(ProgramIndex.FlowState.STALE, session.programIndex().snapshot().flowState());
        }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue("Barrier timed out", latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @Test
    public void runningSessionRejectsPausedOperationsAndResumesAfterStop() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebugSession session = machine.session(null)) {
            machine.state(CPU.RunState.STATE_RUNNING);
            assertThrows(IllegalStateException.class, session::requirePaused);
            assertThrows(IllegalStateException.class, session::liveMemory);
            session.update(() -> session.record(2)).join();
            assertTrue(session.isRecording());

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(1, session.liveMemory().value(0));
            assertEquals(2, session.liveMemory().value(1));
        }
    }

    @Test
    public void retainedLiveReaderRejectsMemoryMetadataGenerationAndDisposalChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        DebugSession session = machine.session(null);
        boolean disposed = false;
        try {
            LiveMemory first = session.liveMemory();
            machine.memoryChanged(1);
            assertThrows(IllegalStateException.class, () -> first.value(0));
            LiveMemory second = session.liveMemory();
            session.annotations().add(new AnnotationIndex.Entry(1, "changed", "UNKNOWN"));
            assertThrows(IllegalStateException.class, second::requireCurrent);
            LiveMemory third = session.liveMemory();
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertThrows(IllegalStateException.class, third::requireCurrent);
            LiveMemory fourth = session.liveMemory();
            session.close();
            disposed = true;
            assertThrows(IllegalStateException.class, fourth::requireCurrent);
        } finally { if (!disposed) session.close(); }
    }

    @Test
    public void resumeAndPauseDiscardAnEarlierPausedGeneration() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            long generation = session.pausedGeneration();
            AtomicBoolean applied = new AtomicBoolean();
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertThrows(IllegalStateException.class,
                    () -> session.applyPaused(generation, () -> applied.set(true)));
            assertFalse(applied.get());
        }
    }

    @Test
    public void failedMutationStillInvalidatesAnalysisAndReleasesMutationGuard() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            long generation = session.pausedGeneration();
            assertThrows(IllegalArgumentException.class, () -> session.runMutation(() -> {
                assertThrows(IllegalStateException.class, session::pausedGeneration);
                throw new IllegalArgumentException("edit failed");
            }));

            assertThrows(IllegalStateException.class, () -> session.applyPaused(generation, () -> {}));
            session.applyPaused(session.pausedGeneration(), () -> {});
        }
    }

    @Test
    public void closePausesRunningCpuAndDetachesOwnedListeners() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        DebugSession session = machine.session(null);
        session.update(() -> session.record(2)).join();
        machine.state(CPU.RunState.STATE_RUNNING);

        session.close();

        assertTrue(machine.detached.get() > 0);
        assertTrue(session.execution().closeAsync().isDone());
        assertTrue(machine.memoryListeners.isEmpty());
        assertThrows(IllegalStateException.class, session::requirePaused);
    }

    @Test
    public void memoryWatchRecordsLastAccessWithoutStopping() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0x3E, 0x11}}, 2);
        try (DebugSession session = machine.session(null)) {
            session.addMemoryWatch(1).join();
            verify(machine.debug).observe(eq(session), anyBoolean());

            assertFalse(machine.beforeStep(0));
            machine.accessed(0, true);
            assertFalse(session.afterStep(true, Optional.empty()));
            assertFalse(machine.beforeStep(0));
            machine.accessed(1, false);
            assertFalse(session.afterStep(true, Optional.empty()));
            session.refreshWatches().join();

            DebugSession.MemoryWatchView view = session.memoryWatchValues().get(0);
            assertEquals(1, view.address);
            assertEquals("17", view.value);
            assertEquals(0, view.lastReadPc);
            assertEquals("17", view.lastReadValue);
            assertEquals(-1, view.lastWritePc);
            assertEquals("", view.error);
            assertNull(session.takeBreakpointHit());

            assertFalse(machine.beforeStep(1));
            machine.accessed(1, true);
            assertFalse(session.afterStep(true, Optional.empty()));
            session.refreshWatches().join();
            view = session.memoryWatchValues().get(0);
            assertEquals(1, view.lastWritePc);
            assertEquals(0, view.lastReadPc);

            session.removeMemoryWatch(1).join();
            assertTrue(session.memoryWatchValues().isEmpty());
            assertTrue(machine.detached.get() > 0);
        }
    }

    @Test
    public void memoryWatchesAreUnavailableWithoutCpuDebugging() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = machine.session(null)) {
            assertThrows(IllegalStateException.class, () -> session.addMemoryWatch(0));
            assertTrue(session.memoryWatches.isEmpty());
        }
    }

    @Test
    public void boundedHistoryKeepsNewestInstructionsAndDisablingStopsCapture() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        StepView first = instruction(1);
        StepView second = instruction(2);
        StepView third = instruction(3);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.record(2)).join();
            capture(session, first);
            capture(session, second);
            capture(session, third);
            assertEquals(List.of(second, third), views(session));
            assertThrows(UnsupportedOperationException.class, () -> session.history().clear());

            session.update(() -> session.record(0)).join();
            capture(session, instruction(4));
            assertFalse(session.isRecording());
            assertEquals(List.of(second, third), views(session));
            verify(machine.debug, atLeastOnce()).observe(eq(session), anyBoolean());
            assertTrue(machine.detached.get() > 0);
        }
    }

    @Test
    public void historyRequiresDebuggerCapabilityAndValidLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = machine.session(null)) {
            assertThrows(IllegalStateException.class, () -> session.record(1));
            assertThrows(IllegalArgumentException.class, () -> session.record(-1));
            assertThrows(IllegalStateException.class,
                    () -> session.saveHistory(temporary.getRoot().toPath().resolve("unsupported.emutrace"), false));
            assertFalse(session.isRecording());
        }
    }

    @Test
    public void newRecordingClearsPreviousCaptureAfterLimitValidation() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.record(2)).join();
            capture(session, instruction(1));
            List<InstructionRecording.Event> previous = session.history();

            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(0, DebugSession.RecordingMode.LAST, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(1_000_001, DebugSession.RecordingMode.LAST, 0, 0));
            assertEquals(previous, session.history());
            session.update(() -> session.startNewRecording(1, DebugSession.RecordingMode.LAST, 0, 0)).join();
            capture(session, instruction(2));
            capture(session, instruction(3));

            assertEquals(1, previous.size());
            assertEquals(1, session.history().size());
            assertEquals(3, session.history().get(0).sequence);
        }
    }

    @Test
    public void firstInstructionsRemainWhenLaterExecutionExceedsLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        StepView first = instruction(1), second = instruction(2);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(2, DebugSession.RecordingMode.FIRST, 0, 0)).join();

            capture(session, first);
            capture(session, second);
            capture(session, instruction(3));

            assertEquals(List.of(first, second), views(session));
            assertTrue(session.isRecording());
            session.update(() -> session.record(0)).join();
            capture(session, instruction(4));
            assertEquals(List.of(first, second), views(session));
        }
    }

    @Test
    public void addressRangeRetainsAllMatchesIncludingBothBounds() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[8]}, 8);
        StepView from = addressInstruction(2, 3), through = addressInstruction(3, 5),
                latest = addressInstruction(4, 4);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(2, DebugSession.RecordingMode.ADDRESS_RANGE, 3, 5)).join();

            capture(session, addressInstruction(1, 2));
            capture(session, from);
            capture(session, through);
            assertEquals(List.of(from, through), views(session));
            capture(session, latest);
            capture(session, addressInstruction(5, 6));

            assertEquals(List.of(from, through, latest), views(session));
            session.update(() -> session.record(0)).join();
            capture(session, addressInstruction(6, 3));
            assertEquals(List.of(from, through, latest), views(session));
        }
    }

    @Test
    public void invalidAddressRangePreservesPreviousCaptureAndActiveMode() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[8]}, 8);
        StepView first = addressInstruction(1, 0);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(1, DebugSession.RecordingMode.FIRST, 0, 0)).join();
            capture(session, first);

            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebugSession.RecordingMode.ADDRESS_RANGE, 5, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebugSession.RecordingMode.ADDRESS_RANGE, -1, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebugSession.RecordingMode.ADDRESS_RANGE, 0, 8));
            capture(session, addressInstruction(2, 1));

            assertTrue(session.isRecording());
            assertEquals(List.of(first), views(session));
        }
    }

    private static StepView addressInstruction(long sequence, int address) {
        return new StepView("Step " + sequence, "nop", OptionalInt.of(address), List.of());
    }

    @Test
    public void breakpointToggleRetainsDisabledAddress() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(2, true).join();
            assertTrue(session.isBreakpointSet(2));

            session.toggleOrdinaryBreakpoint(2);

            session.update(() -> { }).join();
            assertFalse(session.isBreakpointSet(2));
            assertEquals(List.of(2), session.ordinaryBreakpoints());

            session.toggleOrdinaryBreakpoint(2);

            session.update(() -> { }).join();
            assertTrue(session.isBreakpointSet(2));
            session.setBreakpoint(2, false).join();
            assertTrue(session.ordinaryBreakpoints().isEmpty());
        }
    }

    @Test
    public void portableBreakpointReusesRuleAndControlsProbeAttachment() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();
            DebugSession.Breakpoint breakpoint = session.breakpoints.get(0);
            breakpoint.enabled = false;
            session.update(session::updateProbe).join();
            assertFalse(session.isBreakpointSet(1));
            session.setBreakpoint(1, true).join();
            assertEquals(1, session.breakpoints.size());
            assertSame(breakpoint, session.breakpoints.get(0));
            assertTrue(session.isBreakpointSet(1));
            assertTrue(machine.beforeStep(1));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertEquals("Breakpoint: 0001", session.status);

            session.setBreakpoint(1, false).join();
            assertTrue(session.breakpoints.isEmpty());
            verify(machine.debug, atLeastOnce()).observe(eq(session), anyBoolean());
            assertTrue(machine.detached.get() > 0);
        }
    }

    @Test
    public void instructionHookCreatesInspectionHitAndResumeClearsIt() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(machine.cpu.getInstructionLocation()).thenReturn(1);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();
            machine.state(CPU.RunState.STATE_RUNNING);

            assertTrue(machine.beforeStep(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_NORMAL);
            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void settingsRoundTripWatchesBreakpointsAndMemoryWatches() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.watches.add("A + 1");
            session.memoryWatches.add(1);
            session.setBreakpoint(2, true).join();
            session.toggleOrdinaryBreakpoint(2);
            session.update(() -> { }).join();
            session.save();
        }

        Properties saved = AtomicProperties.load(settings);
        Properties expected = new Properties();
        expected.setProperty("watch.0", "A + 1");
        expected.setProperty("breakpoint.0.address", "2");
        expected.setProperty("breakpoint.0.enabled", "false");
        expected.setProperty("memoryWatch.0", "1");
        assertEquals(expected, saved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals(List.of(1), session.memoryWatches);
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertFalse(session.isBreakpointSet(2));
            assertTrue(session.takeLoadWarnings().isEmpty());
            session.toggleOrdinaryBreakpoint(2);
            session.update(() -> { }).join();
            assertTrue(session.isBreakpointSet(2));
        }
    }

    @Test
    public void legacyRulesMigrateToBreakpointsAndMemoryWatches() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("legacy.properties");
        Properties legacy = new Properties();
        legacy.setProperty("version", "4");
        legacy.setProperty("cpu", "another CPU");
        legacy.setProperty("watches", "1");
        legacy.setProperty("watch.0", "A + 1");
        legacy.setProperty("rules", "5");
        String[][] rules = {{"EXEC", "2", "", "false"}, {"EXEC", "1", "A == 1", "true"}, {"READ", "0", "", "true"},
                {"WRITE", "0", "", "true"}, {"CONDITION", "-1", "A > 3", "true"}};
        for (int i = 0; i < rules.length; i++) {
            String key = "rule." + i + ".";
            legacy.setProperty(key + "name", "entry");
            legacy.setProperty(key + "kind", rules[i][0]);
            legacy.setProperty(key + "address", rules[i][1]);
            legacy.setProperty(key + "condition", rules[i][2]);
            legacy.setProperty(key + "enabled", rules[i][3]);
            legacy.setProperty(key + "after", "3");
        }
        legacy.setProperty("image.identity", "Old CPU:3");
        AtomicProperties.save(settings, legacy);
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);

        try (DebugSession session = machine.session(settings)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals(List.of(1, 2), session.ordinaryBreakpoints());
            assertFalse(session.isBreakpointSet(2));
            assertTrue(session.isBreakpointSet(1));
            assertEquals(List.of(0), session.memoryWatches);
            assertEquals(List.of("Removed 2 unsupported breakpoint condition(s) from " + settings + "."),
                    session.takeLoadWarnings());
            assertTrue(session.takeLoadWarnings().isEmpty());
            session.save();
        }

        Properties saved = AtomicProperties.load(settings);
        assertTrue(saved.stringPropertyNames().stream().noneMatch(key -> key.startsWith("rule.")
                || key.equals("version") || key.equals("cpu") || key.equals("watches") || key.equals("image.identity")));
    }

    @Test
    public void settingsFromAnotherCpuOrVersionAreLoaded() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        Properties original = new Properties();
        original.setProperty("version", "2");
        original.setProperty("cpu", "another CPU");
        original.setProperty("watch.0", "A + 1");
        AtomicProperties.save(settings, original);
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebugSession session = machine.session(settings)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertTrue(session.takeLoadWarnings().isEmpty());
        }
        try (var paths = Files.list(settings.getParent())) {
            assertEquals(List.of(settings), paths.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void unreadableSettingsFileIsDeletedWithWarning() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        Files.writeString(settings, "watch.0=\\u12");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebugSession session = machine.session(settings)) {
            assertFalse(Files.exists(settings));
            assertTrue(session.watches.isEmpty());
            assertEquals(List.of("Debugger settings file " + settings + " was unreadable and has been deleted."),
                    session.takeLoadWarnings());
        }
    }

    @Test
    public void settingsFileWithInvalidEncodingIsDeletedWithWarning() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("binary.properties");
        Files.write(settings, new byte[] {'a', '=', (byte) 0xFF, (byte) 0xFE});
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebugSession session = machine.session(settings)) {
            assertFalse(Files.exists(settings));
            assertEquals(List.of("Debugger settings file " + settings + " was unreadable and has been deleted."),
                    session.takeLoadWarnings());
        }
    }

    @Test
    public void portableDisabledBreakpointSurvivesSettingsRoundTrip() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("portable.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.setBreakpoint(1, true).join();
            session.toggleOrdinaryBreakpoint(1);
            session.update(() -> { }).join();
            session.save();
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of(1), session.ordinaryBreakpoints());
            assertFalse(session.isBreakpointSet(1));
            session.toggleOrdinaryBreakpoint(1);
            session.update(() -> { }).join();
            assertTrue(session.isBreakpointSet(1));
        }
    }

    @Test
    public void breakpointAddressIsValidated() {
        assertEquals(0, new DebugSession.Breakpoint(0).address);
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Breakpoint(-1));
    }

    @Test
    public void invalidSettingsEntriesAreSkippedWithWarning() throws Exception {
        Path settings = temporary.newFile("invalid.properties").toPath();
        Properties values = new Properties();
        values.setProperty("breakpoint.0.address", "0");
        values.setProperty("breakpoint.1.address", "99");
        values.setProperty("breakpoint.2.address", "x");
        values.setProperty("breakpoint.3.address", "0");
        values.setProperty("breakpoint.3.enabled", "maybe");
        values.setProperty("memoryWatch.0", "zero");
        values.setProperty("memoryWatch.1", "1");
        values.setProperty("watch.0", " ");
        values.setProperty("rule.0.kind", "SOURCE");
        values.setProperty("entry.0.address", "0");
        values.setProperty("entry.0.end", "99");
        values.setProperty("entry.0.kind", "DATA");
        AtomicProperties.save(settings, values);
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);

        try (DebugSession session = machine.session(settings)) {
            assertEquals(List.of(0), session.ordinaryBreakpoints());
            assertTrue(session.memoryWatches.isEmpty());
            assertTrue(session.watches.isEmpty());
            assertTrue(session.annotations().entries().isEmpty());
            assertEquals(List.of("Skipped 8 invalid debugger setting(s) in " + settings + "."), session.takeLoadWarnings());
        }
    }

    @Test
    public void oneComputerSettingsFileKeepsWatchesBreakpointsAndAnnotationsAcrossEverySave() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebugSession session = original.session(settings)) {
            session.annotations().add(new AnnotationIndex.Entry(1, "buffer", "DATA"));
            session.save();

            session.update(() -> {
                session.watches.add("A + 1");
                session.watches.add("unknown + 1");
            }).join();
            session.setBreakpoint(2, true).join();
            session.save();

            Properties saved = AtomicProperties.load(settings);
            assertEquals("buffer", saved.getProperty("entry.0.label"));
            assertEquals("unknown + 1", saved.getProperty("watch.1"));
            assertEquals("2", saved.getProperty("breakpoint.0.address"));
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebugSession session = reopened.session(settings)) {
            AnnotationIndex.Entry restored = session.annotations().get(1);
            assertEquals("buffer", restored.label);
            assertEquals(List.of("A + 1", "unknown + 1"), session.watches);
            assertTrue(session.isBreakpointSet(2));
            assertTrue(session.watchErrors().containsKey("unknown + 1"));
            session.annotations().update(new AnnotationIndex.Entry(restored.id, 1, 3, "edited buffer", "DATA"));
            session.save();
            assertEquals("edited buffer", AtomicProperties.load(settings).getProperty("entry.0.label"));

            reopened.state(CPU.RunState.STATE_RUNNING);
            session.update(() -> session.watches.add("A * 2")).get(2, TimeUnit.SECONDS);
            session.save();

            assertEquals(CPU.RunState.STATE_RUNNING, session.execution().snapshot().runState());
            assertEquals("A * 2", AtomicProperties.load(settings).getProperty("watch.2"));
            verify(reopened.cpu, never()).step();
            reopened.state(CPU.RunState.STATE_STOPPED_BREAK);
            MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
            session.annotationEngine.put(compiler);
            session.annotations().replace(List.of());
            session.save();

            assertSame(compiler, session.annotationEngine.get(17, 1));
            Properties saved = AtomicProperties.load(settings);
            assertTrue(saved.stringPropertyNames().stream().noneMatch(key -> key.startsWith("entry.")));
        }

        DebuggerTestMachine afterClear = new DebuggerTestMachine(new byte[][] {{6, 5, 4}}, 3);
        try (DebugSession session = afterClear.session(settings)) {
            assertTrue(session.annotations().entries().isEmpty());
            assertEquals(List.of("A + 1", "unknown + 1", "A * 2"), session.watches);
        }
        try (var files = Files.list(settings.getParent())) {
            assertEquals(List.of(settings), files.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void movedComputerSettingsRestoreWatchesAndAnnotations() throws Exception {
        Path original = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(original)) {
            session.annotations().add(new AnnotationIndex.Entry(1, "relocated", "DATA"));
            session.watches.add("A + 1");
            session.save();
        }
        Path moved = temporary.newFolder("relocated").toPath().resolve("renamed.toml.debugger");
        Files.move(original, moved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);

        try (DebugSession session = reopened.session(moved)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals("relocated", session.annotations().get(1).label);
        }
    }

    @Test
    public void malformedAnnotationIsSkippedAndOthersAreImported() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = original.session(settings)) {
            session.annotations().add(new AnnotationIndex.Entry(0, "valid first", "CODE"));
            session.annotations().add(new AnnotationIndex.Entry(1, "invalid later", "DATA"));
            session.save();
        }
        Properties corrupt = AtomicProperties.load(settings);
        corrupt.setProperty("entry.1.end", "99");
        AtomicProperties.save(settings, corrupt);
        MemoryAnnotations engine = new MemoryAnnotations();
        MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
        engine.put(compiler);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings, engine)) {
            assertEquals(List.of("valid first"), session.annotations().entries().stream()
                    .map(entry -> entry.label).collect(java.util.stream.Collectors.toList()));
            assertSame(compiler, engine.get(17, 1));
            assertEquals(1, session.takeLoadWarnings().size());
        }
    }

    @Test
    public void failedCombinedSavePreservesPreviousFileAndRetriesAllSections() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebugSession session = machine.session(settings)) {
            session.watches.add("A");
            AnnotationIndex.Entry annotation = session.annotations().add(new AnnotationIndex.Entry(1, "before", "DATA"));
            session.save();
            byte[] previous = Files.readAllBytes(settings);
            session.watches.add("A + 1");
            session.annotations().update(new AnnotationIndex.Entry(annotation.id, 1, 2, "after", "CODE"));
            try (org.mockito.MockedStatic<AtomicProperties> disk = mockStatic(AtomicProperties.class)) {
                disk.when(() -> AtomicProperties.save(eq(settings), any(Properties.class)))
                        .thenThrow(new IOException("disk full"));

                assertEquals("disk full", assertThrows(IOException.class, session::save).getMessage());
                assertArrayEquals(previous, Files.readAllBytes(settings));
            }

            session.save();

            Properties saved = AtomicProperties.load(settings);
            assertEquals("A + 1", saved.getProperty("watch.1"));
            assertEquals("after", saved.getProperty("entry.0.label"));
            assertEquals("CODE", saved.getProperty("entry.0.kind"));
        }
    }

    @Test
    public void sessionWithoutConfigurationPathKeepsEditableMetadataInMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("unknown + 1")).join();
            session.annotations().add(new AnnotationIndex.Entry(0, "temporary", "DATA"));

            session.save();

            assertEquals("temporary", session.annotations().get(0).label);
            assertEquals(List.of("unknown + 1"), session.watches);
            assertTrue(session.watchErrors().containsKey("unknown + 1"));
            try (var files = Files.list(temporary.getRoot().toPath())) {
                assertEquals(0, files.count());
            }
        }
    }

    @Test
    public void annotationsAreRestoredRegardlessOfProgramOrLayout() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.annotations().add(new AnnotationIndex.Entry(1, "buffer", "DATA"));
            session.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals("buffer", session.annotations().get(1).label);
            verify(reopened.memory, never()).read(anyInt());
        }
        DebuggerTestMachine otherLayout = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebugSession session = new DebugSession(otherLayout.cpu, otherLayout.memory, settings)) {
            assertEquals("buffer", session.annotations().get(1).label);
        }
    }

    @Test
    public void metadataLoadRetriesConcurrentChangesAndPreservesLiveUserEdits() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("concurrent-debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.annotations().add(new AnnotationIndex.Entry(0, "disk old", "CODE"));
            session.annotations().add(new AnnotationIndex.Entry(1, "disk retained", "DATA"));
            session.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        net.emustudio.emulib.runtime.annotations.MemoryAnnotations engine = spy(
                new net.emustudio.emulib.runtime.annotations.MemoryAnnotations());
        AtomicBoolean firstRevision = new AtomicBoolean(true);
        doAnswer(call -> {
            long revision = (long) call.callRealMethod();
            if (firstRevision.getAndSet(false)) {
                engine.put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                        AnnotationIndex.OWNER, 1L << 40, 0, 1, AnnotationIndex.USER_KIND + "CODE", "live edit", null));
                engine.put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                        17, 1, 2, 3, "source", "", null));
            }
            return revision;
        }).when(engine).revision();

        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings, engine)) {
            AnnotationIndex annotations = session.annotations();

            assertEquals(Set.of("live edit", "disk old", "disk retained"), annotations.entries().stream()
                    .map(entry -> entry.label).collect(java.util.stream.Collectors.toSet()));
            assertNotNull(engine.get(17, 1));
        }
    }

    private static StepView instruction(long sequence) {
        return addressInstruction(sequence, 0);
    }

    private static boolean capture(DebugSession session, StepView view) {
        return session.afterStep(true, Optional.of(view));
    }

    private static List<StepView> views(DebugSession session) {
        return session.history().stream().map(event -> event.view).collect(java.util.stream.Collectors.toList());
    }
}
