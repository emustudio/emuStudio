/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import com.sun.management.ThreadMXBean;
import org.junit.Assume;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.function.Consumer;

import static net.emustudio.application.emulation.debugger.FlowGraph.*;
import java.util.*;
import static org.junit.Assert.assertEquals;

/** Flow query allocation contract; breakpoint set lookups may box addresses. */
public class FlowGraphAllocationTest {
    private static final int WARMUP = 30000;
    private static final int SAMPLES = 10000;
    private static volatile long consumed;
    private static final Consumer<Object> VALUES = FlowGraphAllocationTest::consume;
    private static final EdgeVisitor EDGES = FlowGraphAllocationTest::consumeEdge;

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
            System.out.println(name + " FlowGraph queries: " + queries + " B / " + SAMPLES
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

    private static void consumeEdge(int from, int to) {
        consumed += from + to;
    }

    private static final class Workload {
        final FlowGraph graph;
        final int[] addresses;
        int cursor;

        Workload(int size, int common) {
            int[] starts = {0, 32, 4096, size / 2, common - 2, common, size - 2};
            addresses = new int[starts.length * 2 + 3];
            addresses[starts.length * 2] = 128;
            addresses[starts.length * 2 + 1] = size / 4 + 17;
            addresses[starts.length * 2 + 2] = common - 1025;
            Map<Integer, Instruction> instructions = new TreeMap<>();
            Set<Edge> edges = new TreeSet<>();
            for (int i = 0; i < starts.length; i++) {
                int address = starts[i];
                addresses[2 * i] = address;
                addresses[2 * i + 1] = address + 1;
                instructions.put(address, new Instruction(new Range(address, address + 2)));
                int next = starts[(i + 1) % starts.length];
                edges.add(new Edge(address, next));
                edges.add(new Edge(address, address + 1));
                edges.add(new Edge(address, address + 2));
            }
            graph = new FlowGraph(instructions, edges);
        }

        void query(int iterations) {
            for (int i = 0; i < iterations; i++) {
                int selected = cursor++ & Integer.MAX_VALUE;
                int address = addresses[selected % addresses.length];
                Instruction instruction = graph.instructionAt(address);
                if (instruction != null) consumed += instruction.cells().end;
                graph.successors(address, EDGES);
                graph.instructions(VALUES);
            }
        }
    }
}
