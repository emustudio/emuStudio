/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import com.sun.management.ThreadMXBean;
import org.junit.Assume;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.function.Consumer;

import static net.emustudio.application.debugger.index.ProgramIndex.*;
import static org.junit.Assert.assertEquals;

/** Flow query allocation contract; breakpoint set lookups may box addresses. */
public class ProgramIndexAllocationTest {
    private static final int WARMUP = 30000;
    private static final int SAMPLES = 10000;
    private static volatile long consumed;
    private static final Consumer<Object> VALUES = ProgramIndexAllocationTest::consume;
    private static final EdgeVisitor EDGES = ProgramIndexAllocationTest::consumeEdge;

    @Test public void denseFlowQueriesAllocateNothingAfterWarmup() {
        verifyQueries("dense", 65536, 49152);
    }

    @Test public void sparseLargeDomainFlowQueriesAllocateNothingAfterWarmup() {
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
            System.out.println(name + " ProgramIndex flow queries: " + queries + " B / " + SAMPLES
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
        consumed += ((Instruction) value).cells().start;
    }

    private static void consumeEdge(int from, int to, EdgeKind kind) {
        consumed += from + to + kind.ordinal();
    }

    private static final class Workload {
        final ProgramIndex index;
        final int[] addresses;
        int cursor;

        Workload(int size, int common) {
            index = ProgramIndex.create(size, new MemoryAnnotations());
            int[] starts = {0, 32, 4096, size / 2, common - 2, common, size - 2};
            addresses = new int[starts.length * 2 + 3];
            addresses[starts.length * 2] = 128;
            addresses[starts.length * 2 + 1] = size / 4 + 17;
            addresses[starts.length * 2 + 2] = common - 1025;
            try (ContentUpdate content = index.updateContent(index.snapshot())) {
                content.replaceFlow(false);
                for (int i = 0; i < starts.length; i++) {
                    int address = starts[i];
                    addresses[2 * i] = address;
                    addresses[2 * i + 1] = address + 1;
                    content.putInstruction(new Range(address, address + 2), InstructionKind.CALL);
                    int next = starts[(i + 1) % starts.length];
                    content.putEdge(address, next, EdgeKind.CALL);
                    content.putEdge(address, address + 1, EdgeKind.FALL_THROUGH);
                    content.putEdge(address, next, EdgeKind.BRANCH);
                }
                assertEquals(CommitResult.PUBLISHED, content.commit());
            }
            try (BreakpointUpdate breakpoints = index.updateBreakpoints(index.snapshot())) {
                for (int start : starts) breakpoints.add(start);
                assertEquals(CommitResult.PUBLISHED, breakpoints.commit());
            }
        }

        void query(int iterations) {
            for (int i = 0; i < iterations; i++) {
                int selected = cursor++ & Integer.MAX_VALUE;
                int address = addresses[selected % addresses.length];
                Snapshot snapshot = index.snapshot();
                Instruction instruction = snapshot.instructionAt(address);
                if (instruction != null) consumed += instruction.cells().end;
                snapshot.forEachSuccessor(address, EDGES);
                snapshot.forEachInstruction(VALUES);
            }
        }
    }
}
