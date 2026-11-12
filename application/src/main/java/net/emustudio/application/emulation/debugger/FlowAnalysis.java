/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/** Immutable view of instructions and transitions actually observed during execution. */
public final class FlowAnalysis {
    private final FlowGraph graph;
    private final Map<Integer, DecodedInstruction> instructions;
    private final Map<Integer, String> unknown;
    private final MemoryAnnotations annotations;

    private FlowAnalysis(Map<Integer, DecodedInstruction> captured, Set<FlowGraph.Edge> edges,
                         Set<Integer> destinations, MemoryAnnotations annotations) {
        instructions = Map.copyOf(captured);
        Map<Integer, FlowGraph.Instruction> geometry = new TreeMap<>();
        captured.forEach((address, instruction) -> geometry.put(address, new FlowGraph.Instruction(
                new FlowGraph.Range(address, (int) Math.min(Integer.MAX_VALUE, (long) address + instruction.length())))));
        graph = new FlowGraph(geometry, edges);
        Map<Integer, String> pending = new TreeMap<>();
        for (int address : destinations) if (!captured.containsKey(address))
            pending.put(address, "Observed next PC; not executed yet");
        unknown = Map.copyOf(pending);
        this.annotations = annotations;
    }

    public boolean isCurrent(DebuggerSession session) { return session.flowAnalysis() == this; }
    public FlowGraph.Instruction instructionAt(int address) { return graph.instructionAt(address); }
    public int nextAddress(int address) {
        FlowGraph.Instruction instruction = instructionAt(address);
        return instruction == null ? -1 : instruction.cells().end;
    }
    public String mnemonic(int address) {
        DecodedInstruction instruction = instructions.get(address);
        return instruction == null ? "" : instruction.mnemonic();
    }
    public Map<Integer, String> unknown() { return unknown; }
    public boolean isData(int address) {
        return annotations.at(address).stream().anyMatch(annotation ->
                (annotation.ownerId == UserAnnotations.OWNER && annotation.kind.equals("DATA"))
                        || annotation.kind.equals("data"));
    }

    /** Folding follows observed adjacency only; no opcode or transfer classification. */
    public List<List<Integer>> instructionRuns(Set<Integer> addresses, Set<Integer> boundaries) {
        Map<Integer, List<Integer>> incoming = new HashMap<>();
        Map<Integer, List<Integer>> outgoing = new HashMap<>();
        forEachEdge((from, to) -> {
            incoming.computeIfAbsent(to, ignored -> new ArrayList<>()).add(from);
            outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
        });
        List<List<Integer>> runs = new ArrayList<>();
        Integer previous = null;
        List<Integer> run = null;
        for (int address : new TreeSet<>(addresses)) {
            DecodedInstruction next = instructions.get(address);
            DecodedInstruction before = previous == null ? null : instructions.get(previous);
            List<Integer> in = incoming.getOrDefault(address, List.of());
            List<Integer> out = outgoing.getOrDefault(previous, List.of());
            boolean extendsRun = before != null && next != null
                    && (long) previous + before.length() == address && before.length() == next.length()
                    && !boundaries.contains(previous) && !boundaries.contains(address)
                    && in.equals(List.of(previous)) && out.equals(List.of(address))
                    && !next.mnemonic().isBlank() && before.mnemonic().equals(next.mnemonic())
                    && !next.encoding().isBlank() && before.encoding().equals(next.encoding());
            if (!extendsRun) {
                run = new ArrayList<>();
                runs.add(run);
            }
            run.add(address);
            previous = address;
        }
        return runs;
    }

    public void forEachInstruction(Consumer<? super FlowGraph.Instruction> visitor) { graph.instructions(visitor); }
    public void forEachEdge(FlowGraph.EdgeVisitor visitor) {
        Objects.requireNonNull(visitor);
        graph.instructions(instruction -> graph.successors(instruction.cells().start, visitor));
    }

    /** Session-confined accumulation; only an explicit successful CPU reset clears observations. */
    static final class Observations {
        private final NavigableMap<Integer, DecodedInstruction> instructions = new TreeMap<>();
        private final Map<Integer, Set<Integer>> successors = new TreeMap<>();
        private int lastExecutedAddress = -1;

        void executed(DecodedInstruction instruction, OptionalInt nextAddress) {
            lastExecutedAddress = instruction.address();
            instructions.put(instruction.address(), instruction);
            if (nextAddress.isPresent() && nextAddress.getAsInt() >= 0)
                successors.computeIfAbsent(instruction.address(), ignored -> new TreeSet<>()).add(nextAddress.getAsInt());
        }

        void reset() {
            instructions.clear();
            successors.clear();
            lastExecutedAddress = -1;
        }

        int lastExecutedAddress() { return lastExecutedAddress; }

        List<Integer> instructionsBefore(int address, int count) {
            List<Integer> previous = new ArrayList<>();
            for (int at : instructions.headMap(address, false).descendingKeySet()) {
                if (previous.size() == count) break;
                previous.add(at);
            }
            Collections.reverse(previous);
            return previous;
        }

        int nextInstruction(int address) {
            Integer next = instructions.higherKey(address);
            return next == null ? -1 : next;
        }

        FlowAnalysis view(int root, MemoryAnnotations annotations) {
            if (root < 0) throw new IllegalArgumentException("Invalid observed flow root");
            Set<Integer> reachable = reachableFrom(root);
            Map<Integer, DecodedInstruction> selected = new TreeMap<>();
            for (int address : reachable) {
                DecodedInstruction instruction = instructions.get(address);
                if (instruction != null) selected.put(address, instruction);
            }
            Set<FlowGraph.Edge> edges = new TreeSet<>();
            for (int address : selected.keySet()) {
                for (int target : successors.getOrDefault(address, Set.of())) {
                    if (reachable.contains(target)) edges.add(new FlowGraph.Edge(address, target));
                }
            }
            return new FlowAnalysis(selected, edges, reachable, annotations);
        }

        private Set<Integer> reachableFrom(int root) {
            Set<Integer> reachable = new HashSet<>();
            Deque<Integer> pending = new ArrayDeque<>();
            if (instructions.containsKey(root) || successors.values().stream().anyMatch(targets -> targets.contains(root))) {
                pending.add(root);
                reachable.add(root);
            }
            while (!pending.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                int address = pending.removeFirst();
                if (!instructions.containsKey(address)) continue;
                for (int target : successors.getOrDefault(address, Set.of())) {
                    if (reachable.add(target)) pending.addLast(target);
                }
            }
            return reachable;
        }
    }

    public static final class Edge {
        public final int from, to;
        public Edge(int from, int to) { this.from = from; this.to = to; }
        @Override public String toString() { return String.format("%04X → %04X", from, to); }
    }
}
