/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

// Benchmark-only baseline from 184f8d5a49fd40d99e553a80496c508e2e699618; class names only changed.

import net.emustudio.emulib.plugins.cpu.*;

import java.util.*;

/**
 * Bounded static reachability; indirect targets stay unresolved and data annotations stop decoding.
 */
public final class BaselineFlowAnalysis {
    public final Map<Integer, InstructionFlow> instructions = new LinkedHashMap<>();
    public final Map<Integer, String> listing = new LinkedHashMap<>();
    public final List<Edge> edges = new ArrayList<>();
    public boolean truncated;

    public BaselineFlowAnalysis(
            Disassembler dis, int start, int limit, BaselineAnnotationIndex annotations) {
        if (start < 0 || limit < 1 || limit > 10000)
            throw new IllegalArgumentException("Invalid flow bounds");
        Deque<Integer> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty() && instructions.size() < limit) {
            int at = pending.removeFirst();
            if (instructions.containsKey(at)) continue;
            try {
                BaselineAnnotationIndex.Entry annotation =
                        annotations == null ? null : annotations.get(at);
                if (annotation != null && annotation.kind.equals("DATA")) continue;
                Optional<InstructionFlow> decoded = dis.getFlow(at);
                if (decoded.isEmpty()) continue;
                InstructionFlow flow = decoded.get();
                instructions.put(at, flow);
                listing.put(at, dis.disassemble(at).mnemo);
                for (int target : flow.targets) {
                    edges.add(
                            new Edge(
                                    at,
                                    target,
                                    flow.kind == InstructionFlow.Kind.CALL ? "call" : "branch"));
                    pending.add(target);
                }
                if (flow.fallThrough && flow.next != at) {
                    edges.add(new Edge(at, flow.next, "next"));
                    pending.add(flow.next);
                }
            } catch (InvalidInstructionException
                    | IndexOutOfBoundsException
                    | IllegalArgumentException ignored) {
            }
        }
        truncated = !pending.isEmpty();
    }

    public List<Edge> callers(int address) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) if (e.to == address) result.add(e);
        return result;
    }

    public static final class Edge {
        public final int from, to;
        public final String kind;

        public Edge(int from, int to, String kind) {
            this.from = from;
            this.to = to;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return String.format("%04X → %04X (%s)", from, to, kind);
        }
    }
}
