/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sweep explicit endpoints into roots of an immutable primitive posting tree.
 * Each endpoint path-copies at most log2(payload count) nodes; overlapping/nested
 * coverage never copies the entire active set for each address segment.
 */
final class RangePostings<T> {
    private final T[] values;
    private final int[] left, right;
    private final AddressTable addresses;

    RangePostings(ProgramIndex.Layout layout, T[] values, Function<? super T, ProgramIndex.Range> range) {
        this.values = values;
        Map<Integer, List<Integer>> events = new TreeMap<>();
        for (int i = 0; i < values.length; i++) {
            ProgramIndex.Range cells = range.apply(values[i]);
            events.computeIfAbsent(cells.start, ignored -> new ArrayList<>()).add(i + 1);
            events.computeIfAbsent(cells.end, ignored -> new ArrayList<>()).add(-i - 1);
        }
        List<AddressTable.Segment> segments = new ArrayList<>();
        PostingBuilder postings = new PostingBuilder();
        int root = 0;
        int previous = 0;
        for (Map.Entry<Integer, List<Integer>> event : events.entrySet()) {
            int at = event.getKey();
            if (previous < at && root != 0)
                segments.add(new AddressTable.Segment(previous, at, root));
            for (int change : event.getValue()) {
                int id = change > 0 ? change - 1 : -change - 1;
                root = postings.set(root, 0, values.length, id, change > 0);
            }
            previous = at;
        }
        left = Arrays.copyOf(postings.left, postings.size);
        right = Arrays.copyOf(postings.right, postings.size);
        addresses = new AddressTable(layout, segments);
    }

    void visit(int address, Consumer<? super T> visitor) {
        visitNode(addresses.get(address), visitor);
    }

    private void visitNode(int node, Consumer<? super T> visitor) {
        if (node < 0) visitor.accept(values[-node - 1]);
        else if (node > 0) {
            visitNode(left[node], visitor);
            visitNode(right[node], visitor);
        }
    }

    /** Zero is empty, negative IDs are singleton payloads, positive IDs address branch arrays. */
    private static final class PostingBuilder {
        int[] left = new int[32], right = new int[32];
        int size = 1;

        int set(int root, int from, int to, int id, boolean enabled) {
            if (root < 0 && -root - 1 == id) return enabled ? root : 0;
            if (root < 0 && !enabled) return root;
            if (to - from == 1) return enabled ? -id - 1 : 0;
            int middle = from + (to - from) / 2;
            int before = root > 0 ? left[root] : 0;
            int after = root > 0 ? right[root] : 0;
            if (root < 0) {
                if (-root - 1 < middle) before = root;
                else after = root;
            }
            if (id < middle) before = set(before, from, middle, id, enabled);
            else after = set(after, middle, to, id, enabled);
            if (before == 0 && after <= 0) return after;
            if (after == 0 && before <= 0) return before;
            if (root > 0 && before == left[root] && after == right[root]) return root;
            if (size == left.length) {
                left = Arrays.copyOf(left, left.length * 2);
                right = Arrays.copyOf(right, right.length * 2);
            }
            left[size] = before;
            right[size] = after;
            return size++;
        }
    }
}
