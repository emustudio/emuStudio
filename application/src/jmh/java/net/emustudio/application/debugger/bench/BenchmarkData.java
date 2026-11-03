/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.BaselineAnnotationIndex;
import net.emustudio.application.debugger.index.ProgramIndex;

import java.util.Random;
import java.util.function.Consumer;

/** Shared deterministic datasets. Construction is outside timed query methods. */
final class BenchmarkData {
    final String layout;
    final int size, banks, common, count;
    final int[] bank, address, queries = new int[1024];
    final ProgramIndex.Range[] ranges;
    final ProgramIndex.SourceSpan[] sources;
    final String[] labels;
    int cursor;

    BenchmarkData(String layout, int count) {
        this.layout = layout;
        this.count = count;
        size = layout.equals("sparse") ? Integer.MAX_VALUE : 65536;
        banks = layout.equals("single") ? 1 : 4;
        common = layout.equals("single") ? size : size - 16384;
        bank = new int[count];
        address = new int[count];
        ranges = new ProgramIndex.Range[count];
        sources = new ProgramIndex.SourceSpan[count];
        labels = new String[count];
        String[] files = {"file:///bench/0/program.asm", "file:///bench/1/program.asm",
                "file:///bench/2/program.asm", "file:///bench/3/program.asm"};
        for (int i = 0; i < count; i++) {
            if (layout.equals("single")) address[i] = i;
            else if (layout.equals("sparse")) {
                bank[i] = i % banks;
                address[i] = i % 128 == 0 ? common + i / 128 : (i / 1000) * 40000000 + i % 1000;
            } else if (i % 16 == 0) {
                bank[i] = (i / 16) % banks;
                address[i] = common + i / 16;
            } else {
                bank[i] = i % banks;
                address[i] = i / banks;
            }
            ranges[i] = new ProgramIndex.Range(bank[i], address[i], Math.min(size, address[i] + 64));
            int sourceStart = (i % 128) * 2;
            sources[i] = new ProgramIndex.SourceSpan(files[i % files.length], 1,
                    sourceStart, sourceStart + 32);
            labels[i] = "label" + i;
        }
        Random random = new Random(454);
        for (int i = 0; i < queries.length; i++) queries[i] = random.nextInt(count);
    }

    int next() { return queries[(cursor++) & (queries.length - 1)]; }

    ProgramIndex index() { return index(true); }

    ProgramIndex index(boolean includeMappings) {
        ProgramIndex index = ProgramIndex.create("bench", size, banks, common);
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            for (int i = 0; i < count; i++) {
                update.putAnnotation(1, i, ranges[i], ProgramIndex.Kinds.LABEL, labels[i]);
                if (includeMappings) update.putMapping(2, i, ranges[i], sources[i]);
            }
            update.commit();
        }
        return index;
    }

    BaselineAnnotationIndex baseline() {
        return baseline(ProgramIndex.create("bench", size, banks, common).snapshot().layout());
    }

    BaselineAnnotationIndex baseline(ProgramIndex.Layout layout) {
        BaselineAnnotationIndex index = new BaselineAnnotationIndex(layout);
        for (int i = 0; i < count; i++) index.put(new BaselineAnnotationIndex.Entry(
                bank[i], address[i], labels[i], "", false, "UNKNOWN"));
        return index;
    }

    boolean overlaps(ProgramIndex.Range entry, int queryBank, int start, int end) {
        int privateEnd = Math.min(common, Math.min(entry.end, end));
        boolean privateHit = entry.bank == queryBank && Math.max(entry.start, start) < privateEnd;
        boolean commonHit = Math.max(common, Math.max(entry.start, start)) < Math.min(entry.end, end);
        return privateHit || commonHit;
    }

    static final class AnnotationSink implements Consumer<ProgramIndex.Annotation> {
        long checksum;
        ProgramIndex.Annotation last;
        @Override public void accept(ProgramIndex.Annotation annotation) {
            checksum += annotation.entryId() + 1;
            last = annotation;
        }
    }

    static final class MappingSink implements Consumer<ProgramIndex.Mapping> {
        long checksum;
        @Override public void accept(ProgramIndex.Mapping mapping) { checksum += mapping.entryId() + 1; }
    }
}
