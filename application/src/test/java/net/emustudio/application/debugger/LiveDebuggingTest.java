/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.debugger.ValueView;
import net.emustudio.emulib.plugins.debugger.StepView;
import net.emustudio.emulib.plugins.debugger.ViewRow;
import org.junit.Test;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LiveDebuggingTest {
    @Test
    public void firstCaptureStopsAtRequestedOrdinalWhenRetentionWindowIsSmaller() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        String text = "x".repeat(65536);
        StepView view = new StepView(text, "wide", OptionalInt.of(0), List.of(
                new ViewRow("State", "Value", text, text, false, List.of(), List.of())));
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.startNewRecording(200, DebugSession.RecordingMode.FIRST, 0, 0)).join();

            for (int i = 0; i < 201; i++) session.afterStep(true, Optional.of(view));

            List<InstructionRecording.Event> retained = session.history();
            assertTrue(retained.size() < 200);
            assertEquals(200L, retained.get(retained.size() - 1).sequence);
            assertTrue(session.recordingError().contains("retention limit"));
        }
    }

    @Test
    public void pausedWatchShowsValueFromCompletedFastStep() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("A")).join();
            when(machine.cpu.step()).thenAnswer(call -> {
                session.beforeStep();
                machine.values.put("A", 7L);
                session.afterStep(true, Optional.empty());
                return CPU.RunState.STATE_STOPPED_BREAK;
            });

            session.execution().step().get(2, TimeUnit.SECONDS);

            assertEquals("7", session.watchValues().get("A").text);
            assertTrue(session.watchValues().get("A").changed);
        }
    }

    @Test
    public void newExecutionClearsPriorWatchChangeIndication() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("A")).join();
            session.update(() -> machine.values.put("A", 7L)).join();
            assertTrue(session.watchValues().get("A").changed);

            machine.state(CPU.RunState.STATE_RUNNING);

            assertFalse(session.watchValues().get("A").changed);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void unrelatedUpdatesDoNotReactivateFailedWatch() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("1 / A")).join();
            assertTrue(session.watchErrors().containsKey("1 / A"));

            session.update(() -> machine.values.put("A", 1L)).join();
            session.refreshWatches().join();

            assertTrue(session.watchErrors().containsKey("1 / A"));
            assertFalse(session.watchValues().containsKey("1 / A"));
        }
    }

    @Test
    public void watchChangeIndicatorSurvivesRefreshAndUnrelatedRuleEdits() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("A")).join();
            assertFalse(session.watchValues().get("A").changed);

            session.update(() -> machine.values.put("A", 7L)).join();
            session.setBreakpoint(0, true).join();
            session.refreshWatches().join();

            assertEquals("7", session.watchValues().get("A").text);
            assertTrue(session.watchValues().get("A").changed);
        }
    }

    @Test
    public void manualStepExecutesPastBreakpointAfterBoundaryEdit() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        java.util.concurrent.atomic.AtomicInteger executed = new java.util.concurrent.atomic.AtomicInteger();
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(0, true).join();
            when(machine.cpu.step()).thenAnswer(call -> {
                session.runMutation(() -> machine.values.put("A", 9L));
                if (!machine.beforeStep(0)) executed.incrementAndGet();
                return CPU.RunState.STATE_STOPPED_BREAK;
            });

            session.execution().step().get(2, TimeUnit.SECONDS);

            assertEquals(1, executed.get());
            assertEquals(9L, ((Number) machine.values.get("A")).longValue());
        }
    }

    @Test
    public void liveChangesUseCpuWorkerWithoutPauseOrRestart() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            machine.state(CPU.RunState.STATE_RUNNING);
            AtomicReference<String> thread = new AtomicReference<>();

            session.update(() -> {
                thread.set(Thread.currentThread().getName());
                machine.values.put("A", 7L);
                session.watches.add("A");
            }).get(2, TimeUnit.SECONDS);
            AtomicReference<ValueView> displayed = new AtomicReference<>();
            session.update(() -> displayed.set(machine.debug.compile("A").display())).get(2, TimeUnit.SECONDS);

            assertEquals("machine-cpu", thread.get());
            assertEquals("7", displayed.get().text);
            assertEquals(CPU.RunState.STATE_RUNNING, session.execution().snapshot().runState());
            verify(machine.cpu, times(1)).execute();
            verify(machine.cpu, never()).step();
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void invalidExpressionTextIsRetainedWhileOtherRulesRemainActive() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            DebugSession.Rule invalid = new DebugSession.Rule("missing", "CONDITION", -1, "unknown + 1", 1);
            DebugSession.Rule empty = new DebugSession.Rule("empty", "CONDITION", -1, "", 1);
            DebugSession.Rule valid = new DebugSession.Rule("valid", "CONDITION", -1, "A == 0", 1);

            session.update(() -> {
                session.rules.add(invalid);
                session.rules.add(empty);
                session.rules.add(valid);
                session.watches.add("unknown + 1");
            }).join();
            assertTrue(session.afterStep(true, Optional.empty()));

            assertEquals("unknown + 1", invalid.condition);
            assertFalse(invalid.error.isEmpty());
            assertEquals(0, invalid.hits);
            assertEquals("", empty.condition);
            assertFalse(empty.isActive());
            assertFalse(empty.error.isEmpty());
            assertEquals(0, empty.hits);
            assertEquals(1, valid.hits);
            assertEquals("unknown + 1", session.watches.get(0));
            assertTrue(session.watchErrors().containsKey("unknown + 1"));
        }
    }

    @Test
    public void runtimeExpressionFailureDoesNotStopAnUnrelatedRule() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            DebugSession.Rule broken = new DebugSession.Rule("broken", "CONDITION", -1, "1 / A", 1);
            DebugSession.Rule valid = new DebugSession.Rule("valid", "CONDITION", -1, "A == 0", 2);
            session.update(() -> { session.rules.add(broken); session.rules.add(valid); }).join();

            assertFalse(session.afterStep(true, Optional.empty()));
            assertTrue(session.afterStep(true, Optional.empty()));

            assertFalse(broken.error.isEmpty());
            assertEquals(0, broken.hits);
            assertEquals(2, valid.hits);
        }
    }

    @Test
    public void correctionReplacesInvalidWatchAndClearsItsDiagnostic() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.update(() -> session.watches.add("unknown")).join();
            session.afterStep(true, Optional.empty());
            assertTrue(session.watchErrors().containsKey("unknown"));

            session.update(() -> { session.watches.clear(); session.watches.add("A + 1"); }).join();
            session.afterStep(true, Optional.empty());

            assertTrue(session.watchErrors().isEmpty());
            assertEquals("1", session.watchValues().get("A + 1").text);
        }
    }

    @Test
    public void overlappingBreakpointsAllCountAtTheSameBoundary() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            DebugSession.Rule first = new DebugSession.Rule("first", "EXEC", 0, "", 1);
            DebugSession.Rule second = new DebugSession.Rule("second", "EXEC", 0, "", 1);
            session.update(() -> { session.rules.add(first); session.rules.add(second); }).join();

            assertTrue(machine.beforeStep(0));

            assertEquals(1, first.hits);
            assertEquals(1, second.hits);
        }
    }

    @Test
    public void machineEditClearsBreakpointContinuationAllowance() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebugSession session = machine.session(null)) {
            session.setBreakpoint(0, true).join();
            assertTrue(machine.beforeStep(0));
            assertFalse(machine.beforeStep(0));
            assertTrue(machine.beforeStep(0));

            session.execution().update(() -> machine.values.put("A", 9L)).join();

            assertTrue(machine.beforeStep(0));
            assertEquals(3, session.rules.get(0).hits);
        }
    }

}
