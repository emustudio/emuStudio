/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
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
    private final ProgramIndex index;
    private final MemoryAnnotations engine;

    public AnnotationIndex(ProgramIndex index) {
        this(index, index.annotationEngine());
    }

    public AnnotationIndex(ProgramIndex index, MemoryAnnotations engine) {
        this.index = Objects.requireNonNull(index);
        this.engine = Objects.requireNonNull(engine);
    }

    public ProgramIndex programIndex() { return index; }

    public Entry get(int address) {
        Entry result = null;
        for (Entry entry : entries()) if (entry.address == address) result = entry;
        return result;
    }

    public void put(Entry entry) {
        Objects.requireNonNull(entry);
        validateKind(entry.kind);
        edit(List.of(entry), false);
    }

    /** Adds an independent annotation even when another starts at the same address. */
    public Entry add(Entry entry) {
        Entry added = new Entry(IDS.getAndAdd(2), entry.address, entry.end, entry.label, entry.kind);
        edit(List.of(added), false);
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

    private static void validateRange(ProgramIndex.Layout layout, Entry entry) {
        if (entry.address < 0 || entry.end <= entry.address || entry.end > layout.addressSpaceSize())
            throw new IllegalArgumentException("Invalid annotation range");
    }

    private void edit(List<Entry> entries, boolean clear) {
        edit(entries, clear, false);
    }

    private void edit(List<Entry> entries, boolean clear, boolean requireExisting) {
        for (Entry entry : entries) {
            validateKind(entry.kind);
            validateRange(index.snapshot().layout(), entry);
        }
        while (true) {
            long revision = engine.revision();
            List<MemoryAnnotations.Entry> values = clear ? new ArrayList<>() : userEntries();
            for (Entry entry : entries) {
                if (requireExisting && values.stream().noneMatch(existing -> existing.entryId == entry.id)) {
                    if (engine.revision() != revision) break;
                    throw new IllegalArgumentException("Unknown annotation");
                }
                long id = entry.id;
                if (id < 0) {
                    id = IDS.getAndAdd(2);
                    for (MemoryAnnotations.Entry existing : values)
                        if (existing.start == entry.address) id = existing.entryId;
                }
                final long entryId = id;
                values.removeIf(existing -> existing.entryId == entryId);
                values.add(new MemoryAnnotations.Entry(OWNER, id, entry.address, entry.end,
                        USER_KIND + entry.kind, entry.label, null));
            }
            if (engine.compareAndReplaceOwner(revision, OWNER, values)) break;
        }

    }

    /** Disk-loaded metadata commits only against the root from before the I/O. */
    ProgramIndex.CommitResult installProperties(Properties properties, ProgramIndex.Snapshot base) throws IOException {
        return installProperties(properties, base, engine.revision());
    }

    ProgramIndex.CommitResult installProperties(Properties properties, ProgramIndex.Snapshot base,
                                                  long expectedRevision) throws IOException {
        List<Entry> entries = importedEntries(properties);
        if (!index.snapshot().sameRevisions(base)) return ProgramIndex.CommitResult.STALE;
        List<MemoryAnnotations.Entry> values = userEntries();
        for (Entry entry : entries) {
            values.removeIf(existing -> existing.entryId == entry.id);
            values.add(new MemoryAnnotations.Entry(OWNER, entry.id, entry.address, entry.end,
                    USER_KIND + entry.kind, entry.label, null));
        }
        return engine.compareAndReplaceOwner(expectedRevision, OWNER, values)
                ? ProgramIndex.CommitResult.PUBLISHED : ProgramIndex.CommitResult.STALE;
    }

    public void remove(int address) {
        while (true) {
            long revision = engine.revision();
            List<MemoryAnnotations.Entry> values = userEntries();
            MemoryAnnotations.Entry old = null;
            for (MemoryAnnotations.Entry entry : values) if (entry.start == address) old = entry;
            if (old == null) return;
            values.remove(old);
            if (engine.compareAndReplaceOwner(revision, OWNER, values)) {

                return;
            }
        }
    }

    void clear() { edit(List.of(), true); }

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

    public Properties properties() {
        Properties p = new Properties();
        p.setProperty("version", "3");
        p.setProperty("image.identity", index.snapshot().layout().imageId());
        List<Entry> entries = entries();
        p.setProperty("entries", Integer.toString(entries.size()));
        int i = 0;
        for (Entry e : entries) {
            String k = "entry." + (i++) + ".";
            p.setProperty(k + "address", Integer.toString(e.address));
            p.setProperty(k + "end", Integer.toString(e.end));
            p.setProperty(k + "id", Long.toString(e.id));
            p.setProperty(k + "label", e.label);
            p.setProperty(k + "kind", e.kind);
        }
        return p;
    }

    public void save(Path path) throws IOException { AtomicProperties.save(path, properties()); }
    public void load(Path path) throws IOException { importProperties(AtomicProperties.load(path)); }

    public void importProperties(Properties p) throws IOException {
        List<Entry> imported = importedEntries(p);
        List<Entry> copies = new ArrayList<>();
        for (Entry entry : imported)
            copies.add(new Entry(IDS.getAndAdd(2), entry.address, entry.end,
                    entry.label, entry.kind));
        edit(copies, false);
    }

    private List<Entry> importedEntries(Properties p) throws IOException {
        String version = p.getProperty("version");
        if (!"3".equals(version)
                || !index.snapshot().layout().imageId().equals(p.getProperty("image.identity")))
            throw new IOException("Annotations belong to another memory image/layout");
        Map<Long, Entry> imported = new LinkedHashMap<>();
        try {
            int count = Integer.parseInt(p.getProperty("entries"));
            if (count < 0 || count > 100000) throw new IllegalArgumentException("Too many annotations");
            for (int i = 0; i < count; i++) {
                String k = "entry." + i + ".";
                int address = Integer.parseInt(p.getProperty(k + "address"));
                long id = Long.parseLong(p.getProperty(k + "id"));
                if (id < 0 || id >= Long.MAX_VALUE - 1 || (id & 1) != 0) throw new IllegalArgumentException("Invalid annotation id");
                Entry entry = new Entry(id, address, Integer.parseInt(p.getProperty(k + "end")), p.getProperty(k + "label", ""),
                        p.getProperty(k + "kind", "UNKNOWN"));
                validateKind(entry.kind);
                validateRange(index.snapshot().layout(), entry);
                if (imported.containsKey(id)) throw new IllegalArgumentException("Duplicate annotation id");
                imported.put(id, entry);
            }
            for (long id : imported.keySet()) IDS.accumulateAndGet(id + 2, Math::max);
            return new ArrayList<>(imported.values());
        } catch (RuntimeException e) { throw new IOException("Invalid annotation file", e); }
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
