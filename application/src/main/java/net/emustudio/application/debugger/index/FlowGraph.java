/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Application instruction-flow storage: immutable CSR for published graphs and primitive
 * sequential boundaries for the disassembly adapter. Neither representation invokes a decoder.
 */
public final class FlowGraph {
    private static final ProgramIndex.EdgeKind[] KINDS = ProgramIndex.EdgeKind.values();
    private final int[] addresses, outgoingOffsets, outgoing;
    private final byte[] outgoingKinds;
    private final ProgramIndex.Instruction[] instructions;

    FlowGraph(Map<Integer, ProgramIndex.Instruction> decoded, Set<Edge> edges) {
        Set<Integer> keys = new TreeSet<>(decoded.keySet());
        for (Edge e : edges) {
            if (!decoded.containsKey(e.from)) throw new IllegalArgumentException("Edge source is not an instruction start");
            keys.add(e.to);
        }
        addresses = new int[keys.size()];
        instructions = new ProgramIndex.Instruction[keys.size()];
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
        outgoingKinds = new byte[edges.size()];
        int[] outCursor = outgoingOffsets.clone();
        for (Edge e : edges) {
            int from = ids.get(e.from) - 1, to = ids.get(e.to) - 1;
            int outAt = outCursor[from]++;
            outgoing[outAt] = to;
            outgoingKinds[outAt] = (byte) e.kind.ordinal();
        }
    }

    ProgramIndex.Instruction instructionAt(int address) {
        int id = Arrays.binarySearch(addresses, address);
        return id < 0 ? null : instructions[id];
    }

    void instructions(Consumer<? super ProgramIndex.Instruction> visitor) {
        for (ProgramIndex.Instruction instruction : instructions) if (instruction != null) visitor.accept(instruction);
    }

    void successors(int address, ProgramIndex.EdgeVisitor visitor) {
        int id = Arrays.binarySearch(addresses, address);
        if (id < 0) return;
        for (int at = outgoingOffsets[id]; at < outgoingOffsets[id + 1]; at++) {
            int to = outgoing[at];
            visitor.accept(addresses[id], addresses[to], KINDS[outgoingKinds[at]]);
        }
    }

    static final class Edge implements Comparable<Edge> {
        final int from, to;
        final ProgramIndex.EdgeKind kind;
        Edge(int from, int to, ProgramIndex.EdgeKind kind) { this.from = from; this.to = to; this.kind = kind; }
        @Override public int compareTo(Edge other) {
            int comparison = Integer.compare(from, other.from);
            if (comparison == 0) comparison = Integer.compare(to, other.to);
            return comparison == 0 ? kind.compareTo(other.kind) : comparison;
        }
    }

    /**
     * Writer-confined sequential instruction geometry. Addresses are cells; missing lookups
     * return -1. Append is amortized constant time; arbitrary insertion shifts primitive arrays.
     * This mutable discovery storage is never retained by an immutable ProgramIndex snapshot.
     */
    public static final class Boundaries {
        private int[] starts = new int[32], ends = new int[32];
        private int size;
        private int longest = 1;

        public int nextAt(int start) {
            int at = size == 0 || start > starts[size - 1]
                    ? -size - 1 : Arrays.binarySearch(starts, 0, size, start);
            return at < 0 ? -1 : ends[at];
        }

        public int previousStart(int address) {
            int at = lowerBound(address) - 1;
            return at < 0 ? -1 : starts[at];
        }

        /** Records proven nonempty coverage; replaces an obsolete reachable boundary chain. */
        public void record(int start, int end) {
            if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid instruction coverage");
            int at = size == 0 || start > starts[size - 1]
                    ? -size - 1 : Arrays.binarySearch(starts, 0, size, start);
            if (at >= 0 && ends[at] != end) {
                int original = ends[at], next = original;
                while (next >= 0 && next < end) next = nextAt(next);
                if (next >= 0) removeRange(lowerBound(original), upperBound(next));
                at = Arrays.binarySearch(starts, 0, size, start);
            }
            if (at < 0) {
                at = -at - 1;
                if (size == starts.length) {
                    starts = Arrays.copyOf(starts, starts.length * 2);
                    ends = Arrays.copyOf(ends, ends.length * 2);
                }
                System.arraycopy(starts, at, starts, at + 1, size - at);
                System.arraycopy(ends, at, ends, at + 1, size - at);
                starts[at] = start;
                size++;
            }
            ends[at] = end;
            longest = Math.max(longest, end - start);
        }

        public void remove(int start) {
            int at = Arrays.binarySearch(starts, 0, size, start);
            if (at >= 0) removeRange(at, at + 1);
        }

        public void flush(int fromInclusive, int toExclusive) {
            if (fromInclusive > toExclusive) throw new IllegalArgumentException("Reversed boundary interval");
            removeRange(lowerBound(fromInclusive), lowerBound(toExclusive));
        }

        /** Captures a stable inclusive start interval while discovery continues. No arrays escape. */
        public Window capture(int fromInclusive, int toInclusive) {
            if (fromInclusive > toInclusive) throw new IllegalArgumentException("Reversed boundary interval");
            int from = lowerBound(fromInclusive), to = upperBound(toInclusive);
            return new Window(Arrays.copyOfRange(starts, from, to), Arrays.copyOfRange(ends, from, to));
        }

        public int longestInstructionSize() { return longest; }

        private int lowerBound(int value) {
            int at = Arrays.binarySearch(starts, 0, size, value);
            return at < 0 ? -at - 1 : at;
        }
        private int upperBound(int value) {
            int at = Arrays.binarySearch(starts, 0, size, value);
            return at < 0 ? -at - 1 : at + 1;
        }
        private void removeRange(int from, int to) {
            System.arraycopy(starts, to, starts, from, size - to);
            System.arraycopy(ends, to, ends, from, size - to);
            size -= to - from;
        }

        public static final class Window {
            private final int[] starts, ends;
            private Window(int[] starts, int[] ends) { this.starts = starts; this.ends = ends; }
            public int size() { return starts.length; }
            public int startAt(int index) { return starts[index]; }
            public int endAt(int index) { return ends[index]; }
        }
    }
}
