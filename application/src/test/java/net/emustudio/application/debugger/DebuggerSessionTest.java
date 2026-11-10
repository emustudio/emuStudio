/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

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
    @Test
    public void queuedMachineOperationInvalidatesPausedWorkBeforeCpuStateChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            DebuggerSession.Stamp stamp = session.pausedStamp();

            session.executionChanged(CPU.RunState.STATE_STOPPED_BREAK, 1);

            assertThrows(IllegalStateException.class,
                    () -> session.applyCurrent(stamp, "Machine changed", () -> fail("Applied stale work")));
        }
    }

    @Test
    public void breakpointAndAnnotationEditsPreserveObservedFlow() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            observed(session, 0, 1, "NOP", OptionalInt.of(1));
            session.analyzeFlow(0, 10);
            session.setBreakpoint(1, true).join();
            session.annotations().add(new UserAnnotations.Entry(0, "code", "DATA"));

            FlowAnalysis retained = session.analyzeFlow(0, 10);
            assertNotNull(retained.instructionAt(0));
            assertEquals(1, retained.nextAddress(0));
            assertTrue(retained.isData(0));
            assertTrue(session.isBreakpointSet(1));
        }
    }

    @Test
    public void newObservedInstructionDoesNotMutateCapturedView() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = machine.session(null)) {
            observed(session, 0, 1, "FIRST", OptionalInt.of(1));
            FlowAnalysis first = session.analyzeFlow(0, 10);

            observed(session, 0, 2, "SECOND", OptionalInt.of(2));
            FlowAnalysis second = session.analyzeFlow(0, 10);

            assertEquals(1, first.nextAddress(0));
            assertEquals("FIRST", first.mnemonic(0));
            assertEquals(2, second.nextAddress(0));
            assertEquals("SECOND", second.mnemonic(0));
            machine.memoryChanged(1);
            assertNotNull(session.analyzeFlow(0, 10).instructionAt(0));
            assertEquals(2, second.nextAddress(0));
        }
    }

    @Test
    public void pausedResultCannotBeAppliedToAnotherSession() throws Exception {
        DebuggerTestMachine first = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        DebuggerTestMachine second = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession owner = first.session(null); DebuggerSession other = second.session(null)) {
            DebuggerSession.Stamp stamp = owner.pausedStamp();
            assertThrows(IllegalStateException.class,
                    () -> other.applyCurrent(stamp, "Wrong session", () -> fail("Applied to wrong session")));
        }
    }

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void cpuWithoutExecutionHooksRejectsBreakpointChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebuggerSession session = machine.session(null)) {
            IllegalStateException rejected = assertThrows(IllegalStateException.class,
                    () -> session.setBreakpoint(1, true));

            assertEquals("Breakpoints unavailable for this CPU", rejected.getMessage());
            assertTrue(session.breakpoints.isEmpty());
            assertFalse(session.isBreakpointSet(1));
        }
    }

    @Test
    public void pauseAtBreakpointAddressDoesNotInventAnExecutionHit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(net.emustudio.application.TestCpu.context(machine.cpu).getInstructionLocation()).thenReturn(1);
        try (DebuggerSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();

            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void compilationWithoutWritesPreservesMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebuggerSession session = machine.session(null)) {
            long before = session.memoryRevision();

            session.runCompilation(() -> {});

            assertEquals(before, session.memoryRevision());
        }
    }

    @Test
    public void compilationWritesInvalidateMemoryRevision() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        try (DebuggerSession session = machine.session(null)) {
            long before = session.memoryRevision();

            session.runCompilation(() -> machine.memoryChanged(1));

            assertTrue(session.memoryRevision() > before);
        }
    }

    @Test
    public void compilationRestoresDisabledNotificationsAfterFailure() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32);
        when(machine.memory.areMemoryNotificationsEnabled()).thenReturn(false);
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null, false)) {
            assertNull(session.memory);
            assertEquals(32, session.addressSpaceSize());
            session.setBreakpoint(7, true).join();
            assertTrue(session.isBreakpointSet(7));
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(machine.beforeStep(6));
            assertTrue(machine.beforeStep(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebuggerSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(7, hit.address);
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
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null)) {
            java.util.concurrent.atomic.AtomicReference<DebuggerSession.SourceCaptureGuard> guard = new java.util.concurrent.atomic.AtomicReference<>();
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
    public void executionAndMemoryChangesRetainObservedFlowAndItsCapturedSnapshot() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            observed(session, 0, 1, "CAPTURED", OptionalInt.of(1));
            FlowAnalysis old = session.analyzeFlow(0, 10);
            session.runExecution(() -> machine.state(CPU.RunState.STATE_RUNNING));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.memoryChanged(1);

            assertNotNull(session.analyzeFlow(0, 10).instructionAt(0));
            assertEquals("CAPTURED", old.mnemonic(0));
        }
    }

    @Test
    public void annotationsUseSharedMetadataIndexWithoutReadingMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            UserAnnotations initial = session.annotations();
            assertEquals(2, initial.addressSpaceSize());
            session.annotations().add(new UserAnnotations.Entry(1, "user", "DATA"));
            machine.memoryChanged(1);
            assertEquals("user", session.annotations().get(1).label);
            assertSame(initial, session.annotations());
            verify(machine.memory, never()).read(anyInt());
            verify(machine.memory, never()).read(anyInt());
        }
    }

    @Test
    public void observedFlowFilteringNeverCallsTheLiveDecoder() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        Decoder decoder = mock(Decoder.class);
        when(machine.cpu.getDecoder()).thenReturn(decoder);
        try (DebuggerSession session = machine.session(null)) {
            observed(session, 0, 1, "CAPTURED", OptionalInt.of(1));

            assertEquals("CAPTURED", session.analyzeFlow(0, 10).mnemonic(0));
            verifyNoInteractions(decoder);
        }
    }

    @Test
    public void memoryChangesDoNotRewritePreviouslyExecutedInstructionFacts() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{7, 0}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            observed(session, 0, 1, "VALUE 7", OptionalInt.of(1));
            machine.memoryChanged(0);
            session.runMutation(() -> {});

            assertEquals("VALUE 7", session.analyzeFlow(0, 10).mnemonic(0));
        }
    }

    @Test
    public void defaultObserverRetainsBothBranchOutcomesUntilCpuReset() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[256]}, 256);
        try (DebuggerSession session = machine.session(null)) {
            assertEquals(net.emustudio.emulib.plugin.debugger.Debugging.Capture.INSTRUCTION, machine.captureMode.get());
            assertNull(session.analyzeFlow(100, 10).instructionAt(100));
            machine.observedStep(DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(103), true);
            machine.observedStep(DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(200), true);
            machine.observedStep(DecodedInstruction.of(201, 1, "FAILED", "00"), OptionalInt.of(202), false);
            machine.observedStep(null, OptionalInt.of(203), true);
            session.annotations().add(new UserAnnotations.Entry(100, "entry", "CODE"));
            machine.memoryChanged(100);
            session.analyzeFlow(200, 10);

            FlowAnalysis retained = session.analyzeFlow(100, 10);
            java.util.Set<String> edges = new java.util.HashSet<>();
            retained.forEachEdge((from, to) -> edges.add(from + "->" + to));
            assertEquals(Set.of("100->103", "100->200"), edges);
            assertNull(retained.instructionAt(201));

            session.execution().reset().get(2, TimeUnit.SECONDS);

            assertNull(session.analyzeFlow(100, 10).instructionAt(100));
            assertEquals("entry", session.annotations().get(100).label);
        }
    }


    @Test
    public void failedOrUncapturedStepsDoNotInventExecutedInstructions() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            session.afterStep(false, Optional.of(DecodedInstruction.of(0, 1, "FAILED", "00")),
                    OptionalInt.of(1), Optional.empty());
            session.afterStep(true, Optional.empty(), OptionalInt.of(1), Optional.empty());

            assertNull(session.analyzeFlow(0, 10).instructionAt(0));

            observed(session, 0, 1, "DONE", OptionalInt.empty());
            assertEquals("DONE", session.analyzeFlow(0, 10).mnemonic(0));
        }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue("Barrier timed out", latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @Test
    public void runningSessionRejectsPausedOperationsAndResumesAfterStop() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            machine.state(CPU.RunState.STATE_RUNNING);
            assertThrows(IllegalStateException.class, session::requirePaused);
            session.update(() -> session.record(2)).join();
            assertTrue(session.isRecording());

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.requirePaused();
        }
    }

    @Test
    public void resumeAndPauseDiscardAnEarlierPausedGeneration() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null)) {
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
        DebuggerSession session = machine.session(null);
        session.update(() -> session.record(2)).join();
        machine.state(CPU.RunState.STATE_RUNNING);

        session.close();

        assertTrue(machine.detached.get() > 0);
        assertTrue(session.execution().closeAsync().isDone());
        assertTrue(machine.memoryListeners.isEmpty());
        assertThrows(IllegalStateException.class, session::requirePaused);
    }

    @Test
    public void boundedHistoryKeepsNewestInstructionsAndDisablingStopsCapture() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        StepView first = instruction(1);
        StepView second = instruction(2);
        StepView third = instruction(3);
        try (DebuggerSession session = machine.session(null)) {
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
            verify(machine.debug, atLeastOnce()).observe(eq(session), any(Debugging.Capture.class));
            assertTrue(machine.detached.get() > 0);
        }
    }

    @Test
    public void historyRequiresDebuggerCapabilityAndValidLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebuggerSession session = machine.session(null)) {
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
        try (DebuggerSession session = machine.session(null)) {
            session.update(() -> session.record(2)).join();
            capture(session, instruction(1));
            List<InstructionRecording.Event> previous = session.history();

            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(0, DebuggerSession.RecordingMode.LAST, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> session.startNewRecording(1_000_001, DebuggerSession.RecordingMode.LAST, 0, 0));
            assertEquals(previous, session.history());
            session.update(() -> session.startNewRecording(1, DebuggerSession.RecordingMode.LAST, 0, 0)).join();
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
        try (DebuggerSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(2, DebuggerSession.RecordingMode.FIRST, 0, 0)).join();

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
        try (DebuggerSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(2, DebuggerSession.RecordingMode.ADDRESS_RANGE, 3, 5)).join();

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
        try (DebuggerSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(1, DebuggerSession.RecordingMode.FIRST, 0, 0)).join();
            capture(session, first);

            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebuggerSession.RecordingMode.ADDRESS_RANGE, 5, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebuggerSession.RecordingMode.ADDRESS_RANGE, -1, 3));
            assertThrows(IllegalArgumentException.class,
                    () -> session.startNewRecording(2, DebuggerSession.RecordingMode.ADDRESS_RANGE, 0, 8));
            capture(session, addressInstruction(2, 1));

            assertTrue(session.isRecording());
            assertEquals(List.of(first), views(session));
        }
    }

    private static StepView addressInstruction(long sequence, int address) {
        return new StepView("Step " + sequence, "nop", OptionalInt.of(address), List.of());
    }

    @Test
    public void portableBreakpointAddressesAreUniqueWhileInstructionObservationRemainsAttached() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();
            session.setBreakpoint(1, true).join();
            assertEquals(Set.of(1), session.breakpoints);
            assertTrue(session.isBreakpointSet(1));
            assertTrue(machine.beforeStep(1));
            DebuggerSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertTrue(session.status.isEmpty());

            session.setBreakpoint(1, false).join();
            assertTrue(session.breakpoints.isEmpty());
            verify(machine.debug, atLeastOnce()).observe(eq(session), any(Debugging.Capture.class));
            assertEquals(Debugging.Capture.INSTRUCTION, machine.captureMode.get());
        }
    }

    @Test
    public void instructionHookCreatesInspectionHitAndResumeClearsIt() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        when(net.emustudio.application.TestCpu.context(machine.cpu).getInstructionLocation()).thenReturn(1);
        try (DebuggerSession session = machine.session(null)) {
            session.setBreakpoint(1, true).join();
            machine.state(CPU.RunState.STATE_RUNNING);

            assertTrue(machine.beforeStep(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);

            DebuggerSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_NORMAL);
            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void settingsRoundTripWatchesAndBreakpoints() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings)) {
            session.watches.add("A + 1");
            session.watches.add("mem[0x0001]");
            session.setBreakpoint(2, true).join();
            session.update(() -> { }).join();
            session.save();
        }

        Properties saved = AtomicProperties.load(settings);
        Properties expected = new Properties();
        expected.setProperty("watch.0", "A + 1");
        expected.setProperty("watch.1", "mem[0x0001]");
        expected.setProperty("breakpoint.0.address", "2");
        assertEquals(expected, saved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3);
        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings)) {
            assertEquals(List.of("A + 1", "mem[0x0001]"), session.watches);
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertTrue(session.isBreakpointSet(2));
            assertTrue(session.takeLoadWarnings().isEmpty());
            session.update(() -> { }).join();
            assertTrue(session.isBreakpointSet(2));
        }
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

        try (DebuggerSession session = machine.session(settings)) {
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

        try (DebuggerSession session = machine.session(settings)) {
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

        try (DebuggerSession session = machine.session(settings)) {
            assertFalse(Files.exists(settings));
            assertEquals(List.of("Debugger settings file " + settings + " was unreadable and has been deleted."),
                    session.takeLoadWarnings());
        }
    }

    @Test
    public void removedBreakpointIsAbsentAfterSettingsRoundTrip() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("portable.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = original.session(settings)) {
            session.setBreakpoint(1, true).join();
            session.setBreakpoint(1, false).join();
            session.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = reopened.session(settings)) {
            assertFalse(session.isBreakpointSet(1));
            assertTrue(session.breakpoints.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> session.setBreakpoint(-1, true));
            assertThrows(IllegalArgumentException.class, () -> session.setBreakpoint(2, true));
        }
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
        values.setProperty("watch.0", " ");
        values.setProperty("rule.0.kind", "SOURCE");
        values.setProperty("annotation.0.address", "0");
        values.setProperty("annotation.0.end", "99");
        values.setProperty("annotation.0.kind", "DATA");
        AtomicProperties.save(settings, values);
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);

        try (DebuggerSession session = machine.session(settings)) {
            assertEquals(List.of(0), session.ordinaryBreakpoints());
            assertTrue(session.watches.isEmpty());
            assertTrue(session.annotations().entries().isEmpty());
            assertEquals(List.of("Skipped 4 invalid debugger setting(s) in " + settings + "."), session.takeLoadWarnings());
        }
    }

    @Test
    public void oneComputerSettingsFileKeepsWatchesBreakpointsAndAnnotationsAcrossEverySave() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebuggerSession session = original.session(settings)) {
            session.annotations().add(new UserAnnotations.Entry(1, "buffer", "DATA"));
            session.save();

            session.update(() -> {
                session.watches.add("A + 1");
                session.watches.add("unknown + 1");
            }).join();
            session.setBreakpoint(2, true).join();
            session.save();

            Properties saved = AtomicProperties.load(settings);
            assertEquals("buffer", saved.getProperty("annotation.0.label"));
            assertEquals("unknown + 1", saved.getProperty("watch.1"));
            assertEquals("2", saved.getProperty("breakpoint.0.address"));
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebuggerSession session = reopened.session(settings)) {
            UserAnnotations.Entry restored = session.annotations().get(1);
            assertEquals("buffer", restored.label);
            assertEquals(List.of("A + 1", "unknown + 1"), session.watches);
            assertTrue(session.isBreakpointSet(2));
            assertTrue(session.watchErrors().containsKey("unknown + 1"));
            session.annotations().update(new UserAnnotations.Entry(restored.id, 1, 3, "edited buffer", "DATA"));
            session.save();
            assertEquals("edited buffer", AtomicProperties.load(settings).getProperty("annotation.0.label"));

            reopened.state(CPU.RunState.STATE_RUNNING);
            session.update(() -> session.watches.add("A * 2")).get(2, TimeUnit.SECONDS);
            session.save();

            assertEquals(CPU.RunState.STATE_RUNNING, session.execution().getRunState());
            assertEquals("A * 2", AtomicProperties.load(settings).getProperty("watch.2"));
            verify(reopened.cpu, never()).step();
            reopened.state(CPU.RunState.STATE_STOPPED_BREAK);
            MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
            session.annotationEngine.put(compiler);
            session.annotations().replace(List.of());
            session.save();

            assertSame(compiler, session.annotationEngine.get(17, 1));
            Properties saved = AtomicProperties.load(settings);
            assertTrue(saved.stringPropertyNames().stream().noneMatch(key -> key.startsWith("annotation.")));
        }

        DebuggerTestMachine afterClear = new DebuggerTestMachine(new byte[][] {{6, 5, 4}}, 3);
        try (DebuggerSession session = afterClear.session(settings)) {
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
        try (DebuggerSession session = machine.session(original)) {
            session.annotations().add(new UserAnnotations.Entry(1, "relocated", "DATA"));
            session.watches.add("A + 1");
            session.save();
        }
        Path moved = temporary.newFolder("relocated").toPath().resolve("renamed.toml.debugger");
        Files.move(original, moved);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);

        try (DebuggerSession session = reopened.session(moved)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals("relocated", session.annotations().get(1).label);
        }
    }

    @Test
    public void malformedAnnotationIsSkippedAndOthersAreImported() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("computer.toml.debugger");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);
        try (DebuggerSession session = original.session(settings)) {
            session.annotations().add(new UserAnnotations.Entry(0, "valid first", "CODE"));
            session.annotations().add(new UserAnnotations.Entry(1, "invalid later", "DATA"));
            session.save();
        }
        Properties corrupt = AtomicProperties.load(settings);
        corrupt.setProperty("annotation.1.end", "99");
        AtomicProperties.save(settings, corrupt);
        MemoryAnnotations engine = new MemoryAnnotations();
        MemoryAnnotations.Entry compiler = new MemoryAnnotations.Entry(17, 1, 0, 1, "source", "compiler", null);
        engine.put(compiler);
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2);

        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings, engine)) {
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
        try (DebuggerSession session = machine.session(settings)) {
            session.watches.add("A");
            UserAnnotations.Entry annotation = session.annotations().add(new UserAnnotations.Entry(1, "before", "DATA"));
            session.save();
            byte[] previous = Files.readAllBytes(settings);
            session.watches.add("A + 1");
            session.annotations().update(new UserAnnotations.Entry(annotation.id, 1, 2, "after", "CODE"));
            try (org.mockito.MockedStatic<AtomicProperties> disk = mockStatic(AtomicProperties.class)) {
                disk.when(() -> AtomicProperties.save(eq(settings), any(Properties.class)))
                        .thenThrow(new IOException("disk full"));

                assertEquals("disk full", assertThrows(IOException.class, session::save).getMessage());
                assertArrayEquals(previous, Files.readAllBytes(settings));
            }

            session.save();

            Properties saved = AtomicProperties.load(settings);
            assertEquals("A + 1", saved.getProperty("watch.1"));
            assertEquals("after", saved.getProperty("annotation.0.label"));
            assertEquals("CODE", saved.getProperty("annotation.0.kind"));
        }
    }

    @Test
    public void sessionWithoutConfigurationPathKeepsEditableMetadataInMemory() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            session.update(() -> session.watches.add("unknown + 1")).join();
            session.annotations().add(new UserAnnotations.Entry(0, "temporary", "DATA"));

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
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings)) {
            session.annotations().add(new UserAnnotations.Entry(1, "buffer", "DATA"));
            session.save();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2);
        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings)) {
            assertEquals("buffer", session.annotations().get(1).label);
            verify(reopened.memory, never()).read(anyInt());
        }
        DebuggerTestMachine otherLayout = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3);
        try (DebuggerSession session = new DebuggerSession(otherLayout.cpu, net.emustudio.application.TestCpu.context(otherLayout.cpu), otherLayout.memory, settings)) {
            assertEquals("buffer", session.annotations().get(1).label);
        }
    }

    @Test
    public void metadataLoadRetriesConcurrentChangesAndPreservesLiveUserEdits() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("concurrent-debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3);
        try (DebuggerSession session = new DebuggerSession(original.cpu, net.emustudio.application.TestCpu.context(original.cpu), original.memory, settings)) {
            session.annotations().add(new UserAnnotations.Entry(0, "disk old", "CODE"));
            session.annotations().add(new UserAnnotations.Entry(1, "disk retained", "DATA"));
            session.save();
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

        try (DebuggerSession session = new DebuggerSession(reopened.cpu, net.emustudio.application.TestCpu.context(reopened.cpu), reopened.memory, settings, engine)) {
            UserAnnotations annotations = session.annotations();

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
        return session.history().stream().map(event -> event.view).collect(java.util.stream.Collectors.toList());
    }
}
