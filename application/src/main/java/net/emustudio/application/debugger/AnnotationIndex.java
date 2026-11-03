/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Legacy user-metadata editor and persistence adapter over the session's program index. */
public final class AnnotationIndex {
    private static final long OWNER = 1;
    private static final long TYPE_OWNER = 3;
    private static final String USER_KIND = "emuStudio.user/";
    private static final AtomicLong IDS = new AtomicLong();
    private final MemoryAnalysis image;
    private final ProgramIndex index;
    private volatile View cached;
    private final ThreadLocal<Lookup> lookups = ThreadLocal.withInitial(Lookup::new);

    public AnnotationIndex(MemoryAnalysis image) {
        this(image, ProgramIndex.create(image.identity(), image.size(), image.bankCount(), image.common));
    }

    AnnotationIndex(MemoryAnalysis image, ProgramIndex index) {
        this.image = Objects.requireNonNull(image);
        this.index = Objects.requireNonNull(index);
        ProgramIndex.Layout layout = index.snapshot().layout();
        if (layout.addressSpaceSize() != image.size() || layout.bankCount() != image.bankCount()
                || layout.commonBoundary() != image.common)
            throw new IllegalArgumentException("Memory layout differs from program index");
    }

    public ProgramIndex programIndex() { return index; }

    public Entry get(int bank, int address) {
        Lookup lookup = lookup(index.snapshot(), bank, address);
        return lookup.entry;
    }

    private Lookup lookup(ProgramIndex.Snapshot snapshot, int bank, int address) {
        Lookup lookup = lookups.get();
        lookup.view = view(snapshot);
        lookup.entry = null;
        lookup.marker = null;
        snapshot.forEachAnnotationStartingAt(bank, address, lookup);
        return lookup;
    }

    public void put(Entry entry) {
        Objects.requireNonNull(entry);
        validateKind(entry.kind);
        edit(List.of(entry), false);
    }

    private static void validateKind(String kind) {
        if (!List.of("UNKNOWN", "CODE", "DATA").contains(kind))
            throw new IllegalArgumentException("Invalid annotation kind");
    }

    private void edit(List<Entry> entries, boolean clear) {
        // Replay a logical editor operation against fresh content, never a derived analysis result.
        for (;;) {
            ProgramIndex.Snapshot base = index.snapshot();
            try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
                stage(update, base, entries, clear);
                if (update.commit() == ProgramIndex.CommitResult.PUBLISHED) return;
            }
        }
    }

    /** Captured image results commit once against their exact base; unlike editor commands, never replay. */
    ProgramIndex.CommitResult installCaptured(AnnotationIndex captured, ProgramIndex.Snapshot base) {
        if (!image.identity().equals(captured.image.identity()))
            throw new IllegalArgumentException("Annotations belong to another memory image/layout");
        List<Entry> entries = captured.entries();
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
            int bank = base.layout().physicalBank(entry.bank, entry.address);
            ProgramIndex.Annotation old = lookup(base, bank, entry.address).marker;
            long id = old == null ? IDS.getAndAdd(2) : old.entryId();
            ProgramIndex.Range range = new ProgramIndex.Range(bank, entry.address, entry.address + 1);
            String kind = USER_KIND + entry.kind + (entry.bookmark ? "/bookmark" : "");
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

    public void remove(int bank, int address) {
        for (;;) {
            ProgramIndex.Snapshot base = index.snapshot();
            ProgramIndex.Annotation old = lookup(base, bank, address).marker;
            if (old == null) return;
            try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
                update.removeAnnotation(OWNER, old.entryId()).removeAnnotation(OWNER, old.entryId() + 1);
                update.removeAnnotation(TYPE_OWNER, old.entryId());
                if (update.commit() == ProgramIndex.CommitResult.PUBLISHED) return;
            }
        }
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
        p.setProperty("version", "1");
        p.setProperty("image.identity", image.identity());
        List<Entry> entries = entries();
        p.setProperty("entries", Integer.toString(entries.size()));
        int i = 0;
        for (Entry e : entries) {
            String k = "entry." + (i++) + ".";
            p.setProperty(k + "bank", Integer.toString(e.bank));
            p.setProperty(k + "address", Integer.toString(e.address));
            p.setProperty(k + "label", e.label);
            p.setProperty(k + "comment", e.comment);
            p.setProperty(k + "bookmark", Boolean.toString(e.bookmark));
            p.setProperty(k + "kind", e.kind);
        }
        return p;
    }

    public void save(Path path) throws IOException { AtomicProperties.save(path, properties()); }
    public void load(Path path) throws IOException { importProperties(AtomicProperties.load(path)); }

    public void importProperties(Properties p) throws IOException {
        if (!"1".equals(p.getProperty("version")) || !image.identity().equals(p.getProperty("image.identity")))
            throw new IOException("Annotations belong to another memory image/layout");
        Map<Long, Entry> imported = new LinkedHashMap<>();
        try {
            int count = Integer.parseInt(p.getProperty("entries"));
            if (count < 0 || count > 100000) throw new IllegalArgumentException("Too many annotations");
            for (int i = 0; i < count; i++) {
                String k = "entry." + i + ".";
                Entry entry = new Entry(Integer.parseInt(p.getProperty(k + "bank")),
                        Integer.parseInt(p.getProperty(k + "address")), p.getProperty(k + "label", ""),
                        p.getProperty(k + "comment", ""), Boolean.parseBoolean(p.getProperty(k + "bookmark")),
                        p.getProperty(k + "kind", "UNKNOWN"));
                validateKind(entry.kind);
                int bank = index.snapshot().layout().physicalBank(entry.bank, entry.address);
                imported.put(((long) bank << 32) | (entry.address & 0xffffffffL), entry);
            }
            edit(new ArrayList<>(imported.values()), false);
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
                String value = a.kind().substring(USER_KIND.length());
                boolean bookmark = value.endsWith("/bookmark");
                String kind = bookmark ? value.substring(0, value.length() - "/bookmark".length()) : value;
                Entry entry = new Entry(a.range().bank, a.range().start,
                        labels.getOrDefault(a.entryId() + 1, ""), a.text(), bookmark, kind);
                result.add(entry);
                payloads.put(a, entry);
            });
            entries = List.copyOf(result);
        }
    }

    public static final class Entry {
        public final int bank, address;
        public final String label, comment, kind;
        public final boolean bookmark;

        public Entry(int bank, int address, String label, String comment, boolean bookmark, String kind) {
            this.bank = bank;
            this.address = address;
            this.label = Objects.requireNonNull(label);
            this.comment = Objects.requireNonNull(comment);
            this.bookmark = bookmark;
            this.kind = Objects.requireNonNull(kind);
        }

        @Override public String toString() { return String.format("bank %d:%04X %s %s", bank, address, label, comment); }
    }
}
