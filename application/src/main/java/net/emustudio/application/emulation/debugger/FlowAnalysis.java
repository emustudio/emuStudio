/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;

import java.util.*;

/** Observed instruction adjacency used by the listing and repeated-instruction folding. */
public final class FlowAnalysis {
    private FlowAnalysis() { }

    private static List<List<Integer>> instructionRuns(Map<Integer, DecodedInstruction> instructions,
            Map<Integer, ? extends Collection<Integer>> incoming,
            Map<Integer, ? extends Collection<Integer>> outgoing) {
        List<List<Integer>> runs = new ArrayList<>();
        Integer previous = null;
        List<Integer> run = null;
        for (int address : instructions.keySet()) {
            DecodedInstruction next = instructions.get(address);
            DecodedInstruction before = previous == null ? null : instructions.get(previous);
            Collection<Integer> in = incoming.get(address), out = previous == null ? null : outgoing.get(previous);
            boolean extendsRun = identicalAdjacentInstructions(before, next)
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

    /** Session-confined accumulation; only an explicit successful CPU reset clears observations. */
    static final class Observations {
        private static final int MAX_CACHED_BLOCKS = 4096;
        private static final int MAX_CACHED_STEPS = 16384;
        private final NavigableMap<Integer, DecodedInstruction> instructions = new TreeMap<>();
        private final Map<Integer, Set<Integer>> successors = new TreeMap<>();
        private final Map<Integer, Set<Integer>> predecessors = new TreeMap<>();
        // Reuse immutable block facts until any executed instruction changes the current code facts.
        private final Map<ExecutedBlock, Long> mergedBlocks = new IdentityHashMap<>();
        private final Map<DecodedInstruction, MergedStep> mergedSteps = new IdentityHashMap<>();
        private int lastExecutedAddress = -1;
        private long revision;
        private long instructionFactsGeneration;

        private static final class MergedStep {
            long generation;
            int nextAddress;

            MergedStep(long generation, int nextAddress) {
                this.generation = generation;
                this.nextAddress = nextAddress;
            }
        }

        void executed(DecodedInstruction instruction, OptionalInt nextAddress) {
            revision++;
            lastExecutedAddress = instruction.address();
            int next = nextAddress.orElse(-1);
            MergedStep merged = mergedSteps.get(instruction);
            if (merged != null && merged.generation == instructionFactsGeneration && merged.nextAddress == next) return;
            putInstruction(instruction);
            if (next >= 0) addEdge(instruction.address(), next);
            if (merged == null) {
                if (mergedSteps.size() >= MAX_CACHED_STEPS) mergedSteps.clear();
                mergedSteps.put(instruction, new MergedStep(instructionFactsGeneration, next));
            } else {
                merged.generation = instructionFactsGeneration;
                merged.nextAddress = next;
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
            mergedSteps.clear();
            instructionFactsGeneration = 0;
            lastExecutedAddress = -1;
        }

        int lastExecutedAddress() { return lastExecutedAddress; }

        long revision() { return revision; }

        List<List<Integer>> instructionRuns() {
            return FlowAnalysis.instructionRuns(instructions, predecessors, successors);
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

    }

    public static final class Edge {
        public final int from, to;
        public Edge(int from, int to) { this.from = from; this.to = to; }
        @Override public String toString() { return String.format("%04X → %04X", from, to); }
    }
}
