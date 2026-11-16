/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;

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
        return instructionRuns(instructions, incoming, outgoing, addresses, boundaries);
    }

    private static List<List<Integer>> instructionRuns(Map<Integer, DecodedInstruction> instructions,
            Map<Integer, ? extends Collection<Integer>> incoming,
            Map<Integer, ? extends Collection<Integer>> outgoing, Set<Integer> addresses, Set<Integer> boundaries) {
        List<List<Integer>> runs = new ArrayList<>();
        Integer previous = null;
        List<Integer> run = null;
        for (int address : new TreeSet<>(addresses)) {
            DecodedInstruction next = instructions.get(address);
            DecodedInstruction before = previous == null ? null : instructions.get(previous);
            Collection<Integer> in = incoming.get(address), out = previous == null ? null : outgoing.get(previous);
            boolean extendsRun = identicalAdjacentInstructions(before, next)
                    && !boundaries.contains(previous) && !boundaries.contains(address)
                    && in != null && in.size() == 1 && in.contains(previous)
                    && out != null && out.size() == 1 && out.contains(address);
            if (!extendsRun) {
                run = new ArrayList<>();
                runs.add(run);
            }
            run.add(address);
            previous = address;
        }
        return runs;
    }

    /** Presentation equality, independent of CPU opcodes or instruction classifications. */
    public static boolean identicalAdjacentInstructions(DecodedInstruction before, DecodedInstruction next) {
        return before != null && next != null
                && (long) before.address() + before.length() == next.address() && before.length() == next.length()
                && !next.mnemonic().isBlank() && before.mnemonic().equals(next.mnemonic())
                && !next.encoding().isBlank() && before.encoding().equals(next.encoding());
    }

    public void forEachInstruction(Consumer<? super FlowGraph.Instruction> visitor) { graph.instructions(visitor); }
    public void forEachEdge(FlowGraph.EdgeVisitor visitor) {
        Objects.requireNonNull(visitor);
        graph.instructions(instruction -> graph.successors(instruction.cells().start, visitor));
    }

    /** Session-confined accumulation; only an explicit successful CPU reset clears observations. */
    static final class Observations {
        private static final int MAX_CACHED_BLOCKS = 4096;
        private final NavigableMap<Integer, DecodedInstruction> instructions = new TreeMap<>();
        private final Map<Integer, Set<Integer>> successors = new TreeMap<>();
        private final Map<Integer, Set<Integer>> predecessors = new TreeMap<>();
        // Reuse immutable block facts until any executed instruction changes the current code facts.
        private final Map<ExecutedBlock, Long> mergedBlocks = new IdentityHashMap<>();
        private int lastExecutedAddress = -1;
        private long revision;
        private long instructionFactsGeneration;

        void executed(DecodedInstruction instruction, OptionalInt nextAddress) {
            revision++;
            lastExecutedAddress = instruction.address();
            putInstruction(instruction);
            if (nextAddress.isPresent() && nextAddress.getAsInt() >= 0) {
                addEdge(instruction.address(), nextAddress.getAsInt());
            }
        }

        /** A compact block supplies executed facts, not an instruction ordering to infer edges from. */
        void executed(ExecutedBlock block) {
            revision++;
            lastExecutedAddress = block.lastAddress();
            Long mergedGeneration = mergedBlocks.get(block);
            if (mergedGeneration != null && mergedGeneration == instructionFactsGeneration) return;

            for (DecodedInstruction instruction : block.instructions()) putInstruction(instruction);
            for (ExecutedBlock.Transition transition : block.transitions()) addEdge(transition.from(), transition.to());
            if (mergedBlocks.size() >= MAX_CACHED_BLOCKS && !mergedBlocks.containsKey(block)) mergedBlocks.clear();
            mergedBlocks.put(block, instructionFactsGeneration);
        }

        private void putInstruction(DecodedInstruction instruction) {
            DecodedInstruction previous = instructions.get(instruction.address());
            if (previous == null || previous.length() != instruction.length()
                    || !previous.mnemonic().equals(instruction.mnemonic())
                    || !previous.encoding().equals(instruction.encoding())) {
                instructions.put(instruction.address(), instruction);
                instructionFactsGeneration++;
            }
        }

        private void addEdge(int from, int to) {
            successors.computeIfAbsent(from, ignored -> new TreeSet<>()).add(to);
            predecessors.computeIfAbsent(to, ignored -> new TreeSet<>()).add(from);
        }

        void reset() {
            revision++;
            instructions.clear();
            successors.clear();
            predecessors.clear();
            mergedBlocks.clear();
            instructionFactsGeneration = 0;
            lastExecutedAddress = -1;
        }

        int lastExecutedAddress() { return lastExecutedAddress; }

        long revision() { return revision; }

        List<List<Integer>> instructionRuns() {
            return FlowAnalysis.instructionRuns(instructions, predecessors, successors, instructions.keySet(), Set.of());
        }

        List<Edge> edgesAt(int address) {
            List<Edge> edges = new ArrayList<>();
            for (int from : predecessors.getOrDefault(address, Set.of())) {
                if (from != address) edges.add(new Edge(from, address));
            }
            for (int to : successors.getOrDefault(address, Set.of())) edges.add(new Edge(address, to));
            edges.sort(Comparator.comparingInt((Edge edge) -> edge.from).thenComparingInt(edge -> edge.to));
            return List.copyOf(edges);
        }

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
