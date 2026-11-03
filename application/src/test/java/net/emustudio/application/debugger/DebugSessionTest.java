/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
    public void memorylessPortableCpuUsesIndexedAddressBreakpointsAndResumeSkipping() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[32]}, 32, false);
        doReturn(null).when(machine.debug).memory();
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            assertNull(session.memory);
            ProgramIndex index = session.programIndex();
            assertEquals(32, index.snapshot().layout().addressSpaceSize());
            assertEquals(1, index.snapshot().layout().bankCount());
            assertEquals(0, index.snapshot().layout().commonBoundary());
            session.setBreakpoint(7, true);
            assertTrue(index.snapshot().hasExecutionBreakpoint(7));
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.beforeInstruction(6, 9));
            assertTrue(session.beforeInstruction(7, 9));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(7, hit.address);
            assertEquals(9, hit.bank);
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.beforeInstruction(7, 2));
            assertTrue(session.beforeInstruction(7, 5));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(7, false);
            assertFalse(index.snapshot().hasExecutionBreakpoint(7));
            assertThrows(IllegalStateException.class, session::image);
        }
        verifyNoInteractions(machine.memory);
    }

    @Test
    public void executionStartMirrorsNativeBreakpointsSetThroughCpuApi() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, true);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            machine.cpu.setBreakpoint(1);
            machine.state(CPU.RunState.STATE_RUNNING);
            assertTrue(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.cpu.unsetBreakpoint(1);
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void deferredSourceCaptureBelongsToCompletedCompileMutationOnly() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
    public void addressOnlyRulesAndMultipleCandidatesUsePublishedExecutionState() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule disabled = new DebugSession.Rule("disabled", "EXEC", 1, "", 1);
            disabled.enabled = false;
            DebugSession.Rule falseCondition = new DebugSession.Rule("conditional", "EXEC", 1, "0", 1);
            DebugSession.Rule enabled = new DebugSession.Rule("enabled", "EXEC", 1, "", 1);
            session.rules.addAll(List.of(disabled, falseCondition, enabled));
            session.updateProbe();
            machine.state(CPU.RunState.STATE_RUNNING);
            assertTrue(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            assertTrue(session.beforeInstruction(1, 0));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertSame(enabled, session.takeBreakpointHit().rule);
            session.rules.remove(enabled);
            session.updateProbe();
            assertTrue("Remaining conditional rule retains presence", session.programIndex().snapshot().hasExecutionBreakpoint(1));
            session.rules.remove(falseCondition);
            session.updateProbe();
            assertFalse(session.programIndex().snapshot().hasExecutionBreakpoint(1));
            disabled.enabled = true;
            session.updateProbe();
            assertTrue(session.programIndex().snapshot().hasExecutionBreakpoint(1));
        }
    }

    @Test
    public void executionInvalidatesFlowEvenWithoutMemoryNotificationsAndOldSnapshotStaysImmutable() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            ProgramIndex index = session.programIndex();
            try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
                update.replaceFlow(false).putInstruction(new ProgramIndex.Range(0, 0, 2), ProgramIndex.InstructionKind.LINEAR, false);
                update.commit();
            }
            ProgramIndex.Snapshot old = index.snapshot();
            session.runExecution(() -> machine.state(CPU.RunState.STATE_RUNNING));
            assertEquals(ProgramIndex.FlowState.STALE, index.snapshot().flowState());
            assertNull(index.snapshot().instructionAt(0, 0));
            assertEquals(ProgramIndex.FlowState.CURRENT, old.flowState());
            assertNotNull(old.instructionAt(0, 0));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            ProgramIndex.Snapshot beforeWrite = index.snapshot();
            machine.memoryChanged(1);
            assertTrue(index.snapshot().memoryRevision() > beforeWrite.memoryRevision());
        }
    }

    @Test
    public void initialCaptureKeepsSharedIndexButExplicitReplacementCreatesNewInstance() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            ProgramIndex initial = session.programIndex();
            session.installImage(session.imageCapture());
            assertSame(initial, session.programIndex());
            assertSame(initial, session.annotations().programIndex());
            DebugSession.ImageCapture replacement = session.captureImage();
            session.installImage(replacement);
            assertNotSame(initial, session.programIndex());
            assertSame(session.programIndex(), session.annotations().programIndex());
            assertThrows(IllegalStateException.class, () -> session.installImage(replacement));
        }
    }

    @Test
    public void metadataChangeRejectsCapturedImageWithoutPublishingDetachedAnnotations() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            ProgramIndex index = session.programIndex();
            DebugSession.ImageCapture captured = session.captureImage();
            captured.annotations.put(new AnnotationIndex.Entry(0, 0, "detached", "", false, "CODE"));
            try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
                edit.putAnnotation(9, 1, new ProgramIndex.Range(0, 0, 1), "comment", "keep").commit();
            }
            assertThrows(IllegalStateException.class, () -> session.installImage(captured));
            assertSame(index, session.programIndex());
            java.util.ArrayList<ProgramIndex.Annotation> current = new java.util.ArrayList<>();
            index.snapshot().forEachAnnotation(current::add);
            assertEquals(1, current.size());
            assertEquals("keep", current.get(0).text());
        }
    }

    @Test
    public void rawWriteDuringInitialImageInstallationRejectsCapturedAnnotationsWithoutReplay() throws Exception {
        rawWriteDuringImageInstallation(false);
    }

    @Test
    public void rawWriteDuringReplacementInstallationPreventsAdoptingDetachedIndex() throws Exception {
        rawWriteDuringImageInstallation(true);
    }

    private void rawWriteDuringImageInstallation(boolean replacing) throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            CountDownLatch staged = new CountDownLatch(1), invalidate = new CountDownLatch(1), resume = new CountDownLatch(1);
            AtomicBoolean armed = new AtomicBoolean();
            Runnable pauseStaging = () -> {
                if (armed.get()) { staged.countDown(); await(resume); }
            };
            ProgramIndex live = interceptIndex(session.programIndex(), replacing ? () -> {} : pauseStaging, invalidate::countDown);
            java.lang.reflect.Field field = DebugSession.class.getDeclaredField("programIndex");
            field.setAccessible(true);
            field.set(session, live);
            DebugSession.ImageCapture capture = replacing ? session.captureImage() : session.imageCapture();
            capture.annotations.put(new AnnotationIndex.Entry(0, 0, "must stay detached", "", false, "CODE"));
            if (replacing) capture = new DebugSession.ImageCapture(capture.image, capture.annotations, capture.generation,
                    interceptIndex(capture.targetIndex, pauseStaging, () -> {}), capture.base);
            DebugSession.ImageCapture captured = capture;
            armed.set(true);
            Future<AnnotationIndex> installation = executor.submit(() -> session.installImage(captured));
            await(staged);
            Future<?> write = executor.submit(() -> machine.memoryChanged(1));
            // The callback advances memory revision while installation still holds the session monitor.
            await(invalidate);
            resume.countDown();
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> installation.get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof IllegalStateException);
            write.get(5, TimeUnit.SECONDS);
            assertSame(live, session.programIndex());
            live.snapshot().forEachAnnotation(annotation -> fail("Rejected capture published " + annotation.text()));
        } finally { executor.shutdownNow(); }
    }

    private static ProgramIndex interceptIndex(ProgramIndex delegate, Runnable beforeStaging, Runnable invalidated) {
        ProgramIndex index = mock(ProgramIndex.class);
        when(index.snapshot()).thenAnswer(call -> delegate.snapshot());
        when(index.updateContent(any())).thenAnswer(call -> {
            beforeStaging.run();
            return delegate.updateContent(call.getArgument(0));
        });
        when(index.updateBreakpoints(any())).thenAnswer(call -> delegate.updateBreakpoints(call.getArgument(0)));
        doAnswer(call -> { delegate.memoryChanged(); invalidated.run(); return null; }).when(index).memoryChanged();
        return index;
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue("Barrier timed out", latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @Test
    public void runningSessionRejectsPausedOperationsAndResumesAfterStop() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            machine.state(CPU.RunState.STATE_RUNNING);
            assertThrows(IllegalStateException.class, session::requirePaused);
            assertThrows(IllegalStateException.class, session::image);
            assertThrows(IllegalStateException.class, () -> session.record(2));

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertArrayEquals(new byte[] {1, 2}, session.image().bytes(0));
        }
    }

    @Test
    public void memoryChangeDiscardsCapturedResultBeforeItCanBeApplied() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.ImageCapture captured = session.captureImage();
            machine.memoryChanged(1);

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> session.installImage(captured));
            assertTrue(failure.getMessage().contains("machine changed"));
        }
    }

    @Test
    public void resumeAndPauseDiscardAnEarlierPausedGeneration() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        DebugSession session = new DebugSession(machine.cpu, machine.memory, null);
        session.record(2);
        machine.state(CPU.RunState.STATE_RUNNING);

        session.close();

        verify(machine.cpu).pause();
        verify(machine.debug).removeListener(session);
        assertTrue(machine.cpuListeners.isEmpty());
        assertTrue(machine.memoryListeners.isEmpty());
        assertThrows(IllegalStateException.class, session::requirePaused);
    }

    @Test
    public void executionRuleMatchesAddressInEveryBankAndSkipsOneResumeAcrossBanks() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 2, false);
        when(machine.debug.readRegister("A")).thenReturn(1L);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule rule = new DebugSession.Rule("loop", "EXEC", 1, "A == 1", 2);
            session.rules.add(rule);
            assertFalse(session.beforeInstruction(0, 0));
            assertFalse(session.beforeInstruction(1, 0));
            assertEquals(1, rule.hits);

            assertTrue(session.beforeInstruction(1, 1));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertEquals(1, hit.bank);
            assertSame(rule, hit.rule);
            assertNull(session.takeBreakpointHit());
            assertFalse("Resume is address-only even after a bank change", session.beforeInstruction(1, 0));
            assertTrue("Later visits can stop again", session.beforeInstruction(1, 1));
        }
    }

    @Test
    public void disabledAndFalseConditionRulesDoNotCountHits() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule disabled = new DebugSession.Rule("disabled", "EXEC", 0, "", 1);
            disabled.enabled = false;
            DebugSession.Rule falseCondition = new DebugSession.Rule("false", "EXEC", 0, "0", 1);
            session.rules.addAll(List.of(disabled, falseCondition));

            assertFalse(session.beforeInstruction(0, 0));
            assertEquals(0, disabled.hits);
            assertEquals(0, falseCondition.hits);
            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void conditionFailureStopsExecutionWithDiagnostic() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.rules.add(new DebugSession.Rule("invalid", "EXEC", 0, "1 / 0", 1));

            assertTrue(session.beforeInstruction(0, 0));
            assertTrue(session.status.startsWith("Breakpoint evaluation failed:"));
        }
    }

    @Test
    public void writeWatchUsesGuestAccessAddressAndBankInsteadOfInstructionLocation() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.AccessRule watch = new DebugSession.AccessRule("buffer", "WRITE", 1, 1, "", 1);
            session.rules.add(watch);
            assertFalse(session.afterInstruction(instruction(1, List.of(new DebugCPU.Access(1, 0, 5, true)))));
            assertFalse(session.afterInstruction(instruction(2, List.of(new DebugCPU.Access(1, 1, 5, false)))));

            assertTrue(session.afterInstruction(instruction(3, List.of(new DebugCPU.Access(1, 1, 5, true)))));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertEquals(1, hit.bank);
            assertSame(watch, hit.rule);
            assertEquals("Watchpoint: buffer", session.status);
        }
    }

    @Test
    public void readWatchWithAnyBankIgnoresWrites() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.rules.add(new DebugSession.AccessRule("fetch", "READ", 0, -1, "", 1));
            assertFalse(session.afterInstruction(instruction(1, List.of(new DebugCPU.Access(0, 0, 3, true)))));
            assertTrue(session.afterInstruction(instruction(2, List.of(new DebugCPU.Access(0, 0, 3, false)))));
        }
    }

    @Test
    public void boundedHistoryKeepsNewestInstructionsAndDisablingStopsCapture() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        DebugCPU.Instruction first = instruction(1, List.of());
        DebugCPU.Instruction second = instruction(2, List.of());
        DebugCPU.Instruction third = instruction(3, List.of());
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.record(2);
            session.afterInstruction(first);
            session.afterInstruction(second);
            session.afterInstruction(third);
            assertEquals(List.of(second, third), session.history());
            assertThrows(UnsupportedOperationException.class, () -> session.history().clear());

            session.record(0);
            session.afterInstruction(instruction(4, List.of()));
            assertFalse(session.isRecording());
            assertEquals(List.of(second, third), session.history());
            verify(machine.debug).addListener(session);
            verify(machine.debug).removeListener(session);
        }
    }

    @Test
    public void historyRequiresDebuggerCapabilityAndValidLimit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        when(machine.cpu.getDebugger()).thenReturn(Optional.empty());
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            assertThrows(IllegalStateException.class, () -> session.record(1));
            assertThrows(IllegalArgumentException.class, () -> session.record(-1));
            assertThrows(IllegalStateException.class, session::recordingHeader);
            assertFalse(session.isRecording());
        }
    }

    @Test
    public void nativeBreakpointToggleRetainsDisabledAddress() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3, true);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpoint(2, true);
            assertTrue(session.isBreakpointSet(2));

            session.toggleOrdinaryBreakpoint(2);
            assertFalse(session.isBreakpointSet(2));
            assertEquals(List.of(2), session.ordinaryBreakpoints());

            session.toggleOrdinaryBreakpoint(2);
            assertTrue(session.isBreakpointSet(2));
            session.setBreakpoint(2, false);
            assertTrue(session.ordinaryBreakpoints().isEmpty());
        }
    }

    @Test
    public void portableBreakpointReusesRuleAndControlsProbeAttachment() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpoint(1, true);
            DebugSession.Rule rule = session.rules.get(0);
            rule.enabled = false;
            session.updateProbe();
            assertFalse(session.isBreakpointSet(1));
            session.setBreakpoint(1, true);
            assertEquals(1, session.rules.size());
            assertSame(rule, session.rules.get(0));
            assertTrue(session.isBreakpointSet(1));
            assertTrue(session.beforeInstruction(1, 0));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertSame(session.rules.get(0), hit.rule);

            session.setBreakpoint(1, false);
            assertTrue(session.rules.isEmpty());
            verify(machine.debug, times(2)).addListener(session);
            verify(machine.debug, times(2)).removeListener(session);
        }
    }

    @Test
    public void nativeCpuStopCreatesInspectionHitAndResumeClearsIt() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, true);
        when(machine.cpu.getInstructionLocation()).thenReturn(1);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpoint(1, true);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertEquals(0, hit.bank);
            assertNull(hit.rule);

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            machine.state(CPU.RunState.STATE_RUNNING);
            machine.state(CPU.RunState.STATE_STOPPED_NORMAL);
            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void settingsRoundTripWatchesRulesAndDisabledNativeBreakpoints() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3, true);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.watches.add("A + 1");
            DebugSession.AccessRule watch = new DebugSession.AccessRule("buffer writes", "WRITE", 1, 0, "A > 3", 4);
            watch.enabled = false;
            session.rules.add(watch);
            session.setBreakpoint(2, true);
            session.toggleOrdinaryBreakpoint(2);
            session.save();
        }

        Properties saved = AtomicProperties.load(settings);
        assertTrue(saved.stringPropertyNames().stream().noneMatch(key -> key.contains("keepInspection")));
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3, true);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals(1, session.rules.size());
            assertTrue(session.rules.get(0) instanceof DebugSession.AccessRule);
            DebugSession.AccessRule watch = (DebugSession.AccessRule) session.rules.get(0);
            assertEquals("buffer writes", watch.name);
            assertEquals("WRITE", watch.kind);
            assertEquals(1, watch.address);
            assertEquals(0, watch.bank);
            assertEquals("A > 3", watch.condition);
            assertEquals(4, watch.afterHits);
            assertFalse(watch.enabled);
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertFalse(reopened.cpu.isBreakpointSet(2));
            session.toggleOrdinaryBreakpoint(2);
            assertTrue(session.isBreakpointSet(2));
        }
    }

    @Test
    public void executionAndSourcePersistenceHaveNoBankWhileWatchpointsKeepRequiredBank() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("address-only.properties");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, settings)) {
            session.rules.add(new DebugSession.Rule("execute", "EXEC", 1, "", 1));
            DebugSession.Rule source = new DebugSession.Rule("source", "SOURCE", 2, "", 1);
            source.source = temporary.getRoot().toPath().resolve("program.asm").toString();
            session.rules.add(source);
            session.rules.add(new DebugSession.AccessRule("write", "WRITE", 0, 1, "", 1));
            session.save();
        }
        Properties saved = AtomicProperties.load(settings);
        assertFalse(saved.containsKey("rule.0.bank"));
        assertFalse(saved.containsKey("rule.1.bank"));
        assertEquals("1", saved.getProperty("rule.2.bank"));
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 1, false);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(3, session.rules.size());
            assertFalse(session.rules.get(0) instanceof DebugSession.AccessRule);
            assertFalse(session.rules.get(1) instanceof DebugSession.AccessRule);
            assertEquals(1, ((DebugSession.AccessRule) session.rules.get(2)).bank);
        }
        saved.remove("rule.2.bank");
        AtomicProperties.save(settings, saved);
        DebuggerTestMachine invalid = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 1, false);
        assertThrows(IOException.class, () -> new DebugSession(invalid.cpu, invalid.memory, settings));
    }

    @Test
    public void onlyAccessRulesCanCarryWatchpointKindsAndBankSelectors() {
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Rule("read", "READ", 0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.AccessRule("exec", "EXEC", 0, 0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.AccessRule("read", "READ", 0, -2, "", 1));
    }

    @Test
    public void malformedSettingsDoNotApplyEarlierBreakpointEntries() throws Exception {
        Path settings = temporary.newFile("invalid.properties").toPath();
        Properties values = new Properties();
        values.setProperty("version", "1");
        values.setProperty("cpu", "Test CPU");
        values.setProperty("ordinary", "2");
        values.setProperty("ordinary.0", "0");
        values.setProperty("ordinary.1", "99");
        AtomicProperties.save(settings, values);
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, true);

        IOException failure = assertThrows(IOException.class,
                () -> new DebugSession(machine.cpu, machine.memory, settings));

        assertEquals("Invalid debugger settings", failure.getMessage());
        verify(machine.cpu, never()).setBreakpoint(anyInt());
        assertTrue(machine.cpuListeners.isEmpty());
    }

    @Test
    public void annotationsReloadOnlyForMatchingCapturedImage() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        byte[][] bytes = {{1, 2}};
        DebuggerTestMachine machine = new DebuggerTestMachine(bytes, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, settings)) {
            session.annotations().put(new AnnotationIndex.Entry(0, 1, "buffer", "payload", true, "DATA"));
            session.saveAnnotations();
            session.replaceImage();
            assertEquals("buffer", session.annotations().get(0, 1).label);

            bytes[0][1] = 3;
            machine.memoryChanged(1);
            session.replaceImage();
            assertNull(session.annotations().get(0, 1));
            assertEquals(3, session.image().value(1, 0));
        }
    }

    private static DebugCPU.Instruction instruction(long sequence, List<DebugCPU.Access> accesses) {
        return new DebugCPU.Instruction(sequence, 0, 0, "NOP", "00", Map.of(), Map.of(), accesses, true);
    }
}
