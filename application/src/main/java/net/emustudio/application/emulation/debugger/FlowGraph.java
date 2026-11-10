/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Immutable storage for published instruction-flow graphs. Never invokes a decoder.
 */
public final class FlowGraph {
    public static final class Instruction {
        private final Range cells;
        public Instruction(Range cells) {
            this.cells = java.util.Objects.requireNonNull(cells);
        }
        public Range cells() { return cells; }
    }

    @FunctionalInterface
    public interface EdgeVisitor {
        void accept(int fromAddress, int toAddress);
    }

    /** Nonempty half-open range of CPU-visible cells. Addresses are CPU-visible cells. */
    public static final class Range {
        public final int start, end;
        public Range(int start, int end) {
            if (start < 0 || end <= start)
                throw new IllegalArgumentException("Invalid range");
            this.start = start;
            this.end = end;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Range)) return false;
            Range r = (Range) other;
            return start == r.start && end == r.end;
        }
        @Override public int hashCode() { return 31 * start + end; }
    }

    private final int[] addresses, outgoingOffsets, outgoing;
    private final FlowGraph.Instruction[] instructions;

    public FlowGraph(Map<Integer, FlowGraph.Instruction> decoded, Set<Edge> edges) {
        Set<Integer> keys = new TreeSet<>(decoded.keySet());
        for (Edge e : edges) {
            if (!decoded.containsKey(e.from)) throw new IllegalArgumentException("Edge source is not an instruction start");
            keys.add(e.to);
        }
        addresses = new int[keys.size()];
        instructions = new FlowGraph.Instruction[keys.size()];
        Map<Integer, Integer> ids = new TreeMap<>();
        int i = 0;
        for (int key : keys) {
            addresses[i] = key;
            instructions[i] = decoded.get(key);
            ids.put(key, ++i);
        }
        outgoingOffsets = new int[keys.size() + 1];
        for (Edge e : edges) {
            outgoingOffsets[ids.get(e.from)]++;
        }
        for (i = 1; i < outgoingOffsets.length; i++) {
            outgoingOffsets[i] += outgoingOffsets[i - 1];
        }
        outgoing = new int[edges.size()];
        int[] outCursor = outgoingOffsets.clone();
        for (Edge e : edges) {
            int from = ids.get(e.from) - 1, to = ids.get(e.to) - 1;
            int outAt = outCursor[from]++;
            outgoing[outAt] = to;
        }
    }

    public FlowGraph.Instruction instructionAt(int address) {
        int id = Arrays.binarySearch(addresses, address);
        return id < 0 ? null : instructions[id];
    }

    public void instructions(Consumer<? super FlowGraph.Instruction> visitor) {
        for (FlowGraph.Instruction instruction : instructions) if (instruction != null) visitor.accept(instruction);
    }

    public void successors(int address, FlowGraph.EdgeVisitor visitor) {
        int id = Arrays.binarySearch(addresses, address);
        if (id < 0) return;
        for (int at = outgoingOffsets[id]; at < outgoingOffsets[id + 1]; at++) {
            int to = outgoing[at];
            visitor.accept(addresses[id], addresses[to]);
        }
    }

    public static final class Edge implements Comparable<Edge> {
        final int from, to;
        public Edge(int from, int to) { this.from = from; this.to = to; }
        @Override public int compareTo(Edge other) {
            int comparison = Integer.compare(from, other.from);
            return comparison == 0 ? Integer.compare(to, other.to) : comparison;
        }
    }

}
