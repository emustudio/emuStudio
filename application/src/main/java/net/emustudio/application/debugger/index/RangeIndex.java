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

/** A logical range has one payload, even when its physical postings cross common memory. */
final class RangeIndex<T> {
    private final T[] values;
    private final int[] banks;
    private final IntervalTable[] intervals;
    private final AddressPostings<T> starts;

    RangeIndex(ProgramIndex.Layout layout, T[] values, Function<? super T, ProgramIndex.Range> range) {
        this.values = values;
        Map<Long, List<Integer>> beginning = new TreeMap<>();
        Map<Integer, List<IntervalTable.Entry>> partitioned = new TreeMap<>();
        for (int i = 0; i < values.length; i++) {
            int id = i;
            ProgramIndex.Range r = range.apply(values[id]);
            AddressPostings.add(beginning, AddressTable.key(layout.physicalBank(r.bank, r.start), r.start), id);
            AddressTable.split(layout, r, (bank, start, end) -> partitioned
                    .computeIfAbsent(bank, ignored -> new ArrayList<>()).add(new IntervalTable.Entry(start, end, id)));
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

    void startingAt(int physicalBank, int address, Consumer<? super T> visitor) {
        starts.visit(physicalBank, address, visitor);
    }

    void covering(int physicalBank, int address, Consumer<? super T> visitor) {
        int at = Arrays.binarySearch(banks, physicalBank);
        if (at >= 0) intervals[at].query(address, address + 1, values, visitor);
    }
}
