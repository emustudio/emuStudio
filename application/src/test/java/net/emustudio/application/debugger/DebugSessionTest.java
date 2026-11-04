/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
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
            session.setBreakpoint(7, true);
            assertTrue(index.snapshot().hasExecutionBreakpoint(7));
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.beforeInstruction(6));
            assertTrue(session.beforeInstruction(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(7, hit.address);
            machine.state(CPU.RunState.STATE_RUNNING);
            assertFalse(session.beforeInstruction(7));
            assertTrue(session.beforeInstruction(7));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            session.setBreakpoint(7, false);
            assertFalse(index.snapshot().hasExecutionBreakpoint(7));
            assertThrows(IllegalStateException.class, session::liveMemory);
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
    public void executionInvalidatesFlowEvenWithoutMemoryNotificationsAndOldSnapshotStaysImmutable() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            ProgramIndex initial = session.programIndex();
            assertSame(initial, session.annotations().programIndex());
            session.annotations().put(new AnnotationIndex.Entry(1, "user", "DATA"));
            machine.memoryChanged(1);
            assertEquals("user", session.annotations().get(1).label);
            assertSame(initial, session.programIndex());
            verify(machine.memory, never()).read(anyInt());
            verify(machine.memory, never()).read(anyInt());
        }
    }

    @Test
    public void liveCellReadDoesNotHoldSessionMonitorAndRejectsAnObservedWrite() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
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
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            Future<FlowAnalysis> result = executor.submit(() -> session.analyzeFlow(0, 10));
            await(entered);
            executor.submit(session::requirePaused).get(2, TimeUnit.SECONDS);
            session.annotations().put(new AnnotationIndex.Entry(1, "new", "DATA"));
            release.countDown();
            ExecutionException rejected = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
            assertTrue(rejected.getCause() instanceof IllegalStateException);
            assertEquals(ProgramIndex.FlowState.NOT_BUILT, session.programIndex().snapshot().flowState());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    public void flowUsesCpuWiredDecoderAndLiveOperands() throws Exception {
        byte[][] cells = {{1, 0}, {7, 0}};
        DebuggerTestMachine machine = new DebuggerTestMachine(cells, 1, false);
        Disassembler decoder = mock(Disassembler.class);
        when(machine.cpu.getDisassembler()).thenReturn(decoder);
        stopAtZero(decoder);
        when(decoder.disassemble(0)).thenAnswer(call -> new DisassembledInstruction(0,
                "VALUE " + cells[1][0], ""));

        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            assertEquals("VALUE 7", session.analyzeFlow(0, 10).mnemonic(0));

            cells[1][0] = 8;
            machine.memoryChanged(0);

            assertEquals("VALUE 8", session.analyzeFlow(0, 10).mnemonic(0));
        }
    }

    @Test
    public void lateFlowCannotPublishAfterMemoryExecutionOrDisposalChanges() throws Exception {
        for (int change = 0; change < 3; change++) {
            DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
            Disassembler decoder = mock(Disassembler.class);
            when(machine.cpu.getDisassembler()).thenReturn(decoder);
            stopAtZero(decoder);
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            when(decoder.getFlow(0)).thenAnswer(call -> {
                entered.countDown(); await(release);
                return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of()));
            });
            ExecutorService executor = Executors.newSingleThreadExecutor();
            DebugSession session = new DebugSession(machine.cpu, machine.memory, null);
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {1, 0}}, 1, false);
        Disassembler decoder = mock(Disassembler.class);
        when(machine.cpu.getDisassembler()).thenReturn(decoder);
        stopAtZero(decoder);
        when(decoder.getFlow(0)).thenAnswer(call -> {
            machine.memoryChanged(0);
            return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of()));
        });
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
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
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            machine.state(CPU.RunState.STATE_RUNNING);
            assertThrows(IllegalStateException.class, session::requirePaused);
            assertThrows(IllegalStateException.class, session::liveMemory);
            assertThrows(IllegalStateException.class, () -> session.record(2));

            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(1, session.liveMemory().value(0));
            assertEquals(2, session.liveMemory().value(1));
        }
    }

    @Test
    public void retainedLiveReaderRejectsMemoryMetadataGenerationAndDisposalChanges() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        DebugSession session = new DebugSession(machine.cpu, machine.memory, null);
        boolean disposed = false;
        try {
            LiveMemory first = session.liveMemory();
            machine.memoryChanged(1);
            assertThrows(IllegalStateException.class, () -> first.value(0));
            LiveMemory second = session.liveMemory();
            session.annotations().put(new AnnotationIndex.Entry(1, "changed", "UNKNOWN"));
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
    public void writeWatchUsesGuestAccessAddressInsteadOfInstructionLocation() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.AccessRule watch = new DebugSession.AccessRule("buffer", "WRITE", 1, "", 1);
            session.rules.add(watch);
            assertFalse(session.afterInstruction(instruction(1, List.of(new DebugCPU.Access(0, 5, true)))));
            assertFalse(session.afterInstruction(instruction(2, List.of(new DebugCPU.Access(1, 5, false)))));

            assertTrue(session.afterInstruction(instruction(3, List.of(new DebugCPU.Access(1, 5, true)))));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertSame(watch, hit.rule);
            assertEquals("Watchpoint: buffer", session.status);
        }
    }

    @Test
    public void readWatchIgnoresWrites() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.rules.add(new DebugSession.AccessRule("fetch", "READ", 0, "", 1));
            assertFalse(session.afterInstruction(instruction(1, List.of(new DebugCPU.Access(0, 3, true)))));
            assertTrue(session.afterInstruction(instruction(2, List.of(new DebugCPU.Access(0, 3, false)))));
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
            assertTrue(session.beforeInstruction(1));
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
            DebugSession.AccessRule watch = new DebugSession.AccessRule("buffer writes", "WRITE", 1, "A > 3", 4);
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
    public void obsoleteSettingsArePreservedAndDoNotBlockStartup() throws Exception {
        assertIncompatibleSettingsStartClean("1", "Test CPU");
    }

    @Test
    public void settingsForAnotherCpuArePreservedAndDoNotBlockStartup() throws Exception {
        assertIncompatibleSettingsStartClean("2", "another CPU");
    }

    private void assertIncompatibleSettingsStartClean(String version, String cpuTitle) throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, true);
        Properties original = new Properties();
        original.setProperty("version", version);
        original.setProperty("cpu", cpuTitle);
        original.setProperty("watches", "1");
        original.setProperty("watch.0", "A + 1");
        AtomicProperties.save(settings, original);
        byte[] saved = Files.readAllBytes(settings);

        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, settings)) {
            assertTrue(session.watches.isEmpty());
            assertTrue(session.rules.isEmpty());
            assertTrue(session.ordinaryBreakpoints().isEmpty());
            assertFalse(Files.exists(settings));
            session.watches.add("A + 2");
        }

        try (var paths = Files.list(settings.getParent())) {
            List<Path> archives = paths.filter(path -> path.getFileName().toString()
                    .startsWith("debug.properties.incompatible-")).collect(java.util.stream.Collectors.toList());
            assertEquals(1, archives.size());
            assertArrayEquals(saved, Files.readAllBytes(archives.get(0)));
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, true);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of("A + 2"), session.watches);
        }
    }

    @Test
    public void portableDisabledBreakpointSurvivesSettingsRoundTrip() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("portable.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            session.setBreakpoint(1, true);
            session.toggleOrdinaryBreakpoint(1);
            session.save();
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of(1), session.ordinaryBreakpoints());
            assertFalse(session.isBreakpointSet(1));
            session.toggleOrdinaryBreakpoint(1);
            assertTrue(session.isBreakpointSet(1));
            verify(reopened.cpu, never()).setBreakpoint(anyInt());
        }
    }

    @Test
    public void ruleKindsAndOptionsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Rule("read", "READ", 0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Rule("source", "SOURCE", 0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Rule("conditional", "EXEC", 0, "A == 1", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.Rule("hit count", "EXEC", 0, "", 2));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.AccessRule("exec", "EXEC", 0, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new DebugSession.AccessRule("read", "READ", 0, "", 0));
    }

    @Test
    public void malformedSettingsDoNotApplyEarlierBreakpointEntries() throws Exception {
        Path settings = temporary.newFile("invalid.properties").toPath();
        Properties values = new Properties();
        values.setProperty("version", "2");
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
    public void metadataPersistsForComputerLayoutWithoutDependingOnCellContents() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2}}, 2, false);
        String identity;
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            identity = session.programIndex().snapshot().layout().imageId();
            session.annotations().put(new AnnotationIndex.Entry(1, "buffer", "DATA"));
            session.saveAnnotations();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{9, 8}}, 2, false);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(identity, session.programIndex().snapshot().layout().imageId());
            assertEquals("buffer", session.annotations().get(1).label);
            verify(reopened.memory, never()).read(anyInt());
        }
        DebuggerTestMachine otherLayout = new DebuggerTestMachine(new byte[][] {{9, 8, 7}}, 3, false);
        try (DebugSession session = new DebugSession(otherLayout.cpu, otherLayout.memory, settings)) {
            assertNotEquals(identity, session.programIndex().snapshot().layout().imageId());
            assertTrue(session.annotations().entries().isEmpty());
        }
    }

    @Test
    public void metadataLoadRetriesConcurrentChangesAndPreservesLiveUserEdits() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("concurrent-debug.properties");
        DebuggerTestMachine original = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3, false);
        long editedId;
        try (DebugSession session = new DebugSession(original.cpu, original.memory, settings)) {
            editedId = session.annotations().add(new AnnotationIndex.Entry(0, "disk old", "CODE")).id;
            session.annotations().add(new AnnotationIndex.Entry(1, "disk retained", "DATA"));
            session.saveAnnotations();
        }
        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{1, 2, 3}}, 3, false);
        net.emustudio.emulib.runtime.annotations.AnnotationEngine engine = spy(
                new net.emustudio.emulib.runtime.annotations.AnnotationEngine());
        AtomicBoolean firstRevision = new AtomicBoolean(true);
        doAnswer(call -> {
            long revision = (long) call.callRealMethod();
            if (firstRevision.getAndSet(false)) {
                engine.put(new net.emustudio.emulib.runtime.annotations.AnnotationEngine.Entry(
                        AnnotationIndex.OWNER, editedId, 0, 1, AnnotationIndex.USER_KIND + "CODE", "live edit", null));
                engine.put(new net.emustudio.emulib.runtime.annotations.AnnotationEngine.Entry(
                        17, 1, 2, 3, "source", "", null));
            }
            return revision;
        }).when(engine).revision();

        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings, engine)) {
            AnnotationIndex annotations = session.annotations();

            assertEquals("live edit", annotations.get(0).label);
            assertEquals("disk retained", annotations.get(1).label);
            assertNotNull(engine.get(17, 1));
        }
    }

    private static DebugCPU.Instruction instruction(long sequence, List<DebugCPU.Access> accesses) {
        return new DebugCPU.Instruction(sequence, 0, "NOP", "00", Map.of(), Map.of(), accesses, true);
    }
}
