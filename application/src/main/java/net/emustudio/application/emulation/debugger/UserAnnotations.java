/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** User-metadata editor over CPU-visible addresses. */
public final class UserAnnotations {
    public static final long OWNER = Long.MIN_VALUE;
    private static final AtomicLong IDS = new AtomicLong();
    private final int addressSpaceSize;
    private final MemoryAnnotations engine;

    private enum Edit { APPEND, UPDATE, REPLACE }

    public UserAnnotations(int addressSpaceSize, MemoryAnnotations engine) {
        if (addressSpaceSize < 1) throw new IllegalArgumentException("Invalid memory layout");
        this.addressSpaceSize = addressSpaceSize;
        this.engine = Objects.requireNonNull(engine);
    }

    public int addressSpaceSize() { return addressSpaceSize; }

    public Entry get(int address) {
        Entry result = null;
        for (Entry entry : entries()) if (entry.address == address) result = entry;
        return result;
    }

    /** Adds an independent annotation even when another starts at the same address. */
    public Entry add(Entry entry) {
        Entry added = withFreshIdentity(entry);
        edit(List.of(added), Edit.APPEND);
        return added;
    }

    /** Updates an existing annotation by identity. */
    public void update(Entry entry) {
        if (entry.id < 0) throw new IllegalArgumentException("Unknown annotation");
        edit(List.of(entry), Edit.UPDATE);
    }

    public void remove(Entry entry) {
        if (entry.id < 0) throw new IllegalArgumentException("Unknown annotation");
        engine.remove(OWNER, entry.id);
    }

    private static void validateKind(String kind) {
        if (!List.of("CODE", "DATA").contains(kind))
            throw new IllegalArgumentException("Invalid annotation kind");
    }

    private static void validateRange(int size, Entry entry) {
        if (entry.address < 0 || entry.end <= entry.address || entry.end > size)
            throw new IllegalArgumentException("Invalid annotation range");
    }

    private void edit(List<Entry> entries, Edit operation) {
        entries.forEach(this::validate);
        while (true) {
            long revision = engine.revision();
            List<MemoryAnnotations.Entry> values = operation == Edit.REPLACE ? new ArrayList<>() : userEntries();
            for (Entry entry : entries) {
                if (operation == Edit.UPDATE && values.stream().noneMatch(existing -> existing.entryId == entry.id)) {
                    if (engine.revision() != revision) break;
                    throw new IllegalArgumentException("Unknown annotation");
                }
                values.removeIf(existing -> existing.entryId == entry.id);
                values.add(new MemoryAnnotations.Entry(OWNER, entry.id, entry.address, entry.end,
                        entry.kind, entry.label, null));
            }
            if (engine.compareAndReplaceOwner(revision, OWNER, values)) break;
        }
    }

    /** Replaces all user annotations, keeping the given identities. */
    public void replace(List<Entry> entries) { edit(entries, Edit.REPLACE); }

    public List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (MemoryAnnotations.Entry entry : userEntries())
            result.add(new Entry(entry.entryId, entry.start, entry.end, entry.text, entry.kind));
        return List.copyOf(result);
    }

    private List<MemoryAnnotations.Entry> userEntries() {
        List<MemoryAnnotations.Entry> result = new ArrayList<>();
        for (MemoryAnnotations.Entry entry : engine.all()) if (entry.ownerId == OWNER) result.add(entry);
        return result;
    }

    public List<Entry> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries())
            if (entry.label.toLowerCase(Locale.ROOT).contains(needle)) result.add(entry);
        return result;
    }

    /** Appends valid annotations with fresh identities, skipping duplicates. Returns the number of invalid entries. */
    public int merge(List<Entry> entries) {
        Set<List<Object>> known = new HashSet<>();
        for (Entry entry : entries()) known.add(key(entry));
        List<Entry> added = new ArrayList<>();
        int skipped = 0;
        for (Entry entry : entries) {
            try {
                validate(entry);
            } catch (RuntimeException invalid) {
                skipped++;
                continue;
            }
            if (known.add(key(entry)))
                added.add(withFreshIdentity(entry));
        }
        edit(added, Edit.APPEND);
        return skipped;
    }

    private void validate(Entry entry) {
        validateKind(entry.kind);
        validateRange(addressSpaceSize, entry);
    }

    private static Entry withFreshIdentity(Entry entry) {
        return new Entry(IDS.getAndAdd(2), entry.address, entry.end, entry.label, entry.kind);
    }

    private static List<Object> key(Entry entry) {
        return List.of(entry.address, entry.end, entry.kind, entry.label);
    }

    public static final class Entry {
        public final long id;
        public final int address, end;
        public final String label, kind;
        public Entry(int address, String label, String kind) {
            this(-1, address, address + 1, label, kind);
        }
        public Entry(long id, int address, int end, String label, String kind) {
            this.id = id;
            this.address = address;
            this.end = end;
            this.label = Objects.requireNonNull(label);
            this.kind = Objects.requireNonNull(kind);
        }

        @Override public String toString() { return String.format("%04X %s", address, label); }
    }
}
