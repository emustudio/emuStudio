/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** User metadata kept separate from compiler annotations and bound to a captured image. */
public final class AnnotationIndex {
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final MemoryAnalysis image;

    public AnnotationIndex(MemoryAnalysis image) {
        this.image = image;
    }

    private String key(int bank, int address) {
        return image.physicalBank(address, bank) + ":" + address;
    }

    public Entry get(int bank, int address) {
        return entries.get(key(bank, address));
    }

    public void put(Entry entry) {
        int bank = image.physicalBank(entry.address, entry.bank);
        if (!List.of("UNKNOWN", "CODE", "DATA").contains(entry.kind))
            throw new IllegalArgumentException("Invalid annotation kind");
        entries.put(
                key(bank, entry.address),
                new Entry(
                        bank,
                        entry.address,
                        entry.label,
                        entry.comment,
                        entry.bookmark,
                        entry.kind));
    }

    public void remove(int bank, int address) {
        entries.remove(key(bank, address));
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public List<Entry> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries.values())
            if ((e.label + " " + e.comment).toLowerCase(Locale.ROOT).contains(needle))
                result.add(e);
        return result;
    }

    public Properties properties() {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("image.identity", image.identity());
        p.setProperty("entries", Integer.toString(entries.size()));
        int i = 0;
        for (Entry e : entries.values()) {
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

    public void save(Path path) throws IOException {
        AtomicProperties.save(path, properties());
    }

    public void load(Path path) throws IOException {
        importProperties(AtomicProperties.load(path));
    }

    public void importProperties(Properties p) throws IOException {
        if (!"1".equals(p.getProperty("version"))
                || !image.identity().equals(p.getProperty("image.identity")))
            throw new IOException("Annotations belong to another memory image/layout");
        AnnotationIndex imported = new AnnotationIndex(image);
        try {
            int count = Integer.parseInt(p.getProperty("entries"));
            if (count < 0 || count > 100000)
                throw new IllegalArgumentException("Too many annotations");
            for (int i = 0; i < count; i++) {
                String k = "entry." + i + ".";
                imported.put(
                        new Entry(
                                Integer.parseInt(p.getProperty(k + "bank")),
                                Integer.parseInt(p.getProperty(k + "address")),
                                p.getProperty(k + "label", ""),
                                p.getProperty(k + "comment", ""),
                                Boolean.parseBoolean(p.getProperty(k + "bookmark")),
                                p.getProperty(k + "kind", "UNKNOWN")));
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid annotation file", e);
        }
        entries.putAll(imported.entries);
    }

    public static final class Entry {
        public final int bank, address;
        public final String label, comment, kind;
        public final boolean bookmark;

        public Entry(
                int bank,
                int address,
                String label,
                String comment,
                boolean bookmark,
                String kind) {
            this.bank = bank;
            this.address = address;
            this.label = Objects.requireNonNull(label);
            this.comment = Objects.requireNonNull(comment);
            this.bookmark = bookmark;
            this.kind = Objects.requireNonNull(kind);
        }

        @Override
        public String toString() {
            return String.format("bank %d:%04X %s %s", bank, address, label, comment);
        }
    }
}
