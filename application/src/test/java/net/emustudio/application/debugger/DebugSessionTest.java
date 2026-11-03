/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebugSessionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

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
    public void executionRuleCountsOnlyMatchingBankAndConditionThenSkipsOneResume() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 2, false);
        when(machine.debug.readRegister("A")).thenReturn(1L);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule rule = new DebugSession.Rule("loop", "EXEC", 1, 1, "A == 1", 2);
            session.rules.add(rule);
            assertFalse(session.beforeInstruction(1, 0));
            assertFalse(session.beforeInstruction(1, 1));
            assertEquals(1, rule.hits);

            assertTrue(session.beforeInstruction(1, 1));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertEquals(1, hit.bank);
            assertSame(rule, hit.rule);
            assertNull(session.takeBreakpointHit());
            assertFalse("Resume executes the instruction that just stopped", session.beforeInstruction(1, 1));
            assertTrue("Later visits can stop again", session.beforeInstruction(1, 1));
        }
    }

    @Test
    public void disabledAndFalseConditionRulesDoNotCountHits() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule disabled = new DebugSession.Rule("disabled", "EXEC", 0, -1, "", 1);
            disabled.enabled = false;
            DebugSession.Rule falseCondition = new DebugSession.Rule("false", "EXEC", 0, -1, "0", 1);
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
            session.rules.add(new DebugSession.Rule("invalid", "EXEC", 0, -1, "1 / 0", 1));

            assertTrue(session.beforeInstruction(0, 0));
            assertTrue(session.status.startsWith("Breakpoint evaluation failed:"));
        }
    }

    @Test
    public void writeWatchUsesGuestAccessAddressAndBankInsteadOfInstructionLocation() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}, {0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            DebugSession.Rule watch = new DebugSession.Rule("buffer", "WRITE", 1, 1, "", 1);
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
            session.rules.add(new DebugSession.Rule("fetch", "READ", 0, -1, "", 1));
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
    public void nativeBreakpointToggleRetainsDisabledAddressAndInspectionPreference() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3, true);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpointState(2, BreakpointState.KEEP_INSPECTION);
            assertEquals(BreakpointState.KEEP_INSPECTION, session.breakpointState(2));

            session.toggleOrdinaryBreakpoint(2);
            assertEquals(BreakpointState.OFF, session.breakpointState(2));
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertTrue(session.ordinaryBreakpointKeepsInspection(2));

            session.toggleOrdinaryBreakpoint(2);
            assertEquals(BreakpointState.KEEP_INSPECTION, session.breakpointState(2));
            session.setBreakpointState(2, BreakpointState.OFF);
            assertTrue(session.ordinaryBreakpoints().isEmpty());
        }
    }

    @Test
    public void portableBreakpointReusesRuleAndControlsProbeAttachment() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, false);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpointState(1, BreakpointState.ON);
            session.setBreakpointState(1, BreakpointState.KEEP_INSPECTION);
            assertEquals(1, session.rules.size());
            assertEquals(BreakpointState.KEEP_INSPECTION, session.breakpointState(1));
            assertTrue(session.beforeInstruction(1, 0));
            assertTrue(session.keepsInspection(session.takeBreakpointHit()));

            session.setBreakpointState(1, BreakpointState.OFF);
            assertTrue(session.rules.isEmpty());
            verify(machine.debug).addListener(session);
            verify(machine.debug).removeListener(session);
        }
    }

    @Test
    public void nativeCpuStopCreatesInspectionHitAndResumeClearsIt() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0, 0}}, 2, true);
        when(machine.cpu.getInstructionLocation()).thenReturn(1);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.setBreakpointState(1, BreakpointState.KEEP_INSPECTION);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(1, hit.address);
            assertTrue(session.keepsInspection(hit));

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
            DebugSession.Rule watch = new DebugSession.Rule("buffer writes", "WRITE", 1, 0, "A > 3", 4);
            watch.enabled = false;
            watch.keepInspection = true;
            session.rules.add(watch);
            session.setBreakpointState(2, BreakpointState.KEEP_INSPECTION);
            session.toggleOrdinaryBreakpoint(2);
            session.save();
        }

        DebuggerTestMachine reopened = new DebuggerTestMachine(new byte[][] {{0, 0, 0}}, 3, true);
        try (DebugSession session = new DebugSession(reopened.cpu, reopened.memory, settings)) {
            assertEquals(List.of("A + 1"), session.watches);
            assertEquals(1, session.rules.size());
            DebugSession.Rule watch = session.rules.get(0);
            assertEquals("buffer writes", watch.name);
            assertEquals("WRITE", watch.kind);
            assertEquals(1, watch.address);
            assertEquals(0, watch.bank);
            assertEquals("A > 3", watch.condition);
            assertEquals(4, watch.afterHits);
            assertFalse(watch.enabled);
            assertTrue(watch.keepInspection);
            assertEquals(List.of(2), session.ordinaryBreakpoints());
            assertFalse(reopened.cpu.isBreakpointSet(2));
            session.toggleOrdinaryBreakpoint(2);
            assertEquals(BreakpointState.KEEP_INSPECTION, session.breakpointState(2));
        }
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
