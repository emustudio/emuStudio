/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Address cells contain IDs into an immutable pool of primitive posting lists. */
final class AddressPostings<T> {
    private final AddressTable addresses;
    private final int[][] lists;
    private final T[] values;

    AddressPostings(ProgramIndex.Layout layout, T[] values, Map<Long, List<Integer>> entries) {
        this.values = values;
        List<int[]> pool = new ArrayList<>();
        Map<List<Integer>, Integer> interned = new HashMap<>();
        Map<Long, Integer> ids = new TreeMap<>();
        for (Map.Entry<Long, List<Integer>> entry : entries.entrySet()) {
            List<Integer> posting = entry.getValue();
            Integer id = interned.get(posting);
            if (id == null) {
                int[] list = new int[posting.size()];
                for (int i = 0; i < list.length; i++) list[i] = posting.get(i);
                pool.add(list);
                id = pool.size();
                interned.put(posting, id);
            }
            ids.put(entry.getKey(), id);
        }
        lists = pool.toArray(new int[0][]);
        addresses = new AddressTable(layout, ids);
    }

    static void add(Map<Long, List<Integer>> postings, long key, int id) {
        postings.computeIfAbsent(key, ignored -> new ArrayList<>()).add(id);
    }

    void visit(int bank, int address, Consumer<? super T> visitor) {
        int id = addresses.get(bank, address);
        if (id != 0) for (int entry : lists[id - 1]) visitor.accept(values[entry]);
    }
}
