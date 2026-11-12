/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.application.settings.DebuggerConfig;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebuggerSessionTest {
    @Test(timeout = 5000)
    public void sourceCaptureBecomesReadyAfterListenersObservePausedState() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            java.util.concurrent.atomic.AtomicReference<DebuggerSession.SourceCaptureGuard> guard =
                    new java.util.concurrent.atomic.AtomicReference<>();
            List<String> order = new java.util.concurrent.CopyOnWriteArrayList<>();
            machineExecution.addExecutionListener(state -> {
                assertEquals(CPU.RunState.STATE_STOPPED_BREAK, state);
                session.requirePaused();
                assertFalse(guard.get().whenReady().isDone());
                order.add("published");
            });

            machineExecution.compile(() -> {
                guard.set(session.captureSourceGuard());
                guard.get().whenReady().thenRun(() -> {
                    session.applySourceCapture(guard.get(), () -> order.add("ready"));
                });
            }).join();

            assertEquals(List.of("published", "ready"), order);
        }
    }

    @Test(timeout = 5000)
    public void failedManualStepDoesNotLeaveBreakpointSuppressionEnabled() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.setBreakpoint(0, true).join();
            IllegalStateException failure = new IllegalStateException("Step failed before the instruction hook");
            when(machine.cpu.step()).thenThrow(failure);

            assertSame(failure, assertThrows(java.util.concurrent.CompletionException.class,
                    () -> machineExecution.step().join()).getCause());

            assertTrue(machine.beforeStep(0));
            assertEquals(0, session.takeBreakpointHit().getAsInt());
        }
    }

    @Test
    public void hostNotificationInvalidatesPausedWorkWithoutCpuStateChange() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            DebuggerSession.SourceCaptureGuard guard = session.captureSourceGuard();

            session.executionChanged(CPU.RunState.STATE_STOPPED_BREAK, CPU.RunState.STATE_STOPPED_BREAK);

            assertThrows(IllegalStateException.class,
                    () -> session.applySourceCapture(guard, () -> fail("Applied stale work")));
        }
    }

    @Test
    public void breakpointAndAnnotationEditsPreserveObservedFlow() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            observed(session, 0, 1, "NOP", OptionalInt.of(1));
            session.analyzeFlow(0);
            session.setBreakpoint(1, true).join();
            session.config.annotations().add(new UserAnnotations.Entry(0, "code", "DATA"));

            FlowAnalysis retained = session.analyzeFlow(0);
            assertNotNull(retained.instructionAt(0));
            assertEquals(1, retained.nextAddress(0));
            assertTrue(retained.isData(0));
            assertTrue(session.isBreakpointSet(1));
        }
    }

    @Test
    public void newObservedInstructionDoesNotMutateCapturedView() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            observed(session, 0, 1, "FIRST", OptionalInt.of(1));
            FlowAnalysis first = session.analyzeFlow(0);

            observed(session, 0, 2, "SECOND", OptionalInt.of(2));
            FlowAnalysis second = session.analyzeFlow(0);

            assertEquals(1, first.nextAddress(0));
            assertEquals("FIRST", first.mnemonic(0));
            assertEquals(2, second.nextAddress(0));
            assertEquals("SECOND", second.mnemonic(0));
            machine.memoryChanged(1);
            assertNotNull(session.analyzeFlow(0).instructionAt(0));
            assertEquals(2, second.nextAddress(0));
        }
    }

    @Test
    public void pausedResultCannotBeAppliedToAnotherSession() throws Exception {
        DebuggerTestMachine first = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        DebuggerTestMachine second = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession owner = first.session(null); DebuggerSession other = second.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = first.execution) {
            DebuggerSession.SourceCaptureGuard guard = owner.captureSourceGuard();
            assertThrows(IllegalStateException.class,
                    () -> other.applySourceCapture(guard, () -> fail("Applied to wrong session")));
        }
    }

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void cpuWithoutExecutionHooksRejectsBreakpointChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            IllegalStateException rejected = assertThrows(IllegalStateException.class,
                    () -> session.setBreakpoint(1, true));

            assertEquals("Breakpoints unavailable for this CPU", rejected.getMessage());
            assertTrue(session.config.breakpoints.isEmpty());
            assertFalse(session.isBreakpointSet(1));
        }
    }

    @Test
    public void pauseAtBreakpointAddressDoesNotInventAnExecutionHit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(net.emustudio.application.TestCpu.context(machine.cpu).getInstructionLocation()).thenReturn(1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.setBreakpoint(1, true).join();

            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertTrue(session.takeBreakpointHit().isEmpty());
        }
    }

    @Test
    public void compilationWithoutWritesPreservesMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            long before = session.memoryRevision();

            session.runCompilation(() -> {}, java.util.concurrent.CompletableFuture.completedFuture(null));

            assertEquals(before, session.memoryRevision());
        }
    }

    @Test
    public void compilationWritesInvalidateMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            long before = session.memoryRevision();

            session.runCompilation(() -> machine.memoryChanged(1), java.util.concurrent.CompletableFuture.completedFuture(null));

            assertTrue(session.memoryRevision() > before);
        }
    }

    @Test
    public void compilationRestoresDisabledNotificationsAfterFailure() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        when(machine.memory.areMemoryNotificationsEnabled()).thenReturn(false);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            IllegalStateException failure = new IllegalStateException("compile failed");

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> session.runCompilation(() -> { throw failure; }, java.util.concurrent.CompletableFuture.completedFuture(null))));

            org.mockito.InOrder notifications = inOrder(machine.memory);
            notifications.verify(machine.memory).setMemoryNotificationsEnabled(true);
            notifications.verify(machine.memory).setMemoryNotificationsEnabled(false);
        }
    }

    @Test
    public void uncapturedSourceMappingIsNotUsedAfterMemoryChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
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
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
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
        try (DebuggerSession session = machine.session(null, false);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            assertNull(session.memory);
            assertEquals(32, session.addressSpaceSize());
            session.setBreakpoint(7, true).join();
            assertTrue(session.isBreakpointSet(7));
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(machine.beforeStep(6));
            assertTrue(machine.beforeStep(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(7, session.takeBreakpointHit().getAsInt());
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(machine.beforeStep(7));
            assertTrue(machine.beforeStep(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(7, false).join();
            assertFalse(session.isBreakpointSet(7));
        }
        verifyNoInteractions(machine.memory);
    }

    @Test
    public void executionUsesApplicationBreakpointStore() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.setBreakpoint(1, true).join();
            machine.state(CPU.RunState.STATE_RUNNING);
            assertTrue(session.isBreakpointSet(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(1, false).join();
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.isBreakpointSet(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void deferredSourceCaptureBelongsToCompletedCompileMutationOnly() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            java.util.concurrent.atomic.AtomicReference<DebuggerSession.SourceCaptureGuard> guard = new java.util.concurrent.atomic.AtomicReference<>();
            session.runMutation(() -> {
                guard.set(session.captureSourceGuard());
                assertThrows(IllegalStateException.class, () -> session.applySourceCapture(guard.get(), () -> fail("unfinished")));
                machine.memoryChanged(0);
            }, java.util.concurrent.CompletableFuture.completedFuture(null));
            AtomicBoolean applied = new AtomicBoolean();
            session.applySourceCapture(guard.get(), () -> applied.set(true));
            assertTrue(applied.get());
            session.runMutation(() -> {}, java.util.concurrent.CompletableFuture.completedFuture(null));
            assertThrows(IllegalStateException.class, () -> session.applySourceCapture(guard.get(), () -> fail("stale")));
        }
    }

    @Test
    public void executionAndMemoryChangesRetainObservedFlowAndItsCapturedSnapshot() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            observed(session, 0, 1, "CAPTURED", OptionalInt.of(1));
            FlowAnalysis old = session.analyzeFlow(0);
            session.runExecution(() -> machine.state(CPU.RunState.STATE_RUNNING), java.util.concurrent.CompletableFuture.completedFuture(null));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.memoryChanged(1);

            assertNotNull(session.analyzeFlow(0).instructionAt(0));
            assertEquals("CAPTURED", old.mnemonic(0));
        }
    }

    @Test
    public void annotationsUseSharedMetadataIndexWithoutReadingMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            UserAnnotations initial = session.config.annotations();
            assertEquals(2, initial.addressSpaceSize());
            session.config.annotations().add(new UserAnnotations.Entry(1, "user", "DATA"));
            machine.memoryChanged(1);
            assertEquals("user", session.config.annotations().get(1).label);
            assertSame(initial, session.config.annotations());
            verify(machine.memory, never()).read(anyInt());
            verify(machine.memory, never()).read(anyInt());
        }
    }

    @Test
    public void observedFlowFilteringNeverCallsTheLiveDecoder() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        Decoder decoder = mock(Decoder.class);
        when(machine.cpu.getDecoder()).thenReturn(decoder);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            observed(session, 0, 1, "CAPTURED", OptionalInt.of(1));

            assertEquals("CAPTURED", session.analyzeFlow(0).mnemonic(0));
            verifyNoInteractions(decoder);
        }
    }

    @Test
    public void memoryChangesDoNotRewritePreviouslyExecutedInstructionFacts() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{7, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            observed(session, 0, 1, "VALUE 7", OptionalInt.of(1));
            machine.memoryChanged(0);
            session.runMutation(() -> {}, java.util.concurrent.CompletableFuture.completedFuture(null));

            assertEquals("VALUE 7", session.analyzeFlow(0).mnemonic(0));
        }
    }

    @Test
    public void defaultObserverRetainsBothBranchOutcomesUntilCpuReset() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[256]}, 256);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            assertEquals(net.emustudio.emulib.plugin.debugger.Debugging.Capture.INSTRUCTION, machine.captureMode.get());
            assertNull(session.analyzeFlow(100).instructionAt(100));
            machine.observedStep(DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(103), true);
            machine.observedStep(DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(200), true);
            machine.observedStep(DecodedInstruction.of(201, 1, "FAILED", "00"), OptionalInt.of(202), false);
            machine.observedStep(null, OptionalInt.of(203), true);
            session.config.annotations().add(new UserAnnotations.Entry(100, "entry", "CODE"));
            machine.memoryChanged(100);
            session.analyzeFlow(200);

            FlowAnalysis retained = session.analyzeFlow(100);
            java.util.Set<String> edges = new java.util.HashSet<>();
            retained.forEachEdge((from, to) -> edges.add(from + "->" + to));
            assertEquals(Set.of("100->103", "100->200"), edges);
            assertNull(retained.instructionAt(201));

            machineExecution.reset().get(2, TimeUnit.SECONDS);

            assertNull(session.analyzeFlow(100).instructionAt(100));
            assertEquals("entry", session.config.annotations().get(100).label);
        }
    }


    @Test
    public void failedOrUncapturedStepsDoNotInventExecutedInstructions() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.afterStep(false, Optional.of(DecodedInstruction.of(0, 1, "FAILED", "00")),
                    OptionalInt.of(1), Optional.empty());
            session.afterStep(true, Optional.empty(), OptionalInt.of(1), Optional.empty());

            assertNull(session.analyzeFlow(0).instructionAt(0));

            observed(session, 0, 1, "DONE", OptionalInt.empty());
            assertEquals("DONE", session.analyzeFlow(0).mnemonic(0));
        }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue("Barrier timed out", latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @Test
    public void runningSessionRejectsPausedOperationsAndResumesAfterStop() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            machine.state(CPU.RunState.STATE_RUNNING);
            assertThrows(IllegalStateException.class, session::requirePaused);
            session.update(() -> session.record(2)).join();
            assertTrue(session.recording.isRecording());

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.requirePaused();
        }
    }

    @Test
    public void resumeAndPauseDiscardAnEarlierPausedGeneration() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            DebuggerSession.SourceCaptureGuard guard = session.captureSourceGuard();
            AtomicBoolean applied = new AtomicBoolean();
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertThrows(IllegalStateException.class,
                    () -> session.applySourceCapture(guard, () -> applied.set(true)));
            assertFalse(applied.get());
        }
    }

    @Test
    public void failedMutationStillInvalidatesAnalysisAndReleasesMutationGuard() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            DebuggerSession.SourceCaptureGuard before = session.captureSourceGuard();
            assertThrows(IllegalArgumentException.class, () -> session.runMutation(() -> {
                DebuggerSession.SourceCaptureGuard during = session.captureSourceGuard();
                assertThrows(IllegalStateException.class, () -> session.applySourceCapture(during, () -> {}));
                throw new IllegalArgumentException("edit failed");
            }, java.util.concurrent.CompletableFuture.completedFuture(null)));

            assertThrows(IllegalStateException.class, () -> session.applySourceCapture(before, () -> {}));
            session.applySourceCapture(session.captureSourceGuard(), () -> {});
        }
    }

    @Test
    public void ownerStopsExecutionBeforeClosingDebuggerResources() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        DebuggerSession session = machine.session(null);
        net.emustudio.application.emulation.EmulationController machineExecution = machine.execution;
        session.update(() -> session.record(2)).join();
        machine.state(CPU.RunState.STATE_RUNNING);

        assertThrows(IllegalStateException.class, session::close);
        assertEquals(CPU.RunState.STATE_RUNNING, machineExecution.getRunState());
        machineExecution.close();

        session.close();

        assertTrue(machine.detached.get() > 0);
        assertTrue(machineExecution.closeAsync().isDone());
        assertTrue(machine.memoryListeners.isEmpty());
        assertThrows(IllegalStateException.class, session::requirePaused);
    }

    @Test
    public void boundedHistoryKeepsNewestInstructionsAndDisablingStopsCapture() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        StepView first = instruction(1);
        StepView second = instruction(2);
        StepView third = instruction(3);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.record(2)).join();
            capture(session, first);
            capture(session, second);
            capture(session, third);
            assertEquals(List.of(second, third), views(session));
            assertThrows(UnsupportedOperationException.class, () -> session.recording.events().clear());

            session.update(() -> session.record(0)).join();
            capture(session, instruction(4));
            assertFalse(session.recording.isRecording());
            assertEquals(List.of(second, third), views(session));
            verify(machine.debug, atLeastOnce()).observe(eq(session), any(Debugging.Capture.class));
            assertTrue(machine.detached.get() > 0);
        }
    }

    @Test
    public void historyRequiresDebuggerCapabilityAndValidLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            assertThrows(IllegalStateException.class, () -> session.record(1));
            assertThrows(IllegalArgumentException.class, () -> session.record(-1));
            assertThrows(IllegalStateException.class,
                    () -> session.recording.saveHistory(temporary.getRoot().toPath().resolve("unsupported.emutrace"), false));
            assertFalse(session.recording.isRecording());
        }
    }

    @Test
    public void newRecordingClearsPreviousCaptureAfterLimitValidation() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.record(2)).join();
            capture(session, instruction(1));
            List<InstructionRecording.Event> previous = session.recording.events();

            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(0, InstructionRecording.Mode.LAST, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(1_000_001, InstructionRecording.Mode.LAST, 0, 0));
            assertEquals(previous, session.recording.events());
            session.update(() -> session.startNewRecording(1, InstructionRecording.Mode.LAST, 0, 0)).join();
            capture(session, instruction(2));
            capture(session, instruction(3));

            assertEquals(1, previous.size());
            assertEquals(1, session.recording.events().size());
            assertEquals(3, session.recording.events().get(0).sequence);
        }
    }

    @Test
    public void firstInstructionsRemainWhenLaterExecutionExceedsLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        StepView first = instruction(1), second = instruction(2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.startNewRecording(2, InstructionRecording.Mode.FIRST, 0, 0)).join();

            capture(session, first);
            capture(session, second);
            capture(session, instruction(3));

            assertEquals(List.of(first, second), views(session));
            assertTrue(session.recording.isRecording());
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
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.startNewRecording(2, InstructionRecording.Mode.ADDRESS_RANGE, 3, 5)).join();

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
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.startNewRecording(1, InstructionRecording.Mode.FIRST, 0, 0)).join();
            capture(session, first);

            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, InstructionRecording.Mode.ADDRESS_RANGE, 5, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, InstructionRecording.Mode.ADDRESS_RANGE, -1, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, InstructionRecording.Mode.ADDRESS_RANGE, 0, 8));
            capture(session, addressInstruction(2, 1));

            assertTrue(session.recording.isRecording());
            assertEquals(List.of(first), views(session));
        }
    }

    private static StepView addressInstruction(long sequence, int address) {
        return new StepView("Step " + sequence, "nop", OptionalInt.of(address), List.of());
    }

    @Test
    public void portableBreakpointAddressesAreUniqueWhileInstructionObservationRemainsAttached() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.setBreakpoint(1, true).join();
            session.setBreakpoint(1, true).join();
            assertEquals(Set.of(1), session.config.breakpoints);
            assertTrue(session.isBreakpointSet(1));
            assertTrue(machine.beforeStep(1));
            assertEquals(1, session.takeBreakpointHit().getAsInt());

            session.setBreakpoint(1, false).join();
            assertTrue(session.config.breakpoints.isEmpty());
            verify(machine.debug, atLeastOnce()).observe(eq(session), any(Debugging.Capture.class));
            assertEquals(Debugging.Capture.INSTRUCTION, machine.captureMode.get());
        }
    }

    @Test
    public void instructionHookCreatesInspectionHitAndResumeClearsIt() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(net.emustudio.application.TestCpu.context(machine.cpu).getInstructionLocation()).thenReturn(1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.setBreakpoint(1, true).join();
            machine.state(CPU.RunState.STATE_RUNNING);

            assertTrue(machine.beforeStep(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertEquals(1, session.takeBreakpointHit().getAsInt());

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_NORMAL);
            assertTrue(session.takeBreakpointHit().isEmpty());
        }
    }

    @Test
    public void settingsRoundTripWatchesAndBreakpoints() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            session.config.watches.add("A + 1");
            session.config.watches.add("mem[0x0001]");
            session.setBreakpoint(2, true).join();
            session.update(() -> { }).join();
            session.config.save();
        }

        Properties saved = DebuggerConfig.loadProperties(settings);
        Properties expected = new Properties();
        expected.setProperty("watch.0", "A + 1");
        expected.setProperty("watch.1", "mem[0x0001]");
        expected.setProperty("breakpoint.0.address", "2");
        assertEquals(expected, saved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            assertEquals(List.of("A + 1", "mem[0x0001]"), session.config.watches);
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertTrue(session.isBreakpointSet(2));
            assertTrue(session.config.takeLoadWarnings().isEmpty());
            session.update(() -> { }).join();
            assertTrue(session.isBreakpointSet(2));
        }
    }

    @Test
    public void removedBreakpointIsAbsentAfterSettingsRoundTrip() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("portable.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = original.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = original.execution) {
            session.setBreakpoint(1, true).join();
            session.setBreakpoint(1, false).join();
            session.config.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = reopened.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = reopened.execution) {
            assertFalse(session.isBreakpointSet(1));
            assertTrue(session.config.breakpoints.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> session.setBreakpoint(-1, true));
            assertThrows(IllegalArgumentException.class, () -> session.setBreakpoint(2, true));
        }
    }

    @Test
    public void oneComputerSettingsFileKeepsWatchesBreakpointsAndAnnotationsAcrossEverySave() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebuggerSession session = original.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = original.execution) {
            session.config.annotations().add(new UserAnnotations.Entry(1, "buffer", "DATA"));
            session.config.save();

            session.update(() -> {
                session.config.watches.add("A + 1");
                session.config.watches.add("unknown + 1");
            }).join();
            session.setBreakpoint(2, true).join();
            session.config.save();

            Properties saved = DebuggerConfig.loadProperties(settings);
            assertEquals("buffer", saved.getProperty("annotation.0.label"));
            assertEquals("unknown + 1", saved.getProperty("watch.1"));
            assertEquals("2", saved.getProperty("breakpoint.0.address"));
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebuggerSession session = reopened.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = reopened.execution) {
            UserAnnotations.Entry restored = session.config.annotations().get(1);
            assertEquals("buffer", restored.label);
            assertEquals(List.of("A + 1", "unknown + 1"), session.config.watches);
            assertTrue(session.isBreakpointSet(2));
            assertTrue(session.watchErrors().containsKey("unknown + 1"));
            session.config.annotations().update(new UserAnnotations.Entry(restored.id, 1, 3, "edited buffer", "DATA"));
            session.config.save();
            assertEquals("edited buffer", DebuggerConfig.loadProperties(settings).getProperty("annotation.0.label"));

            reopened.state(CPU.RunState.STATE_RUNNING);
            session.update(() -> session.config.watches.add("A * 2")).get(2, TimeUnit.SECONDS);
            session.config.save();

            assertEquals(CPU.RunState.STATE_RUNNING, machineExecution.getRunState());
            assertEquals("A * 2", DebuggerConfig.loadProperties(settings).getProperty("watch.2"));
            verify(reopened.cpu, never()).step();
            reopened.state(CPU.RunState.STATE_STOPPED_BREAK);
            MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
            session.annotationEngine.put(compiler);
            session.config.annotations().replace(List.of());
            session.config.save();

            assertSame(compiler, session.annotationEngine.get(17, 1));
            Properties saved = DebuggerConfig.loadProperties(settings);
            assertTrue(saved.stringPropertyNames().stream().noneMatch(key -> key.startsWith("annotation.")));
        }

        DebuggerTestMachine afterClear = new DebuggerTestMachine(new byte[][] {{6, 5, 4}}, 3);
        try (DebuggerSession session = afterClear.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = afterClear.execution) {
            assertTrue(session.config.annotations().entries().isEmpty());
            assertEquals(List.of("A + 1", "unknown + 1", "A * 2"), session.config.watches);
        }
        try (var files = Files.list(settings.getParent())) {
            assertEquals(List.of(settings), files.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void movedComputerSettingsRestoreWatchesAndAnnotations() throws Exception {
        Path original = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(original);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.config.annotations().add(new UserAnnotations.Entry(1, "relocated", "DATA"));
            session.config.watches.add("A + 1");
            session.config.save();
        }
        Path moved = temporary.newFolder("relocated").toPath().resolve("renamed.toml.debugger");
        Files.move(original, moved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);

        try (DebuggerSession session = reopened.session(moved);
             net.emustudio.application.emulation.EmulationController machineExecution = reopened.execution) {
            assertEquals(List.of("A + 1"), session.config.watches);
            assertEquals("relocated", session.config.annotations().get(1).label);
        }
    }

    @Test
    public void malformedAnnotationIsSkippedAndOthersAreImported() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = original.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = original.execution) {
            session.config.annotations().add(new UserAnnotations.Entry(0, "valid first", "CODE"));
            session.config.annotations().add(new UserAnnotations.Entry(1, "invalid later", "DATA"));
            session.config.save();
        }
        Properties corrupt = DebuggerConfig.loadProperties(settings);
        corrupt.setProperty("annotation.1.end", "99");
        DebuggerConfig.saveProperties(settings, corrupt);
        MemoryAnnotations engine = new MemoryAnnotations();
        MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
        engine.put(compiler);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings, engine);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            assertEquals(List.of("valid first"), session.config.annotations().entries().stream()
                    .map(entry -> entry.label).collect(java.util.stream.Collectors.toList()));
            assertSame(compiler, engine.get(17, 1));
            assertEquals(1, session.config.takeLoadWarnings().size());
        }
    }

    @Test
    public void failedCombinedSavePreservesPreviousFileAndRetriesAllSections() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(settings);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.config.watches.add("A");
            UserAnnotations.Entry annotation = session.config.annotations().add(new UserAnnotations.Entry(1, "before", "DATA"));
            session.config.save();
            byte[] previous = Files.readAllBytes(settings);
            session.config.watches.add("A + 1");
            session.config.annotations().update(new UserAnnotations.Entry(annotation.id, 1, 2, "after", "CODE"));
            try (org.mockito.MockedStatic<DebuggerConfig> disk = mockStatic(DebuggerConfig.class)) {
                disk.when(() -> DebuggerConfig.saveProperties(eq(settings), any(Properties.class)))
                        .thenThrow(new IOException("disk full"));

                assertEquals("disk full", assertThrows(IOException.class, session.config::save).getMessage());
                assertArrayEquals(previous, Files.readAllBytes(settings));
            }

            session.config.save();

            Properties saved = DebuggerConfig.loadProperties(settings);
            assertEquals("A + 1", saved.getProperty("watch.1"));
            assertEquals("after", saved.getProperty("annotation.0.label"));
            assertEquals("CODE", saved.getProperty("annotation.0.kind"));
        }
    }

    @Test
    public void sessionWithoutConfigurationPathKeepsEditableMetadataInMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null);
             net.emustudio.application.emulation.EmulationController machineExecution = machine.execution) {
            session.update(() -> session.config.watches.add("unknown + 1")).join();
            session.config.annotations().add(new UserAnnotations.Entry(0, "temporary", "DATA"));

            session.config.save();

            assertEquals("temporary", session.config.annotations().get(0).label);
            assertEquals(List.of("unknown + 1"), session.config.watches);
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
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            session.config.annotations().add(new UserAnnotations.Entry(1, "buffer", "DATA"));
            session.config.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);
        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            assertEquals("buffer", session.config.annotations().get(1).label);
            verify(reopened.memory, never()).read(anyInt());
        }
        DebuggerTestMachine otherLayout = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebuggerSession session = new DebuggerSession(otherLayout.cpu, net.emustudio.application.TestCpu.context(otherLayout.cpu), otherLayout.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            assertEquals("buffer", session.config.annotations().get(1).label);
        }
    }

    @Test
    public void metadataLoadRetriesConcurrentChangesAndPreservesLiveUserEdits() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("concurrent-debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            session.config.annotations().add(new UserAnnotations.Entry(0, "disk old", "CODE"));
            session.config.annotations().add(new UserAnnotations.Entry(1, "disk retained", "DATA"));
            session.config.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        net.emustudio.emulib.application.annotations.MemoryAnnotations engine = spy(
                new net.emustudio.emulib.application.annotations.MemoryAnnotations());
        AtomicBoolean firstRevision = new AtomicBoolean(true);
        doAnswer(call -> {
            long revision = (long) call.callRealMethod();
            if (firstRevision.getAndSet(false)) {
                engine.put(new net.emustudio.emulib.application.annotations.MemoryAnnotations.Entry(
                        UserAnnotations.OWNER, 1L << 40, 0, 1, "CODE", "live edit", null));
                engine.put(new net.emustudio.emulib.application.annotations.MemoryAnnotations.Entry(
                        17, 1, 2, 3, "source", "", null));
            }
            return revision;
        }).when(engine).revision();

        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings, engine);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            UserAnnotations annotations = session.config.annotations();

            assertEquals(Set.of("live edit", "disk old", "disk retained"), annotations.entries().stream()
                    .map(entry -> entry.label).collect(java.util.stream.Collectors.toSet()));
            assertNotNull(engine.get(17, 1));
        }
    }

    private static void observed(DebuggerSession session, int address, int length, String mnemonic, OptionalInt next) {
        session.afterStep(true, Optional.of(DecodedInstruction.of(address, length, mnemonic, "00")), next, Optional.empty());
    }

    private static StepView instruction(long sequence) {
        return addressInstruction(sequence, 0);
    }

    private static boolean capture(DebuggerSession session, StepView view) {
        return session.afterStep(true, Optional.empty(), OptionalInt.empty(), Optional.of(view));
    }

    private static List<StepView> views(DebuggerSession session) {
        return session.recording.events().stream().map(event -> event.view).collect(java.util.stream.Collectors.toList());
    }
}
