/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** A logical range has one payload, even when its physical postings cross common memory. */
final class RangeIndex<T> {
    private final T[] values;
    private final int[] banks;
    private final IntervalTable[] intervals;
    private final AddressPostings<T> starts;

    RangeIndex(ProgramIndex.Layout layout, T[] values, ProgramIndex.Range[] ranges) {
        this.values = values;
        Map<Long, List<Integer>> beginning = new TreeMap<>();
        Map<Integer, List<IntervalTable.Entry>> partitioned = new TreeMap<>();
        for (int id = 0; id < ranges.length; id++) {
            ProgramIndex.Range r = ranges[id];
            int physical = layout.physicalBank(r.bank, r.start);
            add(beginning, AddressTable.key(physical, r.start), id);
            int lowerEnd = Math.min(r.end, layout.commonBoundary());
            if (r.start < lowerEnd) addInterval(partitioned, r.bank, r.start, lowerEnd, id);
            int upperStart = Math.max(r.start, layout.commonBoundary());
            if (upperStart < r.end) addInterval(partitioned, 0, upperStart, r.end, id);
        }
        starts = new AddressPostings<>(layout, values, beginning);
        banks = new int[partitioned.size()];
        intervals = new IntervalTable[banks.length];
        int i = 0;
        for (Map.Entry<Integer, List<IntervalTable.Entry>> p : partitioned.entrySet()) {
            banks[i] = p.getKey();
            intervals[i++] = new IntervalTable(p.getValue());
        }
    }

    static void add(Map<Long, List<Integer>> map, long key, int id) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(id);
    }

    private static void addInterval(Map<Integer, List<IntervalTable.Entry>> map, int bank, int start, int end, int id) {
        map.computeIfAbsent(bank, ignored -> new ArrayList<>()).add(new IntervalTable.Entry(start, end, id));
    }

    void startingAt(int physicalBank, int address, Consumer<? super T> visitor) {
        starts.visit(physicalBank, address, visitor);
    }

    void covering(int physicalBank, int address, Consumer<? super T> visitor) {
        int at = Arrays.binarySearch(banks, physicalBank);
        if (at >= 0) intervals[at].query(address, address + 1, values, visitor);
    }
}
