/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Static centered interval tree. Nodes and crossing postings live in primitive arrays. */
final class IntervalTable {
    static final class Entry {
        final int start, end, id;
        Entry(int start, int end, int id) { this.start = start; this.end = end; this.id = id; }
    }

    private final int[] centers, left, right, offsets, counts;
    private final int[] starts, startIds, ends, endIds;
    private int nodes, posting;

    IntervalTable(List<Entry> entries) {
        int size = entries.size();
        centers = new int[size];
        left = new int[size];
        right = new int[size];
        offsets = new int[size];
        counts = new int[size];
        starts = new int[size];
        startIds = new int[size];
        ends = new int[size];
        endIds = new int[size];
        if (!entries.isEmpty()) build(new ArrayList<>(entries));
    }

    private int build(List<Entry> entries) {
        int node = nodes++;
        int[] midpoints = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            midpoints[i] = e.start + (e.end - e.start) / 2;
        }
        Arrays.sort(midpoints);
        int center = midpoints[midpoints.length / 2];
        centers[node] = center;
        List<Entry> before = new ArrayList<>(), after = new ArrayList<>(), crossing = new ArrayList<>();
        for (Entry e : entries) {
            if (e.end <= center) before.add(e);
            else if (e.start > center) after.add(e);
            else crossing.add(e);
        }
        int at = posting;
        offsets[node] = at;
        counts[node] = crossing.size();
        posting += crossing.size();
        crossing.sort(Comparator.comparingInt((Entry e) -> e.start).thenComparingInt(e -> e.id));
        for (int i = 0; i < crossing.size(); i++) {
            starts[at + i] = crossing.get(i).start;
            startIds[at + i] = crossing.get(i).id;
        }
        crossing.sort(Comparator.comparingInt((Entry e) -> e.end).reversed().thenComparingInt(e -> e.id));
        for (int i = 0; i < crossing.size(); i++) {
            ends[at + i] = crossing.get(i).end;
            endIds[at + i] = crossing.get(i).id;
        }
        left[node] = before.isEmpty() ? -1 : build(before);
        right[node] = after.isEmpty() ? -1 : build(after);
        return node;
    }

    <T> void query(int from, int to, T[] values, Consumer<? super T> visitor,
                   ProgramIndex.Range[] ranges, int skipBank, int skipFrom, int skipTo) {
        if (nodes != 0) query(0, from, to, values, visitor, ranges, skipBank, skipFrom, skipTo);
    }

    private <T> void query(int node, int from, int to, T[] values, Consumer<? super T> visitor,
                          ProgramIndex.Range[] ranges, int skipBank, int skipFrom, int skipTo) {
        int offset = offsets[node], count = counts[node], center = centers[node];
        if (to <= center) {
            for (int i = offset; i < offset + count && starts[i] < to; i++)
                emit(startIds[i], values, visitor, ranges, skipBank, skipFrom, skipTo);
            if (left[node] >= 0) query(left[node], from, to, values, visitor, ranges, skipBank, skipFrom, skipTo);
        } else if (from > center) {
            for (int i = offset; i < offset + count && ends[i] > from; i++)
                emit(endIds[i], values, visitor, ranges, skipBank, skipFrom, skipTo);
            if (right[node] >= 0) query(right[node], from, to, values, visitor, ranges, skipBank, skipFrom, skipTo);
        } else {
            for (int i = offset; i < offset + count; i++)
                emit(startIds[i], values, visitor, ranges, skipBank, skipFrom, skipTo);
            if (left[node] >= 0) query(left[node], from, to, values, visitor, ranges, skipBank, skipFrom, skipTo);
            if (right[node] >= 0) query(right[node], from, to, values, visitor, ranges, skipBank, skipFrom, skipTo);
        }
    }

    private static <T> void emit(int id, T[] values, Consumer<? super T> visitor,
                                 ProgramIndex.Range[] ranges, int skipBank, int skipFrom, int skipTo) {
        if (skipBank >= 0) {
            ProgramIndex.Range r = ranges[id];
            if (r.bank == skipBank && r.start < skipTo && r.end > skipFrom) return;
        }
        visitor.accept(values[id]);
    }
}
