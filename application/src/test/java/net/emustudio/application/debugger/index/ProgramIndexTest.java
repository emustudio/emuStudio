/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static net.emustudio.application.debugger.index.ProgramIndex.*;
import static org.junit.Assert.*;

public class ProgramIndexTest {
    @Test public void validatesLayoutRangesAndUsesExplicitHalfOpenCoverage() {
        assertThrows(IllegalArgumentException.class, () -> create("", 16));
        assertThrows(IllegalArgumentException.class, () -> create("x", 0));
        assertThrows(IllegalArgumentException.class, () -> new Range(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Range(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Range(Integer.MAX_VALUE, Integer.MIN_VALUE));
        ProgramIndex index = create("word-memory", 16);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            assertThrows(IllegalArgumentException.class, () -> edit.putMapping(1, 1, new Range(16, 17), source(0, 1)));
        }
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            assertThrows(IllegalArgumentException.class, () -> edit.putMapping(1, 1, new Range(15, 17), source(0, 1)));
        }
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(1, 1, new Range(0, 2), source(0, 1));
            edit.putMapping(1, 2, new Range(15, 16), source(2, 3));
            assertEquals(CommitResult.PUBLISHED, edit.commit());
        }
        Snapshot snapshot = index.snapshot();
        assertEquals(List.of(1L), mappingIds(snapshot, 0));
        assertEquals(List.of(1L), mappingIds(snapshot, 1));
        assertTrue(mappingIds(snapshot, 2).isEmpty());
        assertTrue(mappingIds(snapshot, 14).isEmpty());
        assertEquals(List.of(2L), mappingIds(snapshot, 15));
        assertThrows(IllegalArgumentException.class, () -> mappingIds(snapshot, 16));
        assertThrows(IllegalArgumentException.class, () -> snapshot.hasExecutionBreakpoint(-1));
        assertThrows(NullPointerException.class, () -> snapshot.forEachAnnotationCovering(0, null));
    }

    @Test public void overlappingRangesKeepIndependentOwnersAndExactEndpoints() {
        ProgramIndex index = create("ranges", 16);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putAnnotation(1, 1, new Range(6, 11), "region", "crossing");
            edit.putAnnotation(1, 2, new Range(6, 7), Kinds.LABEL, "same");
            edit.putAnnotation(2, 3, new Range(6, 7), Kinds.LABEL, "same");
            edit.putAnnotation(2, 4, new Range(9, 10), "plugin:custom", "common");
            edit.commit();
        }
        Snapshot snapshot = index.snapshot();
        assertEquals(Set.of(1L, 2L, 3L), annotationsAt(snapshot, 6));
        assertEquals(Set.of(1L), annotationsAt(snapshot, 7));
        assertEquals(Set.of(1L, 4L), annotationsAt(snapshot, 9));
        List<Annotation> starts = new ArrayList<>();
        snapshot.forEachAnnotationStartingAt(9, starts::add);
        assertEquals(9, starts.get(0).range().start);
        try (ContentUpdate edit = index.updateContent(snapshot)) {
            edit.removeAnnotations(1).putAnnotation(1, 10, new Range(0, 1), "comment", "");
            edit.commit();
        }
        List<Annotation> all = new ArrayList<>();
        index.snapshot().forEachAnnotation(all::add);
        assertEquals(Set.of(3L, 4L, 10L), annotationIds(all));
        assertEquals(Set.of(1L, 2L, 3L), annotationsAt(snapshot, 6));
    }

    @Test public void sourceSpansSupportManyToManyNestedSpansAndExactRevisionIdentity() {
        ProgramIndex index = create("sources", 32);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(1, 1, new Range(0, 2), new SourceSpan("file:///a/main.asm", 7, 0, 20));
            edit.putMapping(1, 2, new Range(5, 8), new SourceSpan("file:///a/main.asm", 7, 5, 7));
            edit.putMapping(1, 3, new Range(5, 6), new SourceSpan("file:///a/main.asm", 7, 0, 20));
            edit.putMapping(1, 4, new Range(10, 11), new SourceSpan("file:///b/main.asm", 7, 0, 20));
            edit.putMapping(1, 5, new Range(12, 13), new SourceSpan("file:///a/main.asm", 8, 0, 20));
            edit.commit();
        }
        assertEquals(Set.of(2L, 3L), new HashSet<>(mappingIds(index.snapshot(), 5)));
        List<Mapping> found = new ArrayList<>();
        index.snapshot().forEachMappingOverlappingSource(new SourceSpan("file:///a/main.asm", 7, 6, 7), found::add);
        assertEquals(Set.of(1L, 2L, 3L), mappingIdSet(found));
        found.clear();
        index.snapshot().forEachMappingOverlappingSource(new SourceSpan("file:///a/main.asm", 7, 20, 21), found::add);
        assertTrue(found.isEmpty());
        index.snapshot().forEachMappingOverlappingSource(new SourceSpan("file:///a/main.asm", 9, 0, 1), found::add);
        assertTrue(found.isEmpty());
        Snapshot before = index.snapshot();
        index.memoryChanged();
        List<Mapping> historical = new ArrayList<>();
        index.snapshot().forEachMappingAt(0, historical::add);
        assertTrue(historical.stream().allMatch(m -> m.memoryRevision() == before.memoryRevision()));
        assertNotEquals(index.snapshot().memoryRevision(), historical.get(0).memoryRevision());
        assertEquals(0, before.memoryRevision());
    }

    @Test public void breakpointBitmapPreservesMultipleDisabledAndConditionalRulesAtCpuVisibleAddresses() {
        ProgramIndex index = create("rules", 128);
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
        ProgramIndex index = create("flow", 32);
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

    @Test public void memoryAndCodeDataEditsStaleGraphButOtherMetadataPreservesIt() {
        ProgramIndex index = create("stale", 32);
        buildTinyFlow(index);
        Snapshot historical = index.snapshot();
        try (ContentUpdate edit = index.updateContent(historical)) {
            edit.putAnnotation(1, 1, new Range(1, 2), "comment", "operand").commit();
        }
        assertEquals(FlowState.CURRENT, index.snapshot().flowState());
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putAnnotation(1, 1, new Range(1, 2), Kinds.DATA, "operand").commit();
        }
        assertEquals(FlowState.STALE, index.snapshot().flowState());
        assertNull(index.snapshot().instructionAt(0));
        assertNotNull(historical.instructionAt(0));
        buildTinyFlow(index);
        index.memoryChanged(); // A change in any covered cell, including an operand, invalidates all flow.
        assertEquals(FlowState.STALE, index.snapshot().flowState());
        assertTrue(edges(index.snapshot(), 3, true).isEmpty());
        assertNotNull(historical.instructionAt(0));
    }

    @Test public void editsAreAtomicConsumedAndRejectForeignOrStaleSameComponentBases() {
        ProgramIndex index = create("atomic", 8);
        Snapshot initial = index.snapshot();
        assertSame(initial, index.snapshot());
        ProgramIndex foreign = create("atomic", 8);
        assertThrows(IllegalArgumentException.class, () -> index.updateContent(foreign.snapshot()));
        ContentUpdate first = index.updateContent(initial), second = index.updateContent(initial);
        first.putAnnotation(1, 1, new Range(0, 1), Kinds.LABEL, "one");
        second.putAnnotation(1, 2, new Range(1, 2), Kinds.LABEL, "two");
        assertEquals(CommitResult.PUBLISHED, first.commit());
        assertEquals(CommitResult.STALE, second.commit());
        assertThrows(IllegalStateException.class, first::commit);
        first.close(); first.close();
        ContentUpdate closed = index.updateContent(index.snapshot());
        closed.putAnnotation(1, 3, new Range(2, 3), "comment", "discard");
        closed.close();
        assertThrows(IllegalStateException.class, closed::commit);
        assertTrue(annotationsAt(index.snapshot(), 2).isEmpty());
        Snapshot beforeInvalid = index.snapshot();
        ContentUpdate invalid = index.updateContent(beforeInvalid);
        invalid.putAnnotation(1, 4, new Range(3, 4), "comment", "discard");
        invalid.replaceFlow(false).putEdge(5, 6, EdgeKind.CALL);
        assertThrows(IllegalArgumentException.class, invalid::commit);
        assertSame(beforeInvalid, index.snapshot());
        BreakpointUpdate b1 = index.updateBreakpoints(beforeInvalid), b2 = index.updateBreakpoints(beforeInvalid);
        b1.put(1, 0, true).commit();
        assertEquals(CommitResult.STALE, b2.put(2, 1, true).commit());
    }

    @Test public void validationFailurePoisonsEarlierStagedEntriesInBothEditKinds() {
        ProgramIndex index = create("poison", 8);
        Snapshot initial = index.snapshot();
        ContentUpdate content = index.updateContent(initial);
        content.putAnnotation(1, 1, new Range(0, 1), "comment", "must not publish");
        assertThrows(IllegalArgumentException.class,
                () -> content.putAnnotation(1, 2, new Range(1, 2), Kinds.LABEL, ""));
        assertThrows(IllegalStateException.class, content::commit);
        assertSame(initial, index.snapshot());
        BreakpointUpdate breakpoint = index.updateBreakpoints(initial);
        breakpoint.put(1, 0, true);
        assertThrows(IllegalArgumentException.class, () -> breakpoint.put(2, 8, true));
        assertThrows(IllegalStateException.class, breakpoint::commit);
        assertSame(initial, index.snapshot());
        content.close(); breakpoint.close();
    }

    @Test public void independentConcurrentCommitsPreserveBothComponents() throws Exception {
        ProgramIndex index = create("race", 32);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 20; round++) {
                Snapshot base = index.snapshot();
                CountDownLatch ready = new CountDownLatch(2), publish = new CountDownLatch(1);
                Future<CommitResult> content = executor.submit(() -> {
                    try (ContentUpdate edit = index.updateContent(base)) {
                        edit.putAnnotation(1, 1, new Range(0, 1), Kinds.LABEL, "label");
                        ready.countDown(); await(publish);
                        return edit.commit();
                    }
                });
                Future<CommitResult> breakpoint = executor.submit(() -> {
                    try (BreakpointUpdate edit = index.updateBreakpoints(base)) {
                        edit.put(1, 0, true);
                        ready.countDown(); await(publish);
                        return edit.commit();
                    }
                });
                await(ready); publish.countDown();
                assertEquals(CommitResult.PUBLISHED, content.get(10, TimeUnit.SECONDS));
                assertEquals(CommitResult.PUBLISHED, breakpoint.get(10, TimeUnit.SECONDS));
                assertEquals(Set.of(1L), annotationsAt(index.snapshot(), 0));
                assertTrue(index.snapshot().hasExecutionBreakpoint(0));
                assertEquals(base.contentRevision() + 1, index.snapshot().contentRevision());
            }
        } finally { executor.shutdownNow(); }
    }

    @Test public void memoryChangeRejectsLateContentButPreservesIndependentBreakpointEdit() throws Exception {
        ProgramIndex index = create("memory-race", 32);
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

    @Test public void sparseHugeLayoutsAndLongRangesDoNotAllocateFlatAddressDomains() {
        ProgramIndex index = create("sparse", Integer.MAX_VALUE);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(1, 1, new Range(7, Integer.MAX_VALUE), source(0, 1));
            edit.putAnnotation(1, 2, new Range(1999999999, Integer.MAX_VALUE), "region", "large");
            edit.replaceFlow(false).putInstruction(new Range(7, 9), InstructionKind.JUMP)
                    .putEdge(7, Integer.MAX_VALUE - 1, EdgeKind.BRANCH).commit();
        }
        try (BreakpointUpdate edit = index.updateBreakpoints(index.snapshot())) {
            edit.put(1, Integer.MAX_VALUE - 1, true).commit();
        }
        assertEquals(List.of(1L), mappingIds(index.snapshot(), 7));
        assertEquals(List.of(1L), mappingIds(index.snapshot(), Integer.MAX_VALUE - 1));
        assertEquals(Set.of(2L), annotationsAt(index.snapshot(), Integer.MAX_VALUE - 1));
        assertTrue(index.snapshot().hasExecutionBreakpoint(Integer.MAX_VALUE - 1));
        assertNotNull(index.snapshot().instructionAt(7));
        assertEquals(Set.of("7>2147483646:BRANCH"), edges(index.snapshot(), Integer.MAX_VALUE - 1, true));
    }

    @Test public void randomizedIntervalsMatchIndependentAddressScan() {
        ProgramIndex index = create("oracle", 64);
        Random random = new Random(454);
        List<Range> ranges = new ArrayList<>();
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            for (int i = 0; i < 300; i++) {
                int start = random.nextInt(64), end = start + 1 + random.nextInt(64 - start);
                Range r = new Range(start, end);
                ranges.add(r);
                edit.putAnnotation(i % 7, i, r, "region", "r" + i);
                edit.putMapping(i % 7, i, r, source(i, i + 1));
            }
            edit.commit();
        }
        for (int address = 0; address < 64; address++) {
            Set<Long> expected = new HashSet<>();
            for (int i = 0; i < ranges.size(); i++) {
                Range r = ranges.get(i);
                if (r.start <= address && address < r.end) expected.add((long) i);
            }
            assertEquals(expected, annotationsAt(index.snapshot(), address));
            assertEquals(expected, new HashSet<>(mappingIds(index.snapshot(), address)));
        }
    }

    @Test public void denseOverlappingCoverageAndSparseUniformPagesKeepExactEndpoints() {
        ProgramIndex dense = create("dense-overlap", 65536);
        try (ContentUpdate edit = dense.updateContent(dense.snapshot())) {
            for (int i = 0; i < 64; i++) edit.putMapping(1, i, new Range(0, 65536), source(i, i + 1));
            edit.commit();
        }
        assertEquals(64, mappingIds(dense.snapshot(), 0).size());
        assertEquals(64, mappingIds(dense.snapshot(), 65535).size());
        ProgramIndex sparse = create("uniform-pages", Integer.MAX_VALUE);
        List<Range> ranges = List.of(new Range(1, 1048577),
                new Range(1023, 2097152), new Range(1048575, 1073741825),
                new Range(1073741824, Integer.MAX_VALUE), new Range(1073741823, 1073741825));
        try (ContentUpdate edit = sparse.updateContent(sparse.snapshot())) {
            for (int i = 0; i < ranges.size(); i++) edit.putMapping(1, i, ranges.get(i), source(i, i + 1));
            edit.commit();
        }
        int[] points = {0, 1, 1022, 1023, 1024, 1048574, 1048575, 1048576, 1048577,
                2097151, 2097152, 1073741823, 1073741824, 1073741825, Integer.MAX_VALUE - 1};
        for (int point : points) {
            Set<Long> expected = new HashSet<>();
            for (int i = 0; i < ranges.size(); i++) {
                Range range = ranges.get(i);
                if (range.start <= point && point < range.end)
                    expected.add((long) i);
            }
            assertEquals("address=" + point, expected,
                    new HashSet<>(mappingIds(sparse.snapshot(), point)));
        }
    }

    @Test public void fiftyThousandNestedMappingsSharePostingsAndMatchScanOracle() {
        ProgramIndex index = create("nested-budget", 65536);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            for (int id = 0; id < 50000; id++)
                edit.putMapping(1, id, new Range(id, 65536), source(id, id + 1));
            assertEquals(CommitResult.PUBLISHED, edit.commit());
        }
        MappingCounter actual = new MappingCounter();
        for (int point : new int[] {0, 1, 127, 32768, 49999, 50000, 65535}) {
            long expectedCount = 0, expectedSum = 0;
            for (int id = 0; id < 50000; id++) if (id <= point && point < 65536) {
                expectedCount++;
                expectedSum += id;
            }
            actual.count = 0; actual.sum = 0;
            index.snapshot().forEachMappingAt(point, actual);
            assertEquals(expectedCount, actual.count);
            assertEquals(expectedSum, actual.sum);
        }
    }

    @Test public void sparsePagesKeepEndpointHitsMissesUniformCoverageAndSignedPostingIds() {
        ProgramIndex index = create("sparse-page-exceptions", Integer.MAX_VALUE);
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(1, 1, new Range(0, 1), source(0, 1));
            edit.putMapping(1, 2, new Range(1023, 1024), source(1, 2));
            edit.putMapping(1, 3, new Range(2048, 4096), source(2, 3));
            edit.putMapping(1, 4, new Range(2048, 2049), source(3, 4));
            edit.putMapping(1, 5, new Range(3071, 3072), source(4, 5));
            edit.putMapping(1, 6, new Range(Integer.MAX_VALUE - 1, Integer.MAX_VALUE), source(5, 6));
            edit.putAnnotation(1, 7, new Range(0, 1), Kinds.LABEL, "zero");
            edit.putAnnotation(1, 8, new Range(1023, 1024), Kinds.LABEL, "last-in-page");
            edit.replaceFlow(false).putInstruction(new Range(Integer.MAX_VALUE - 1, Integer.MAX_VALUE),
                    InstructionKind.STOP).commit();
        }
        Snapshot snapshot = index.snapshot();
        assertEquals(List.of(1L), mappingIds(snapshot, 0));
        assertTrue(mappingIds(snapshot, 1).isEmpty());
        assertTrue(mappingIds(snapshot, 1022).isEmpty());
        assertEquals(List.of(2L), mappingIds(snapshot, 1023));
        assertTrue(mappingIds(snapshot, 1024).isEmpty());
        assertEquals(Set.of(3L, 4L), new HashSet<>(mappingIds(snapshot, 2048)));
        assertEquals(List.of(3L), mappingIds(snapshot, 2049));
        assertEquals(Set.of(3L, 5L), new HashSet<>(mappingIds(snapshot, 3071)));
        assertEquals(List.of(3L), mappingIds(snapshot, 3072));
        assertTrue(mappingIds(snapshot, 4096).isEmpty());
        assertEquals(List.of(6L), mappingIds(snapshot, Integer.MAX_VALUE - 1));
        assertTrue(mappingIds(snapshot, Integer.MAX_VALUE - 2).isEmpty());
        List<Annotation> starts = new ArrayList<>();
        snapshot.forEachAnnotationStartingAt(0, starts::add);
        snapshot.forEachAnnotationStartingAt(1023, starts::add);
        snapshot.forEachAnnotationStartingAt(1, starts::add);
        assertEquals(Set.of(7L, 8L), annotationIds(starts));
        assertNotNull(snapshot.instructionAt(Integer.MAX_VALUE - 1));
        assertNull(snapshot.instructionAt(Integer.MAX_VALUE - 2));
    }

    private static final class MappingCounter implements java.util.function.Consumer<Mapping> {
        long count, sum;
        @Override public void accept(Mapping mapping) { count++; sum += mapping.entryId(); }
    }

    @Test public void randomizedSourceAndGraphQueriesMatchIndependentScans() {
        ProgramIndex index = create("reverse-oracles", 256);
        Random random = new Random(266);
        List<SourceSpan> sources = new ArrayList<>();
        Set<String> graph = new HashSet<>();
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false);
            for (int i = 0; i < 200; i++) {
                int from = random.nextInt(100), to = from + 1 + random.nextInt(100 - from);
                SourceSpan span = new SourceSpan("file:///" + random.nextInt(3) + "/same.asm", random.nextInt(3), from, to);
                sources.add(span);
                edit.putMapping(1, i, new Range(i, i + 1), span);
                edit.putInstruction(new Range(i, i + 1), InstructionKind.LINEAR);
            }
            for (int i = 0; i < 700; i++) {
                int from = random.nextInt(200), to = random.nextInt(256);
                EdgeKind kind = EdgeKind.values()[random.nextInt(3)];
                edit.putEdge(from, to, kind);
                graph.add(edge(from, to, kind));
            }
            edit.commit();
        }
        for (int test = 0; test < 300; test++) {
            int from = random.nextInt(100), to = from + 1 + random.nextInt(100 - from);
            SourceSpan query = new SourceSpan("file:///" + random.nextInt(3) + "/same.asm", random.nextInt(3), from, to);
            Set<Long> expected = new HashSet<>();
            for (int i = 0; i < sources.size(); i++) {
                SourceSpan span = sources.get(i);
                if (span.fileId.equals(query.fileId) && span.sourceRevision == query.sourceRevision
                        && span.startOffset < query.endOffset && span.endOffset > query.startOffset) expected.add((long) i);
            }
            List<Mapping> actual = new ArrayList<>();
            index.snapshot().forEachMappingOverlappingSource(query, actual::add);
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, mappingIdSet(actual));
        }
        for (int address = 0; address < 256; address++) {
            Set<String> incoming = new HashSet<>(), outgoing = new HashSet<>();
            for (String e : graph) {
                if (e.startsWith(address + ">")) outgoing.add(e);
                if (e.contains(">" + address + ":")) incoming.add(e);
            }
            assertEquals(incoming, edges(index.snapshot(), address, true));
            assertEquals(outgoing, edges(index.snapshot(), address, false));
        }
    }

    private static void buildTinyFlow(ProgramIndex index) {
        try (ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false).putInstruction(new Range(0, 3), InstructionKind.LINEAR)
                    .putEdge(0, 3, EdgeKind.FALL_THROUGH).commit();
        }
    }
    private static SourceSpan source(int from, int to) { return new SourceSpan("file:///source.asm", 0, from, to); }
    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue("Barrier timed out", latch.await(10, TimeUnit.SECONDS));
    }
    private static List<Long> mappingIds(Snapshot snapshot, int address) {
        List<Long> ids = new ArrayList<>();
        snapshot.forEachMappingAt(address, mapping -> ids.add(mapping.entryId()));
        return ids;
    }
    private static Set<Long> mappingIdSet(List<Mapping> mappings) {
        Set<Long> ids = new HashSet<>();
        for (Mapping mapping : mappings) ids.add(mapping.entryId());
        return ids;
    }
    private static Set<Long> annotationIds(List<Annotation> annotations) {
        Set<Long> ids = new HashSet<>();
        for (Annotation annotation : annotations) ids.add(annotation.entryId());
        return ids;
    }
    private static Set<Long> annotationsAt(Snapshot snapshot, int address) {
        List<Annotation> values = new ArrayList<>();
        snapshot.forEachAnnotationCovering(address, values::add);
        return annotationIds(values);
    }
    private static String edge(int from, int to, EdgeKind kind) {
        return from + ">" + to + ":" + kind;
    }
    private static Set<String> edges(Snapshot snapshot, int address, boolean incoming) {
        Set<String> edges = new HashSet<>();
        EdgeVisitor visitor = (from, to, kind) -> assertTrue("Duplicate edge", edges.add(edge(from, to, kind)));
        if (incoming) {
            snapshot.forEachInstruction(instruction -> snapshot.forEachSuccessor(instruction.cells().start, (from, to, kind) -> {
                        if (to == address)
                            visitor.accept(from, to, kind);
                    }));
        } else snapshot.forEachSuccessor(address, visitor);
        return edges;
    }
}
