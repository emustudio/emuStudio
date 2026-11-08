/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.emulib.runtime.annotations.AnnotationEngine;
import net.emustudio.application.debugger.AnnotationIndex;
import net.emustudio.application.debugger.BaselineAnnotationIndex;
import net.emustudio.application.debugger.index.ProgramIndex;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** The real UI-facing adapter, distinct from the raw ProgramIndex starting-address benchmark. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexAdapterBenchmark {
    @State(Scope.Thread)
    public static class Adapters {
        @Param({"1", "1000", "10000", "50000"}) public int entries;
        @Param({"single", "clustered"}) public String layout;
        BenchmarkData data;
        AnnotationIndex adapter;
        BaselineAnnotationIndex baseline;
        ProgramIndex.Layout layoutMetadata;

        @Setup public void setup() throws IOException {
            data = new BenchmarkData(layout, entries);
            layoutMetadata = ProgramIndex.create("bench", data.size, new AnnotationEngine()).snapshot().layout();
            baseline = data.baseline(layoutMetadata);
            adapter = new AnnotationIndex(ProgramIndex.create("bench", data.size, new AnnotationEngine()));
            adapter.importProperties(baseline.properties());
            // Build the adapter's immutable projection before timing steady-state point queries.
            adapter.get(data.address[0]);
            for (int i = 0; i < data.count; i++) {
                AnnotationIndex.Entry current = adapter.get(data.address[i]);
                BaselineAnnotationIndex.Entry old = baseline.get(data.address[i]);
                if (current == null || old == null || current.address != old.address
                        || !current.label.equals(old.label)
                        || !current.kind.equals(old.kind))
                    throw new IllegalStateException("Annotation adapter workload differs from baseline");
            }
        }
    }

    @Benchmark public Object adapterPointHit(Adapters state) {
        int i = state.data.next();
        return state.adapter.get(state.data.address[i]);
    }

    @Benchmark public Object adapterPointMiss(Adapters state) {
        return state.adapter.get(state.data.size - 1);
    }

    @Benchmark public Object oldPointHit(Adapters state) {
        int i = state.data.next();
        return state.baseline.get(state.data.address[i]);
    }

    @Benchmark public Object oldPointMiss(Adapters state) {
        return state.baseline.get(state.data.size - 1);
    }
}
