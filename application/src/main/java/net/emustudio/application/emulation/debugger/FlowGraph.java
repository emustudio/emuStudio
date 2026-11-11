/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
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
            this.cells = Objects.requireNonNull(cells);
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
            Range range = (Range) other;
            return start == range.start && end == range.end;
        }
        @Override public int hashCode() { return 31 * start + end; }
    }

    private final int[] addresses, outgoingOffsets, outgoing;
    private final Instruction[] instructions;

    public FlowGraph(Map<Integer, Instruction> decoded, Set<Edge> edges) {
        Set<Integer> keys = new TreeSet<>(decoded.keySet());
        for (Edge edge : edges) {
            if (!decoded.containsKey(edge.from)) throw new IllegalArgumentException("Edge source is not an instruction start");
            keys.add(edge.to);
        }
        addresses = new int[keys.size()];
        instructions = new Instruction[keys.size()];
        Map<Integer, Integer> indexes = new TreeMap<>();
        int index = 0;
        for (int key : keys) {
            addresses[index] = key;
            instructions[index] = decoded.get(key);
            indexes.put(key, index++);
        }
        outgoingOffsets = successorOffsets(indexes, edges);
        outgoing = successorIndexes(indexes, edges);
    }

    private static int[] successorOffsets(Map<Integer, Integer> indexes, Set<Edge> edges) {
        int[] offsets = new int[indexes.size() + 1];
        for (Edge edge : edges) {
            offsets[indexes.get(edge.from) + 1]++;
        }
        for (int index = 1; index < offsets.length; index++) {
            offsets[index] += offsets[index - 1];
        }
        return offsets;
    }

    private int[] successorIndexes(Map<Integer, Integer> indexes, Set<Edge> edges) {
        int[] successors = new int[edges.size()];
        int[] cursor = outgoingOffsets.clone();
        for (Edge edge : edges) {
            int from = indexes.get(edge.from);
            successors[cursor[from]++] = indexes.get(edge.to);
        }
        return successors;
    }

    public Instruction instructionAt(int address) {
        int id = Arrays.binarySearch(addresses, address);
        return id < 0 ? null : instructions[id];
    }

    public void instructions(Consumer<? super Instruction> visitor) {
        for (Instruction instruction : instructions) if (instruction != null) visitor.accept(instruction);
    }

    public void successors(int address, EdgeVisitor visitor) {
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
