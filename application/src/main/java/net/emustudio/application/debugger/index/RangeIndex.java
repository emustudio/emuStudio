/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

/** Range queries over the CPU-visible address space. */
final class RangeIndex<T> {
    private final T[] values;
    private final IntervalTable intervals;
    private final AddressPostings<T> starts;

    RangeIndex(ProgramIndex.Layout layout, T[] values, Function<? super T, ProgramIndex.Range> range) {
        this.values = values;
        Map<Integer, List<Integer>> beginning = new TreeMap<>();
        List<IntervalTable.Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int id = i;
            ProgramIndex.Range r = range.apply(values[id]);
            AddressPostings.add(beginning, r.start, id);
            entries.add(new IntervalTable.Entry(r.start, r.end, id));
        }
        starts = new AddressPostings<>(layout, values, beginning);
        intervals = new IntervalTable(entries);
    }

    void startingAt(int address, Consumer<? super T> visitor) {
        starts.visit(address, visitor);
    }

    void covering(int address, Consumer<? super T> visitor) {
        intervals.query(address, address + 1, values, visitor);
    }
}
