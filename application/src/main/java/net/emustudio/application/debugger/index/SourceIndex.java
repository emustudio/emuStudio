/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Reverse mappings are independent interval trees for each exact document revision. */
final class SourceIndex {
    private final ProgramIndex.Mapping[] values;
    private final Map<String, Revisions> files = new HashMap<>();

    SourceIndex(ProgramIndex.Mapping[] values) {
        this.values = values;
        Map<String, Map<Long, List<IntervalTable.Entry>>> entries = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            ProgramIndex.SourceSpan s = values[i].source();
            entries.computeIfAbsent(s.fileId, ignored -> new TreeMap<>())
                    .computeIfAbsent(s.sourceRevision, ignored -> new ArrayList<>())
                    .add(new IntervalTable.Entry(s.startOffset, s.endOffset, i));
        }
        entries.forEach((file, revisions) -> files.put(file, new Revisions(revisions)));
    }

    void overlapping(ProgramIndex.SourceSpan span, Consumer<? super ProgramIndex.Mapping> visitor) {
        Revisions revisions = files.get(span.fileId);
        if (revisions == null) return;
        int revision = Arrays.binarySearch(revisions.ids, span.sourceRevision);
        if (revision >= 0) revisions.trees[revision].query(span.startOffset, span.endOffset,
                values, visitor, null, -1, 0, 0);
    }

    private static final class Revisions {
        final long[] ids;
        final IntervalTable[] trees;
        Revisions(Map<Long, List<IntervalTable.Entry>> revisions) {
            ids = new long[revisions.size()];
            trees = new IntervalTable[ids.length];
            int i = 0;
            for (Map.Entry<Long, List<IntervalTable.Entry>> e : revisions.entrySet()) {
                ids[i] = e.getKey();
                trees[i++] = new IntervalTable(e.getValue());
            }
        }
    }
}
