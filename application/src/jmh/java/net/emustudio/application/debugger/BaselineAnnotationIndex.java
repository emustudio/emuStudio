/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;

// Benchmark-only string-key comparison model, using CPU-visible addresses and layout metadata.

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** User metadata kept separate from compiler annotations and bound to computer/layout metadata. */
public final class BaselineAnnotationIndex {
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final ProgramIndex.Layout layout;

    public BaselineAnnotationIndex(ProgramIndex.Layout layout) {
        this.layout = layout;
    }

    private String key(int address) {
        if (address < 0 || address >= layout.addressSpaceSize()) throw new IllegalArgumentException("Address outside memory");
        return Integer.toString(address);
    }

    public Entry get(int address) {
        return entries.get(key(address));
    }

    public void put(Entry entry) {
        if (!List.of("UNKNOWN", "CODE", "DATA").contains(entry.kind))
            throw new IllegalArgumentException("Invalid annotation kind");
        entries.put(
                key(entry.address),
                new Entry(
                        entry.address,
                        entry.label,
                        entry.bookmark,
                        entry.kind));
    }

    public void remove(int address) {
        entries.remove(key(address));
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public List<Entry> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries.values())
            if (e.label.toLowerCase(Locale.ROOT).contains(needle))
                result.add(e);
        return result;
    }

    public Properties properties() {
        Properties p = new Properties();
        p.setProperty("version", "3");
        p.setProperty("image.identity", layout.imageId());
        p.setProperty("entries", Integer.toString(entries.size()));
        int i = 0;
        for (Entry e : entries.values()) {
            String k = "entry." + i + ".";
            p.setProperty(k + "id", Long.toString(2L * i++));
            p.setProperty(k + "address", Integer.toString(e.address));
            p.setProperty(k + "end", Integer.toString(e.address + 1));
            p.setProperty(k + "label", e.label);
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
        if (!"3".equals(p.getProperty("version"))
                || !layout.imageId().equals(p.getProperty("image.identity")))
            throw new IOException("Annotations belong to another memory image/layout");
        BaselineAnnotationIndex imported = new BaselineAnnotationIndex(layout);
        try {
            int count = Integer.parseInt(p.getProperty("entries"));
            if (count < 0 || count > 100000)
                throw new IllegalArgumentException("Too many annotations");
            for (int i = 0; i < count; i++) {
                String k = "entry." + i + ".";
                imported.put(
                        new Entry(
                                Integer.parseInt(p.getProperty(k + "address")),
                                p.getProperty(k + "label", ""),
                                Boolean.parseBoolean(p.getProperty(k + "bookmark")),
                                p.getProperty(k + "kind", "UNKNOWN")));
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid annotation file", e);
        }
        entries.putAll(imported.entries);
    }

    public static final class Entry {
        public final int address;
        public final String label, kind;
        public final boolean bookmark;

        public Entry(
                int address,
                String label,
                boolean bookmark,
                String kind) {
            this.address = address;
            this.label = Objects.requireNonNull(label);
            this.bookmark = bookmark;
            this.kind = Objects.requireNonNull(kind);
        }

        @Override
        public String toString() {
            return String.format("%04X %s", address, label);
        }
    }
}
