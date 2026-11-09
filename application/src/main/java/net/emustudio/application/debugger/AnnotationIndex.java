/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** User-metadata editor and persistence adapter over CPU-visible addresses. */
public final class AnnotationIndex {
    public static final long OWNER = Long.MIN_VALUE;
    public static final String USER_KIND = "emuStudio.user/";
    private static final AtomicLong IDS = new AtomicLong();
    private final int addressSpaceSize;
    private final MemoryAnnotations engine;

    public AnnotationIndex(int addressSpaceSize, MemoryAnnotations engine) {
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
        Entry added = new Entry(IDS.getAndAdd(2), entry.address, entry.end, entry.label, entry.kind);
        edit(List.of(added), false, false);
        return added;
    }

    /** Updates an existing annotation by identity. */
    public void update(Entry entry) {
        if (entry.id < 0) throw new IllegalArgumentException("Unknown annotation");
        edit(List.of(entry), false, true);
    }

    public void remove(Entry entry) {
        if (entry.id < 0) throw new IllegalArgumentException("Unknown annotation");
        engine.remove(OWNER, entry.id);

    }

    private static void validateKind(String kind) {
        if (!List.of("UNKNOWN", "CODE", "DATA").contains(kind))
            throw new IllegalArgumentException("Invalid annotation kind");
    }

    private static void validateRange(int size, Entry entry) {
        if (entry.address < 0 || entry.end <= entry.address || entry.end > size)
            throw new IllegalArgumentException("Invalid annotation range");
    }

    private void edit(List<Entry> entries, boolean clear, boolean requireExisting) {
        for (Entry entry : entries) {
            validateKind(entry.kind);
            validateRange(addressSpaceSize, entry);
        }
        while (true) {
            long revision = engine.revision();
            List<MemoryAnnotations.Entry> values = clear ? new ArrayList<>() : userEntries();
            for (Entry entry : entries) {
                if (requireExisting && values.stream().noneMatch(existing -> existing.entryId == entry.id)) {
                    if (engine.revision() != revision) break;
                    throw new IllegalArgumentException("Unknown annotation");
                }
                values.removeIf(existing -> existing.entryId == entry.id);
                values.add(new MemoryAnnotations.Entry(OWNER, entry.id, entry.address, entry.end,
                        USER_KIND + entry.kind, entry.label, null));
            }
            if (engine.compareAndReplaceOwner(revision, OWNER, values)) break;
        }

    }

    /** Replaces all user annotations, keeping the given identities. */
    void replace(List<Entry> entries) { edit(entries, true, false); }

    public List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (MemoryAnnotations.Entry entry : userEntries())
            if (entry.kind.startsWith(USER_KIND)) result.add(new Entry(entry.entryId, entry.start, entry.end,
                    entry.text, entry.kind.substring(USER_KIND.length())));
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
        for (Entry e : entries())
            if (e.label.toLowerCase(Locale.ROOT).contains(needle)) result.add(e);
        return result;
    }

    /** Only user annotations are saved; identities are runtime-only. */
    public Properties properties() {
        Properties p = new Properties();
        int i = 0;
        for (Entry e : entries()) {
            String k = "entry." + (i++) + ".";
            p.setProperty(k + "address", Integer.toString(e.address));
            p.setProperty(k + "end", Integer.toString(e.end));
            p.setProperty(k + "label", e.label);
            p.setProperty(k + "kind", e.kind);
        }
        return p;
    }

    public void save(Path path) throws IOException { AtomicProperties.save(path, properties()); }
    public int load(Path path) throws IOException { return importProperties(AtomicProperties.load(path)); }

    /** Appends saved annotations, skipping exact duplicates. Returns the number of invalid entries skipped. */
    public int importProperties(Properties p) {
        int size = addressSpaceSize;
        Set<List<Object>> known = new HashSet<>();
        for (Entry entry : entries()) known.add(key(entry));
        List<Entry> added = new ArrayList<>();
        int skipped = 0;
        for (int i : AtomicProperties.indices(p, "entry.", ".address")) {
            String k = "entry." + i + ".";
            Entry entry;
            try {
                entry = new Entry(IDS.getAndAdd(2), Integer.parseInt(p.getProperty(k + "address")),
                        Integer.parseInt(p.getProperty(k + "end")), p.getProperty(k + "label", ""),
                        p.getProperty(k + "kind", "UNKNOWN"));
                validateKind(entry.kind);
                validateRange(size, entry);
            } catch (RuntimeException invalid) {
                skipped++;
                continue;
            }
            if (known.add(key(entry))) added.add(entry);
        }
        edit(added, false, false);
        return skipped;
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
