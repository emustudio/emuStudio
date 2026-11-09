/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.FlowGraph;
import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.emulib.plugin.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugin.cpu.InstructionFlow;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/** Immutable result of bounded static instruction-flow analysis. */
public final class FlowAnalysis {
    private final FlowGraph graph;
    private final boolean truncated;
    private final Map<Integer, String> listing;
    private final Map<Integer, String> opcodes;
    private final Map<Integer, String> unknown;
    private final Set<Integer> data;

    private FlowAnalysis(Builder builder) {
        Map<Integer, FlowGraph.Instruction> instructions = new TreeMap<>();
        builder.instructions.forEach((address, decoded) ->
                instructions.put(address, new FlowGraph.Instruction(decoded.cells, decoded.kind)));
        Set<FlowGraph.Edge> edges = new TreeSet<>();
        for (Edge edge : builder.edges) edges.add(new FlowGraph.Edge(edge.from, edge.to, edge.edgeKind));
        graph = new FlowGraph(instructions, edges);
        truncated = builder.truncated;
        listing = Map.copyOf(builder.listing);
        opcodes = Map.copyOf(builder.opcodes);
        unknown = Map.copyOf(builder.unknown);
        data = Set.copyOf(builder.data);
    }

    /**
     * Decode live memory on a worker while the machine is paused. The session adapter must reject
     * a changed machine, memory revision, or disposed session before publishing the staged graph.
     * The limit bounds visited addresses, including undecodable ones.
     */
    public static FlowAnalysis reachable(Disassembler disassembler, int start, int limit,
                                         int addressSpaceSize, MemoryAnnotations annotations) {
        Builder builder = new Builder(disassembler, limit, addressSpaceSize, annotations);
        if (start < 0 || start >= builder.size) throw new IllegalArgumentException("Address outside memory");
        Deque<Integer> pending = new ArrayDeque<>();
        Set<Integer> scheduled = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        pending.add(start);
        scheduled.add(start);
        while (!pending.isEmpty() && visited.size() < limit) {
            checkCancelled();
            int address = pending.removeFirst();
            if (!visited.add(address)) continue;
            Decoded decoded = builder.decode(address);
            if (decoded == null) continue;
            for (int destination : decoded.destinations) {
                if (scheduled.contains(destination)) continue;
                if (scheduled.size() == limit) {
                    builder.truncated = true;
                    continue;
                }
                scheduled.add(destination);
                pending.addLast(destination);
            }
        }
        builder.truncated |= !pending.isEmpty();
        return new FlowAnalysis(builder);
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    public boolean isCurrent(DebuggerSession session) { return session.flowAnalysis() == this; }

    public boolean truncated() { return truncated; }
    public FlowGraph.Instruction instructionAt(int address) { return graph.instructionAt(address); }
    public int nextAddress(int address) {
        FlowGraph.Instruction instruction = instructionAt(address);
        return instruction == null ? -1 : instruction.cells().end;
    }
    public String mnemonic(int address) { return listing.getOrDefault(address, ""); }

    /** Terminal nodes whose instruction could not be decoded, with the reason. */
    public Map<Integer, String> unknown() { return unknown; }

    /** Decoded instructions inside a DATA annotation. */
    public boolean isData(int address) { return data.contains(address); }

    /** Consecutive identical linear instructions, split at entry points and caller-supplied boundaries. */
    List<List<Integer>> instructionRuns(Set<Integer> addresses, Set<Integer> boundaries) {
        Map<Integer, FlowGraph.Instruction> instructions = new HashMap<>();
        forEachInstruction(instruction -> instructions.put(instruction.cells().start, instruction));
        Map<Integer, List<Edge>> incoming = new HashMap<>(), outgoing = new HashMap<>();
        Set<Integer> separate = new HashSet<>(boundaries);
        forEachEdge((from, to, kind) -> {
            Edge edge = new Edge(from, to, kind);
            incoming.computeIfAbsent(to, ignored -> new ArrayList<>()).add(edge);
            outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(edge);
            if (kind != FlowGraph.EdgeKind.FALL_THROUGH) separate.add(to);
        });
        List<List<Integer>> runs = new ArrayList<>();
        Integer previous = null;
        List<Integer> run = null;
        for (int address : new TreeSet<>(addresses)) {
            FlowGraph.Instruction next = instructions.get(address);
            if (next == null) {
                if (unknown.containsKey(address)) {
                    runs.add(new ArrayList<>(List.of(address)));
                    run = null;
                    previous = null;
                }
                continue;
            }
            FlowGraph.Instruction before = previous == null ? null : instructions.get(previous);
            List<Edge> in = incoming.getOrDefault(address, List.of());
            List<Edge> out = outgoing.getOrDefault(previous, List.of());
            boolean identical = before != null
                    && before.kind() == FlowGraph.InstructionKind.LINEAR
                    && next.kind() == FlowGraph.InstructionKind.LINEAR
                    && before.cells().end == address
                    && before.cells().end - before.cells().start == next.cells().end - address
                    && !separate.contains(previous) && !separate.contains(address)
                    && in.size() == 1 && out.size() == 1
                    && in.get(0).from == previous && in.get(0).edgeKind == FlowGraph.EdgeKind.FALL_THROUGH
                    && out.get(0).to == address && out.get(0).edgeKind == FlowGraph.EdgeKind.FALL_THROUGH
                    && !mnemonic(address).isBlank() && mnemonic(previous).equals(mnemonic(address))
                    && !opcodes.getOrDefault(address, "").isBlank()
                    && Objects.equals(opcodes.get(previous), opcodes.get(address));
            if (!identical) {
                run = new ArrayList<>();
                runs.add(run);
            }
            run.add(address);
            previous = address;
        }
        return runs;
    }

    public void forEachInstruction(Consumer<? super FlowGraph.Instruction> visitor) {
        graph.instructions(visitor);
    }

    public void forEachEdge(FlowGraph.EdgeVisitor visitor) {
        Objects.requireNonNull(visitor);
        graph.instructions(instruction -> graph.successors(instruction.cells().start, visitor));
    }

    private static final class Builder {
        final Disassembler disassembler;
        final MemoryAnnotations annotations;
        final int size;
        final Map<Integer, Decoded> instructions = new LinkedHashMap<>();
        final Map<Integer, String> listing = new LinkedHashMap<>();
        final Map<Integer, String> opcodes = new LinkedHashMap<>();
        final Map<Integer, String> unknown = new LinkedHashMap<>();
        final Set<Integer> data = new HashSet<>();
        final List<Edge> edges = new ArrayList<>();
        boolean truncated;

        Builder(Disassembler disassembler, int limit, int size,
                MemoryAnnotations annotations) {
            if (limit < 1 || size < 1) throw new IllegalArgumentException("Invalid flow bounds");
            this.disassembler = Objects.requireNonNull(disassembler);
            this.annotations = Objects.requireNonNull(annotations);
            this.size = size;
        }

        Decoded decode(int address) {
            try {
                Optional<InstructionFlow> result = disassembler.getFlow(address);
                if (result.isEmpty()) return unknown(address, "No flow information");
                InstructionFlow flow = result.get();
                int end = disassembler.getNextInstructionPosition(address);
                if (end <= address || end > size) return unknown(address, "Invalid instruction length");
                DisassembledInstruction decodedInstruction = disassembler.disassemble(address);
                String mnemonic = decodedInstruction.mnemo;
                FlowGraph.Range cells = new FlowGraph.Range(address, end);
                List<Integer> destinations = new ArrayList<>();
                FlowGraph.EdgeKind targetKind = flow.kind == InstructionFlow.Kind.CALL
                        ? FlowGraph.EdgeKind.CALL : FlowGraph.EdgeKind.BRANCH;
                for (int target : flow.targets) {
                    checkCancelled();
                    if (target >= 0 && target < size) {
                        addEdge(address, target, targetKind);
                        destinations.add(target);
                    }
                }
                if (flow.fallThrough && flow.next >= 0 && flow.next < size) {
                    addEdge(address, flow.next, FlowGraph.EdgeKind.FALL_THROUGH);
                    destinations.add(flow.next);
                }
                Decoded decoded = new Decoded(cells, FlowGraph.InstructionKind.valueOf(flow.kind.name()), destinations);
                instructions.put(address, decoded);
                listing.put(address, mnemonic);
                opcodes.put(address, decodedInstruction.opCode == null ? "" : decodedInstruction.opCode);
                if (annotations.at(address).stream().anyMatch(annotation ->
                        annotation.kind.equals(AnnotationIndex.USER_KIND + "DATA")
                                || annotation.kind.equals("data"))) data.add(address);
                return decoded;
            } catch (InvalidInstructionException | IndexOutOfBoundsException | IllegalArgumentException failure) {
                return unknown(address, Objects.toString(failure.getMessage(), failure.getClass().getSimpleName()));
            }
        }

        private Decoded unknown(int address, String reason) {
            unknown.put(address, reason);
            listing.put(address, "unknown");
            return null;
        }

        private void addEdge(int from, int to, FlowGraph.EdgeKind kind) {
            edges.add(new Edge(from, to, kind));
        }

    }

    private static final class Decoded {
        final FlowGraph.Range cells;
        final FlowGraph.InstructionKind kind;
        final List<Integer> destinations;

        Decoded(FlowGraph.Range cells, FlowGraph.InstructionKind kind, List<Integer> destinations) {
            this.cells = cells;
            this.kind = kind;
            this.destinations = destinations;
        }
    }

    public static final class Edge {
        public final int from, to;
        public final String kind;
        private final FlowGraph.EdgeKind edgeKind;

        public Edge(int from, int to, FlowGraph.EdgeKind kind) {
            this.from = from;
            this.to = to;
            this.edgeKind = Objects.requireNonNull(kind);
            switch (kind) {
                case CALL: this.kind = "call"; break;
                case BRANCH: this.kind = "branch"; break;
                default: this.kind = "next";
            }
        }

        @Override public String toString() {
            return String.format("%04X → %04X (%s)", from, to, kind);
        }
    }
}
