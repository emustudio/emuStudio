/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import com.sun.management.ThreadMXBean;
import org.junit.Assume;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.function.Consumer;

import static net.emustudio.application.debugger.index.ProgramIndex.*;
import static org.junit.Assert.assertEquals;

/** Allocation contract only; latency and retained-memory measurements belong to JMH. */
public class ProgramIndexAllocationTest {
    private static final int WARMUP = 30000;
    private static final int SAMPLES = 10000;
    private static volatile long consumed;
    private static final Consumer<Object> VALUES = ProgramIndexAllocationTest::consume;
    private static final EdgeVisitor EDGES = ProgramIndexAllocationTest::consumeEdge;

    @Test public void denseBankedQueriesAllocateNothingAfterWarmup() {
        verifyQueries("dense", 65536, 49152);
    }

    @Test public void sparseLargeDomainQueriesAllocateNothingAfterWarmup() {
        verifyQueries("sparse", Integer.MAX_VALUE, Integer.MAX_VALUE - 1024);
    }

    private static void verifyQueries(String name, int size, int common) {
        java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
        Assume.assumeTrue("Thread allocation counters unavailable", platformBean instanceof ThreadMXBean);
        ThreadMXBean bean = (ThreadMXBean) platformBean;
        Assume.assumeTrue("Thread allocation counters unsupported", bean.isThreadAllocatedMemorySupported());
        boolean enabled = bean.isThreadAllocatedMemoryEnabled();
        if (!enabled) bean.setThreadAllocatedMemoryEnabled(true);
        try {
            long thread = Thread.currentThread().getId();
            Assume.assumeTrue("Current-thread allocation counter unavailable", bean.getThreadAllocatedBytes(thread) >= 0);
            Workload workload = new Workload(size, common);
            workload.query(WARMUP);
            baseline(WARMUP);

            // Both samples use the same counter calls. Warm the sampling paths as well, so
            // class initialization/JIT setup cannot be mistaken for an index allocation.
            allocated(bean, thread, workload, true);
            allocated(bean, thread, workload, false);
            long baseline = allocated(bean, thread, workload, false);
            long queries = allocated(bean, thread, workload, true);
            System.out.println(name + " ProgramIndex queries: " + queries + " B / " + SAMPLES
                    + " batches; empty-loop/counter baseline " + baseline + " B");
            assertEquals(name + " hot queries allocated bytes above the empty-loop/counter baseline",
                    0L, queries - baseline);
        } finally {
            if (!enabled) bean.setThreadAllocatedMemoryEnabled(false);
        }
    }

    private static long allocated(ThreadMXBean bean, long thread, Workload workload, boolean query) {
        long before = bean.getThreadAllocatedBytes(thread);
        if (query) workload.query(SAMPLES);
        else baseline(SAMPLES);
        return bean.getThreadAllocatedBytes(thread) - before;
    }

    private static void baseline(int iterations) {
        long value = consumed;
        for (int i = 0; i < iterations; i++) value = (value * 31) ^ i;
        consumed = value;
    }

    private static void consume(Object value) {
        if (value instanceof Mapping) consumed += ((Mapping) value).entryId();
        else if (value instanceof Annotation) consumed += ((Annotation) value).entryId();
        else if (value instanceof Breakpoint) consumed += ((Breakpoint) value).id();
        else consumed += ((Instruction) value).cells().start;
    }

    private static void consumeEdge(int fromBank, int from, int toBank, int to, EdgeKind kind) {
        consumed += fromBank + from + toBank + to + kind.ordinal();
    }

    private static final class Workload {
        final ProgramIndex index;
        final int[] addresses;
        final Range[] ranges;
        final SourceSpan[] sources;
        final String[] labels = {"label0", "label1", "missing"};
        int cursor;

