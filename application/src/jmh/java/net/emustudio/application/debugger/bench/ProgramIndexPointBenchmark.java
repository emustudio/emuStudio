/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.BaselineAnnotationIndex;
import net.emustudio.application.debugger.index.ProgramIndex;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/** Same starting-address semantics against the frozen production string-key implementation. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexPointBenchmark {
    @State(Scope.Thread)
    public static class Points {
        @Param({"1", "1000", "10000", "50000"}) public int entries;
        @Param({"single", "banked"}) public String layout;
        BenchmarkData data;
        BaselineAnnotationIndex baseline;
        ProgramIndex.Snapshot snapshot;
        ProgramIndex.Layout layoutMetadata;
        final BenchmarkData.AnnotationSink sink = new BenchmarkData.AnnotationSink();

        @Setup public void setup() {
            data = new BenchmarkData(layout, entries);
            layoutMetadata = ProgramIndex.create("bench", data.size, data.banks, data.common).snapshot().layout();
            baseline = data.baseline(layoutMetadata);
            snapshot = data.index(false).snapshot();
        }
    }

    @Benchmark public Object oldPointHit(Points state) {
        int i = state.data.next();
        return state.baseline.get(state.data.bank[i], state.data.address[i]);
    }

    @Benchmark public Object indexedPointHit(Points state) {
        int i = state.data.next();
        state.sink.last = null;
        state.snapshot.forEachAnnotationStartingAt(state.data.bank[i], state.data.address[i], state.sink);
        return state.sink.last;
    }

    @Benchmark public Object oldPointMiss(Points state) {
        return state.baseline.get((state.data.cursor++ & Integer.MAX_VALUE) % state.data.banks, state.data.size - 1);
    }

    @Benchmark public Object indexedPointMiss(Points state) {
        state.sink.last = null;
        state.snapshot.forEachAnnotationStartingAt((state.data.cursor++ & Integer.MAX_VALUE) % state.data.banks,
                state.data.size - 1, state.sink);
        return state.sink.last;
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object oldFullRebuild(Points state) { return state.data.baseline(state.layoutMetadata); }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object indexedFullRebuild(Points state) { return state.data.index(false); }
}
