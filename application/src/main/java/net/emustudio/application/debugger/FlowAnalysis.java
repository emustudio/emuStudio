/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/** Decoder adapter for bounded static analysis. The published graph lives in ProgramIndex. */
public final class FlowAnalysis {
    private final ProgramIndex index;
    private final ProgramIndex.Snapshot snapshot;
    private final Map<Integer, String> listing;
    private final int bank;

    private FlowAnalysis(ProgramIndex index, ProgramIndex.Snapshot snapshot,
                         Map<Integer, String> listing, int bank) {
        this.index = index;
        this.snapshot = snapshot;
        this.listing = Map.copyOf(listing);
        this.bank = bank;
    }

    /**
     * Decode live memory on a worker while the machine is paused. The session adapter must reject
     * a changed machine, memory revision, or disposed session before publishing the staged graph.
     * The limit bounds attempted undecodable addresses as well as scheduled instructions.
     */
    public static Pending reachable(Disassembler disassembler, int start, int limit,
                                    ProgramIndex index, int bank) {
        return reachable(disassembler, start, limit, index, index.snapshot(), bank);
    }

    /** Uses the metadata root from before live decoding. */
    public static Pending reachable(Disassembler disassembler, int start, int limit,
                                    ProgramIndex index, ProgramIndex.Snapshot base, int bank) {
        Builder builder = new Builder(disassembler, limit, index, base, bank);
        builder.base.layout().physicalBank(bank, start);
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
        return new Pending(builder);
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    public boolean isCurrent(ProgramIndex currentIndex) {
        if (currentIndex != index) return false;
        ProgramIndex.Snapshot current = index.snapshot();
        return current.contentRevision() == snapshot.contentRevision()
                && current.memoryRevision() == snapshot.memoryRevision()
                && current.flowState() != ProgramIndex.FlowState.STALE
                && current.flowState() != ProgramIndex.FlowState.NOT_BUILT;
    }

    public int bank() { return bank; }
    public boolean truncated() { return snapshot.flowState() == ProgramIndex.FlowState.TRUNCATED; }
    public String mnemonic(int address) { return listing.getOrDefault(address, ""); }

    public void forEachInstruction(Consumer<? super ProgramIndex.Instruction> visitor) {
        snapshot.forEachInstruction(visitor);
    }

    public void forEachEdge(ProgramIndex.EdgeVisitor visitor) {
        Objects.requireNonNull(visitor);
        snapshot.forEachInstruction(instruction -> snapshot.forEachSuccessor(
                instruction.cells().bank, instruction.cells().start, visitor));
    }

    /** Staged decoder output; publish only after the adapter validates its paused session. */
    public static final class Pending {
        private final Builder builder;
        private boolean consumed;

        private Pending(Builder builder) { this.builder = builder; }

        public FlowAnalysis publish() {
            if (consumed) throw new IllegalStateException("Flow result already consumed");
            consumed = true;
            checkCancelled();
            try (ProgramIndex.ContentUpdate update = builder.index.updateContent(builder.base)) {
                update.replaceFlow(builder.truncated);
                for (Decoded decoded : builder.instructions.values())
                    update.putInstruction(decoded.cells, decoded.kind);
                for (Edge edge : builder.edges)
                    update.putEdge(builder.bank, edge.from, edge.to, edge.edgeKind);
                if (update.commit() != ProgramIndex.CommitResult.PUBLISHED)
                    throw new IllegalStateException("Flow analysis discarded: program metadata or memory changed");
            }
            ProgramIndex.Snapshot published = builder.index.snapshot();
            if (published.contentRevision() != builder.base.contentRevision() + 1
                    || published.memoryRevision() != builder.base.memoryRevision())
                throw new IllegalStateException("Flow analysis discarded: graph replaced during publication");
            return new FlowAnalysis(builder.index, published, builder.listing, builder.bank);
        }
    }

    private static final class Builder {
        final Disassembler disassembler;
        final ProgramIndex index;
        final ProgramIndex.Snapshot base;
        final int bank, size, edgeLimit;
        final Map<Integer, Decoded> instructions = new LinkedHashMap<>();
        final Map<Integer, String> listing = new LinkedHashMap<>();
        final List<Edge> edges = new ArrayList<>();
        final DataAt data = new DataAt();
        int targetAttempts;
        boolean truncated;

        Builder(Disassembler disassembler, int limit, ProgramIndex index, ProgramIndex.Snapshot base, int bank) {
            if (limit < 1 || limit > 100000) throw new IllegalArgumentException("Invalid flow bounds");
            this.disassembler = Objects.requireNonNull(disassembler);
            this.index = Objects.requireNonNull(index);
            this.base = Objects.requireNonNull(base);
            this.bank = bank;
            this.size = base.layout().addressSpaceSize();
            base.layout().physicalBank(bank, 0);
            // Bound even a malformed decoder returning a huge target list. Partial results say TRUNCATED.
            edgeLimit = limit * 8;
        }

        Decoded decode(int address) {
            data.found = false;
            base.forEachAnnotationCovering(bank, address, data);
            if (data.found) return null;
            try {
                Optional<InstructionFlow> result = disassembler.getFlow(address);
                if (result.isEmpty()) return null;
                InstructionFlow flow = result.get();
                int end = disassembler.getNextInstructionPosition(address);
                if (end <= address || end > size) return null;
                String mnemonic = disassembler.disassemble(address).mnemo;
                ProgramIndex.Range cells = new ProgramIndex.Range(bank, address, end);
                List<Integer> destinations = new ArrayList<>();
                ProgramIndex.EdgeKind targetKind = flow.kind == InstructionFlow.Kind.CALL
                        ? ProgramIndex.EdgeKind.CALL : ProgramIndex.EdgeKind.BRANCH;
                for (int target : flow.targets) {
                    checkCancelled();
                    if (edges.size() == edgeLimit || targetAttempts == edgeLimit) { truncated = true; break; }
                    targetAttempts++;
                    if (target >= 0 && target < size) {
                        addEdge(address, target, targetKind);
                        destinations.add(target);
                    }
                }
                if (flow.fallThrough && flow.next >= 0 && flow.next < size) {
                    if (edges.size() == edgeLimit) truncated = true;
                    else {
                        addEdge(address, flow.next, ProgramIndex.EdgeKind.FALL_THROUGH);
                        destinations.add(flow.next);
                    }
                }
                Decoded decoded = new Decoded(cells, ProgramIndex.InstructionKind.valueOf(flow.kind.name()), destinations);
                instructions.put(address, decoded);
                listing.put(address, mnemonic);
                return decoded;
            } catch (InvalidInstructionException | IndexOutOfBoundsException | IllegalArgumentException ignored) {
                return null;
            }
        }

        private void addEdge(int from, int to, ProgramIndex.EdgeKind kind) {
            edges.add(new Edge(from, to, kind));
        }

    }

    private static final class DataAt implements Consumer<ProgramIndex.Annotation> {
        boolean found;
        @Override public void accept(ProgramIndex.Annotation annotation) {
            found |= annotation.kind().equals(ProgramIndex.Kinds.DATA);
        }
    }

    private static final class Decoded {
        final ProgramIndex.Range cells;
        final ProgramIndex.InstructionKind kind;
        final List<Integer> destinations;

        Decoded(ProgramIndex.Range cells, ProgramIndex.InstructionKind kind, List<Integer> destinations) {
            this.cells = cells;
            this.kind = kind;
            this.destinations = destinations;
        }
    }

    public static final class Edge {
        public final int from, to;
        public final String kind;
        private final ProgramIndex.EdgeKind edgeKind;

        public Edge(int from, int to, ProgramIndex.EdgeKind kind) {
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
