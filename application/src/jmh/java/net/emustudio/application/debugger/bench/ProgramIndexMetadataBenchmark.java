/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.index.ProgramIndex;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/** Interval/source features had no equivalent old index; scan methods are independent oracles. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexMetadataBenchmark {
    @State(Scope.Thread)
    public static class Metadata {
        @Param({"1", "1000", "10000", "50000"}) public int entries;
        @Param({"single", "clustered", "sparse"}) public String layout;
        BenchmarkData data;
        ProgramIndex index;
        ProgramIndex.Snapshot snapshot;
        final BenchmarkData.AnnotationSink annotations = new BenchmarkData.AnnotationSink();
        final BenchmarkData.MappingSink mappings = new BenchmarkData.MappingSink();

        @Setup public void setup() {
            data = new BenchmarkData(layout, entries);
            index = data.index();
            snapshot = index.snapshot();
        }
    }

    @Benchmark public long pointCovering(Metadata state) {
        int i = state.data.next();
        state.annotations.checksum = 0;
        state.snapshot.forEachAnnotationCovering(state.data.address[i], state.annotations);
        return state.annotations.checksum;
    }

    @Benchmark public long sourcePoint(Metadata state) {
        int i = state.data.next();
        state.mappings.checksum = 0;
        state.snapshot.forEachMappingAt(state.data.address[i], state.mappings);
        return state.mappings.checksum;
    }

    @Benchmark public long sourceReverseOverlap(Metadata state) {
        state.mappings.checksum = 0;
        state.snapshot.forEachMappingOverlappingSource(state.data.sources[state.data.next()], state.mappings);
        return state.mappings.checksum;
    }

    @Benchmark public long scanSourceOracle(Metadata state) {
        ProgramIndex.SourceSpan query = state.data.sources[state.data.next()];
        long result = 0;
        for (int i = 0; i < state.data.count; i++) {
            ProgramIndex.SourceSpan source = state.data.sources[i];
            if (source.fileId.equals(query.fileId) && source.sourceRevision == query.sourceRevision
                    && source.startOffset < query.endOffset && query.startOffset < source.endOffset) result += i + 1;
        }
        return result;
    }

    @Benchmark public Object snapshot(Metadata state) { return state.index.snapshot(); }

    @Benchmark public ProgramIndex.CommitResult contentPublication(Metadata state) {
        try (ProgramIndex.ContentUpdate update = state.index.updateContent(state.index.snapshot())) {
            return update.commit();
        }
    }

    @Benchmark public ProgramIndex.CommitResult breakpointPublication(Metadata state) {
        try (ProgramIndex.BreakpointUpdate update = state.index.updateBreakpoints(state.index.snapshot())) {
            return update.commit();
        }
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ProgramIndex.CommitResult annotationEdit(Metadata state) {
        int i = state.data.next();
        try (ProgramIndex.ContentUpdate update = state.index.updateContent(state.index.snapshot())) {
            update.putAnnotation(1, i, state.data.ranges[i], ProgramIndex.Kinds.LABEL, state.data.labels[i]);
            return update.commit();
        }
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ProgramIndex.CommitResult mappingEdit(Metadata state) {
        int i = state.data.next();
        try (ProgramIndex.ContentUpdate update = state.index.updateContent(state.index.snapshot())) {
            update.putMapping(2, i, state.data.ranges[i], state.data.sources[i]);
            return update.commit();
        }
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object fullRebuild(Metadata state) { return state.data.index(); }
}
