/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Rule storage includes disabled entries; only enabled entries populate bitmap and postings. */
final class BreakpointIndex {
    final ProgramIndex.Breakpoint[] values;
    private final AddressTable words;
    private final long[] bits;
    private final AddressPostings<ProgramIndex.Breakpoint> postings;

    BreakpointIndex(ProgramIndex.Layout layout, ProgramIndex.Breakpoint[] values) {
        this.values = values;
        Map<Integer, List<Integer>> entries = new TreeMap<>();
        Map<Integer, Long> bitmap = new TreeMap<>();
        for (int i = 0; i < values.length; i++) {
            ProgramIndex.Breakpoint b = values[i];
            if (!b.enabled()) continue;
            AddressPostings.add(entries, b.address(), i);
            int key = b.address() >>> 6;
            bitmap.merge(key, 1L << (b.address() & 63), (a, c) -> a | c);
        }
        bits = new long[bitmap.size()];
        Map<Integer, Integer> ids = new TreeMap<>();
        int at = 0;
        for (Map.Entry<Integer, Long> entry : bitmap.entrySet()) {
            bits[at] = entry.getValue();
            ids.put(entry.getKey(), ++at);
        }
        words = AddressTable.bitmapWords(layout, ids);
        postings = new AddressPostings<>(layout, values, entries);
    }

    boolean has(int address) {
        int word = words.get(address >>> 6);
        return word != 0 && (bits[word - 1] & (1L << (address & 63))) != 0;
    }

    void visit(int address, Consumer<? super ProgramIndex.Breakpoint> visitor) {
        postings.visit(address, visitor);
    }
}
