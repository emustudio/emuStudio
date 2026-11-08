/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static net.emustudio.application.debugger.index.ProgramIndex.*;
import static org.junit.Assert.*;

public class ProgramIndexTest {
    @Test public void breakpointBitmapPreservesMultipleDisabledAndConditionalRulesAtCpuVisibleAddresses() {
        ProgramIndex index = create(128, new MemoryAnnotations());
        assertFalse(index.snapshot().hasExecutionBreakpoint(0));
        try (BreakpointUpdate edit = index.updateBreakpoints(index.snapshot())) {
            edit.put(1, 0, true);
            edit.put(2, 0, true);
            edit.put(3, 63, true);
            edit.put(4, 1, false);
            edit.put(5, 127, true);
            edit.commit();
        }
        Snapshot old = index.snapshot();
        assertTrue(old.hasExecutionBreakpoint(0));
        assertFalse(old.hasExecutionBreakpoint(1));
        assertTrue(old.hasExecutionBreakpoint(63));
        assertTrue(old.hasExecutionBreakpoint(127));
        assertFalse(old.hasExecutionBreakpoint(64));
        List<Breakpoint> rules = new ArrayList<>();
        old.forEachExecutionBreakpoint(0, rules::add);
        assertEquals(2, rules.size());
        assertEquals(Set.of(1L, 2L), Set.of(rules.get(0).id(), rules.get(1).id()));
        try (BreakpointUpdate edit = index.updateBreakpoints(old)) {
            edit.remove(1).put(4, 1, true);
            edit.commit();
        }
        assertTrue(index.snapshot().hasExecutionBreakpoint(0));
        assertTrue(index.snapshot().hasExecutionBreakpoint(1));
        try (BreakpointUpdate edit = index.updateBreakpoints(index.snapshot())) {
            edit.remove(2).commit();
        }
        assertFalse(index.snapshot().hasExecutionBreakpoint(0));
        assertTrue(old.hasExecutionBreakpoint(0));
    }

    @Test public void graphKeepsEdgeKindsSelfLoopsAndKnownUndecodedTargets() {
        ProgramIndex index = create(32, new MemoryAnnotations());
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(true)
                    .putInstruction(new Range(0, 3), InstructionKind.CALL)
                    .putInstruction(new Range(3, 4), InstructionKind.JUMP)
                    .putInstruction(new Range(16, 17), InstructionKind.RETURN)
                    .putEdge(0, 3, EdgeKind.FALL_THROUGH)
                    .putEdge(0, 3, EdgeKind.CALL)
                    .putEdge(0, 3, EdgeKind.CALL)
                    .putEdge(3, 3, EdgeKind.BRANCH)
                    .putEdge(3, 20, EdgeKind.BRANCH);
            edit.commit();
        }
        Snapshot snapshot = index.snapshot();
        assertEquals(FlowState.TRUNCATED, snapshot.flowState());
        assertNotNull(snapshot.instructionAt(0));
        assertNull(snapshot.instructionAt(1));
        assertEquals(InstructionKind.JUMP, snapshot.instructionAt(3).kind());
        assertEquals(17, snapshot.instructionAt(16).cells().end);
        assertEquals(Set.of("0>3:FALL_THROUGH", "0>3:CALL", "3>3:BRANCH"), edges(snapshot, 3, true));
        assertNull(snapshot.instructionAt(20));
        assertEquals(Set.of("3>20:BRANCH"), edges(snapshot, 20, true));
        assertTrue(edges(snapshot, 20, false).isEmpty());
        List<Instruction> instructions = new ArrayList<>();
        snapshot.forEachInstruction(instructions::add);
        assertEquals(3, instructions.size());
        assertEquals(0, instructions.get(0).cells().start);
    }

    @Test public void memoryChangeRejectsLateContentButPreservesIndependentBreakpointEdit() throws Exception {
        ProgramIndex index = create(32, new MemoryAnnotations());
        Snapshot base = index.snapshot();
        CountDownLatch staged = new CountDownLatch(1), changed = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<CommitResult> result = executor.submit(() -> {
                try (ContentUpdate edit = index.updateContent(base)) {
                    edit.replaceFlow(false).putInstruction(new Range(0, 2), InstructionKind.STOP);
                    staged.countDown(); await(changed);
                    return edit.commit();
                }
            });
            BreakpointUpdate breakpoint = index.updateBreakpoints(base);
            breakpoint.put(1, 0, true);
            await(staged); index.memoryChanged(); changed.countDown();
            assertEquals(CommitResult.STALE, result.get(10, TimeUnit.SECONDS));
            assertEquals(CommitResult.PUBLISHED, breakpoint.commit());
            assertEquals(1, index.snapshot().memoryRevision());
            assertEquals(FlowState.STALE, index.snapshot().flowState());
            assertTrue(index.snapshot().hasExecutionBreakpoint(0));
        } finally { executor.shutdownNow(); }
    }

    @Test public void annotationChangesInvalidateFlowAndRejectEarlierEdits() {
        ProgramIndex index = create(8, new MemoryAnnotations());
        try (ContentUpdate update = index.updateContent(index.snapshot())) {
            update.replaceFlow(false).putInstruction(new Range(0, 1), InstructionKind.STOP).commit();
        }
        Snapshot before = index.snapshot();
        ContentUpdate pending = index.updateContent(before);
        pending.replaceFlow(false).putInstruction(new Range(1, 2), InstructionKind.STOP);

        index.annotationsChanged();

        assertEquals(FlowState.STALE, index.snapshot().flowState());
        assertNotNull(before.instructionAt(0));
        assertEquals(CommitResult.STALE, pending.commit());
    }

    @Test public void graphAndBreakpointCommitsKeepIndependentChanges() {
        ProgramIndex index = create(8, new MemoryAnnotations());
        Snapshot base = index.snapshot();
        ContentUpdate graph = index.updateContent(base);
        BreakpointUpdate breakpoint = index.updateBreakpoints(base);
        graph.replaceFlow(false).putInstruction(new Range(0, 1), InstructionKind.STOP);
        breakpoint.put(7, 0, true);

        assertEquals(CommitResult.PUBLISHED, breakpoint.commit());
        assertEquals(CommitResult.PUBLISHED, graph.commit());
        assertNotNull(index.snapshot().instructionAt(0));
        assertTrue(index.snapshot().hasExecutionBreakpoint(0));
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue("Barrier timed out", latch.await(10, TimeUnit.SECONDS));
    }
    private static String edge(int from, int to, EdgeKind kind) {
        return from + ">" + to + ":" + kind;
    }
    private static Set<String> edges(Snapshot snapshot, int address, boolean incoming) {
        Set<String> edges = new HashSet<>();
        EdgeVisitor visitor = (from, to, kind) -> assertTrue("Duplicate edge", edges.add(edge(from, to, kind)));
        if (incoming) {
            snapshot.forEachInstruction(instruction -> snapshot.forEachSuccessor(instruction.cells().start, (from, to, kind) -> {
                if (to == address) visitor.accept(from, to, kind);
            }));
        } else snapshot.forEachSuccessor(address, visitor);
        return edges;
    }
}