        Workload(int size, int common) {
            index = ProgramIndex.create("allocation", size, 2, common);
            int[] starts = {0, 32, 4096, size / 2, common - 2, common, size - 2};
            addresses = new int[starts.length * 2 + 3];
            addresses[starts.length * 2] = 128;
            addresses[starts.length * 2 + 1] = size / 4 + 17;
            addresses[starts.length * 2 + 2] = common - 1025;
            sources = new SourceSpan[starts.length + 1];
            ranges = new Range[] {
                    new Range(0, 0, 2), new Range(1, common - 2, common + 2),
                    new Range(1, size / 2 - 1, size / 2 + 3), new Range(0, size - 1, size)
            };
            try (ContentUpdate content = index.updateContent(index.snapshot())) {
                content.replaceFlow(false);
                for (int i = 0; i < starts.length; i++) {
                    int address = starts[i];
                    addresses[2 * i] = address;
                    addresses[2 * i + 1] = address + 1;
                    sources[i] = new SourceSpan("file:///allocation.asm", 2000, i * 2, i * 2 + 3);
                    content.putMapping(1, i, new Range(1, address, address + 2), sources[i]);
                    content.putAnnotation(1, i, new Range(1, address, address + 2), Kinds.LABEL, "label" + i % 2);
                    content.putInstruction(new Range(1, address, address + 2), InstructionKind.CALL, false);
                    int next = starts[(i + 1) % starts.length];
                    content.putEdge(1, address, 1, next, EdgeKind.CALL);
                    content.putEdge(1, address, 1, address + 1, EdgeKind.FALL_THROUGH);
                    content.putEdge(1, address, 1, next, EdgeKind.BRANCH);
                }
                content.putAnnotation(2, 1, new Range(1, 0, 128), Kinds.REGION, "outer");
                content.putAnnotation(2, 2, new Range(1, 16, 64), Kinds.REGION, "nested");
                content.putAnnotation(2, 3, new Range(1, common - 2, common + 2), Kinds.REGION, "crossing");
                assertEquals(CommitResult.PUBLISHED, content.commit());
            }
            sources[starts.length] = new SourceSpan("file:///allocation.asm", 2001, 0, 1);
            try (BreakpointUpdate breakpoints = index.updateBreakpoints(index.snapshot())) {
                for (int i = 0; i < starts.length; i++) {
                    breakpoints.put(i, starts[i], true, "");
                    breakpoints.put(100 + i, starts[i], true, "A == 1");
                    breakpoints.put(200 + i, starts[i], false, "");
                }
                assertEquals(CommitResult.PUBLISHED, breakpoints.commit());
            }
        }

        void query(int iterations) {
            for (int i = 0; i < iterations; i++) {
                int selected = cursor++ & Integer.MAX_VALUE;
                int bank = (selected / addresses.length) & 1;
                int address = addresses[selected % addresses.length];
                Snapshot snapshot = index.snapshot();
                snapshot.forEachMappingAt(bank, address, VALUES);
                snapshot.forEachAnnotationStartingAt(bank, address, VALUES);
                snapshot.forEachAnnotationCovering(bank, address, VALUES);
                snapshot.forEachAnnotationOverlapping(ranges[selected % ranges.length], VALUES);
                snapshot.forEachLabelNamed(labels[selected % labels.length], VALUES);
                snapshot.forEachMappingOverlappingSource(sources[selected % sources.length], VALUES);
                consumed += snapshot.hasExecutionBreakpoint(address) ? 1 : 0;
                snapshot.forEachExecutionBreakpoint(address, VALUES);
                Instruction instruction = snapshot.instructionAt(bank, address);
                if (instruction != null) consumed += instruction.cells().end;
                snapshot.forEachSuccessor(bank, address, EDGES);
                snapshot.forEachPredecessor(bank, address, EDGES);
                snapshot.forEachMapping(VALUES);
                snapshot.forEachAnnotation(VALUES);
                snapshot.forEachBreakpoint(VALUES);
                snapshot.forEachInstruction(VALUES);
            }
        }
    }
}
