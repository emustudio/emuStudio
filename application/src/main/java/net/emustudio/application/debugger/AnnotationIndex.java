/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** User-metadata editor and persistence adapter over CPU-visible addresses. */
public final class AnnotationIndex {
    private static final long OWNER = 1;
    private static final long TYPE_OWNER = 3;
    private static final String USER_KIND = "emuStudio.user/";
    private static final AtomicLong IDS = new AtomicLong();
    private final ProgramIndex index;
    private final Runnable changed;
    private volatile View cached;
    private final ThreadLocal<Lookup> lookups = ThreadLocal.withInitial(Lookup::new);

    public AnnotationIndex(ProgramIndex index) {
        this(index, () -> {});
    }

    AnnotationIndex(ProgramIndex index, Runnable changed) {
        this.index = Objects.requireNonNull(index);
        this.changed = Objects.requireNonNull(changed);
    }

    public ProgramIndex programIndex() { return index; }

    public Entry get(int address) {
        Lookup lookup = lookup(index.snapshot(), address);
        return lookup.entry;
    }

    private Lookup lookup(ProgramIndex.Snapshot snapshot, int address) {
        Lookup lookup = lookups.get();
        lookup.view = view(snapshot);
        lookup.entry = null;
        lookup.marker = null;
        snapshot.forEachAnnotationStartingAt(address, lookup);
        return lookup;
    }

    public void put(Entry entry) {
        Objects.requireNonNull(entry);
        validateKind(entry.kind);
        edit(List.of(entry), false);
    }

    /** Adds an independent annotation even when another starts at the same address. */
    public Entry add(Entry entry) {
        Entry added = new Entry(IDS.getAndAdd(2), entry.address, entry.end, entry.label, entry.comment, entry.kind);
        edit(List.of(added), false);
        return added;
    }

    /** Updates an existing annotation by identity. */
    public void update(Entry entry) {
        if (entry.id < 0 || entries().stream().noneMatch(existing -> existing.id == entry.id))
            throw new IllegalArgumentException("Unknown annotation");
        edit(List.of(entry), false);
    }

    public void remove(Entry entry) {
        if (entry.id < 0) throw new IllegalArgumentException("Unknown annotation");
        index.editContent((base, update) -> {
            removeEntry(update, entry.id);
            return true;
        });
        changed.run();
    }

    private static void removeEntry(ProgramIndex.ContentUpdate update, long id) {
        update.removeAnnotation(OWNER, id).removeAnnotation(OWNER, id + 1).removeAnnotation(TYPE_OWNER, id);
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
        // Replay a logical editor operation against fresh content, never a derived analysis result.
        index.editContent((base, update) -> {
            stage(update, base, entries, clear);
            return true;
        });
        changed.run();
    }

    /** Disk-loaded metadata commits only against the root from before the I/O. */
    ProgramIndex.CommitResult installProperties(Properties properties, ProgramIndex.Snapshot base) throws IOException {
        List<Entry> entries = importedEntries(properties);
        try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
            stage(update, base, entries, false);
            return update.commit();
        }
    }

    private void stage(ProgramIndex.ContentUpdate update, ProgramIndex.Snapshot base,
                       List<Entry> entries, boolean clear) {
        if (clear) update.removeAnnotations(OWNER).removeAnnotations(TYPE_OWNER);
        for (Entry entry : entries) {
            validateKind(entry.kind);
            validateRange(base.layout(), entry);
            ProgramIndex.Annotation old = lookup(base, entry.address).marker;
            long id = entry.id >= 0 ? entry.id : old == null ? IDS.getAndAdd(2) : old.entryId();
            ProgramIndex.Range range = new ProgramIndex.Range(entry.address, entry.end);
            String kind = USER_KIND + entry.kind;
            update.putAnnotation(OWNER, id, range, kind, entry.comment);
            update.removeAnnotation(OWNER, id + 1);
            if (!entry.label.isEmpty())
                update.putAnnotation(OWNER, id + 1, range, ProgramIndex.Kinds.LABEL, entry.label);
            // CODE/DATA are separate typed annotations so flow consumers need no editor knowledge.
            update.removeAnnotation(TYPE_OWNER, id);
            if (!entry.kind.equals("UNKNOWN"))
                update.putAnnotation(TYPE_OWNER, id, range,
                        entry.kind.equals("CODE") ? ProgramIndex.Kinds.CODE : ProgramIndex.Kinds.DATA, "");
        }
    }

    public void remove(int address) {
        boolean removed = index.editContent((base, update) -> {
            ProgramIndex.Annotation old = lookup(base, address).marker;
            if (old == null) return false;
            removeEntry(update, old.entryId());
            return true;
        });
        if (removed) changed.run();
    }

    void clear() { edit(List.of(), true); }

    public List<Entry> entries() { return view(index.snapshot()).entries; }

    public List<Entry> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries())
            if ((e.label + " " + e.comment).toLowerCase(Locale.ROOT).contains(needle)) result.add(e);
        return result;
    }

    private View view(ProgramIndex.Snapshot snapshot) {
        View result = cached;
        if (result == null || result.revision != snapshot.contentRevision()) {
            result = new View(snapshot);
            cached = result;
        }
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
            p.setProperty(k + "comment", e.comment);
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
                    entry.label, entry.comment, entry.kind));
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
                        p.getProperty(k + "comment", ""),
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

    private static final class Lookup implements Consumer<ProgramIndex.Annotation> {
        View view;
        Entry entry;
        ProgramIndex.Annotation marker;

        @Override public void accept(ProgramIndex.Annotation annotation) {
            Entry candidate = view.payloads.get(annotation);
            if (candidate != null) { entry = candidate; marker = annotation; }
        }
    }

    /** Derived editor projection only; ProgramIndex remains the authoritative storage. */
    private static final class View {
        final long revision;
        final List<Entry> entries;
        final IdentityHashMap<ProgramIndex.Annotation, Entry> payloads = new IdentityHashMap<>();

        View(ProgramIndex.Snapshot snapshot) {
            revision = snapshot.contentRevision();
            Map<Long, String> labels = new HashMap<>();
            snapshot.forEachAnnotation(a -> {
                if (a.ownerId() == OWNER && a.kind().equals(ProgramIndex.Kinds.LABEL)) labels.put(a.entryId(), a.text());
            });
            List<Entry> result = new ArrayList<>();
            snapshot.forEachAnnotation(a -> {
                if (a.ownerId() != OWNER || !a.kind().startsWith(USER_KIND)) return;
                String kind = a.kind().substring(USER_KIND.length());
                Entry entry = new Entry(a.entryId(), a.range().start, a.range().end,
                        labels.getOrDefault(a.entryId() + 1, ""), a.text(), kind);
                result.add(entry);
                payloads.put(a, entry);
            });
            entries = List.copyOf(result);
        }
    }

    public static final class Entry {
        public final long id;
        public final int address, end;
        public final String label, comment, kind;
        public Entry(int address, String label, String comment, String kind) {
            this(-1, address, address + 1, label, comment, kind);
        }
        public Entry(long id, int address, int end, String label, String comment, String kind) {
            this.id = id;
            this.address = address;
            this.end = end;
            this.label = Objects.requireNonNull(label);
            this.comment = Objects.requireNonNull(comment);
            this.kind = Objects.requireNonNull(kind);
        }

        @Override public String toString() { return String.format("%04X %s %s", address, label, comment); }
    }
}
