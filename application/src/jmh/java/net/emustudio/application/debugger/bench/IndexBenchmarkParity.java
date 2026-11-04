/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.BaselineAnnotationIndex;
import net.emustudio.application.debugger.index.ProgramIndex;

/** Checks the workload semantics independently of timing, including tiny entry ID zero. */
public final class IndexBenchmarkParity {
    private IndexBenchmarkParity() { }

    public static void main(String[] arguments) {
        int cases = 0;
        for (String layout : new String[] {"single", "clustered", "sparse"}) {
            for (int entries : new int[] {1, 1000, 10000, 50000}) {
                ProgramIndexMetadataBenchmark.Metadata state = new ProgramIndexMetadataBenchmark.Metadata();
                state.layout = layout;
                state.entries = entries;
                state.setup();
                ProgramIndexMetadataBenchmark benchmark = new ProgramIndexMetadataBenchmark();
                for (int query = 0; query < 16; query++) {
                    state.data.cursor = query;
                    long indexed = benchmark.sourceReverseOverlap(state);
                    state.data.cursor = query;
                    equal(indexed, benchmark.scanSourceOracle(state), "source overlap " + layout + " " + entries);
                    int point = state.data.queries[query];
                    long expected = 0;
                    for (int i = 0; i < entries; i++)
                        if (state.data.overlaps(state.data.ranges[i],
                                state.data.address[point], state.data.address[point] + 1)) expected += i + 1;
                    state.data.cursor = query;
                    equal(expected, benchmark.pointCovering(state), "annotation covering " + layout + " " + entries);
                    state.data.cursor = query;
                    equal(expected, benchmark.sourcePoint(state), "source point " + layout + " " + entries);
                }
                cases++;
                if (!layout.equals("sparse")) {
                    ProgramIndexPointBenchmark.Points points = new ProgramIndexPointBenchmark.Points();
                    points.layout = layout;
                    points.entries = entries;
                    points.setup();
                    for (int query = 0; query < 16; query++) {
                        int point = points.data.queries[query];
                        BaselineAnnotationIndex.Entry old = points.baseline.get(points.data.address[point]);
                        points.sink.last = null;
                        points.snapshot.forEachAnnotationStartingAt(points.data.address[point], points.sink);
                        ProgramIndex.Annotation current = points.sink.last;
                        if (old == null || current == null || !old.label.equals(current.text())
                                || old.address != current.range().start)
                            throw new IllegalStateException("Point baseline mismatch " + layout + " " + entries);
                    }
                    cases++;
                }
            }
            for (int nodes : new int[] {1, 1000, 10000}) {
                ProgramIndexGraphBenchmark.Graphs state = new ProgramIndexGraphBenchmark.Graphs();
                state.layout = layout;
                state.nodes = nodes;
                state.setup();
                ProgramIndexGraphBenchmark benchmark = new ProgramIndexGraphBenchmark();
                for (int query = 0; query < 16; query++) {
                    state.cursor = query;
                    long old = benchmark.scanSuccessorsOracle(state);
                    state.cursor = query;
                    equal(old, benchmark.indexedSuccessors(state), "graph outgoing " + layout + " " + nodes);
                }
                cases++;
            }
            for (int rules : new int[] {0, 1, 1000, 10000}) {
                ProgramIndexBreakpointBenchmark.Rules state = new ProgramIndexBreakpointBenchmark.Rules();
                state.layout = layout;
                state.rules = rules;
                state.setup();
                ProgramIndexBreakpointBenchmark benchmark = new ProgramIndexBreakpointBenchmark();
                for (int query = 0; query < 16; query++) {
                    state.cursor = query;
                    boolean scan = benchmark.scanPresenceOracle(state);
                    state.cursor = query;
                    if (scan != benchmark.indexedHit(state))
                        throw new IllegalStateException("Breakpoint presence mismatch " + layout + " " + rules);
                }
                cases++;
            }
        }
        System.out.println("PASS " + cases + " benchmark datasets; point, interval, source, CSR and breakpoint semantics match");
    }

    private static void equal(long expected, long actual, String label) {
        if (expected != actual) throw new IllegalStateException(label + ": " + expected + " != " + actual);
    }
}
