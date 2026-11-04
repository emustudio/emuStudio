/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** Source navigation captured after a successful compilation, using CPU-visible addresses. */
public final class SourceAddressMap {
    // One compiler capture belongs to the application, not to a plugin-owned annotation store.
    private static final long MAPPING_OWNER = 2;
    private final MemoryContext<?> memory;
    private final MemoryContextAnnotations annotations;
    private final ProgramIndex index;
    private final ProgramIndex.Layout layout;
    private final long revision;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<Path, Source> sources = new LinkedHashMap<>();
    private final List<Declaration> declarations = new ArrayList<>();
    private final List<String> diagnostics = new ArrayList<>();

    private SourceAddressMap(MemoryContext<?> memory, ProgramIndex index, ProgramIndex.Snapshot base) {
        this.memory = memory;
        this.index = index;
        annotations = memory == null ? null : memory.annotations();
        layout = base == null ? null : base.layout();
        revision = base == null ? 0 : Math.incrementExact(base.contentRevision());
    }

    public static SourceAddressMap empty() { return new SourceAddressMap(null, null, null); }

    public ProgramIndex programIndex() { return index; }

    /** Unsupported compiler positions are excluded from bindings, with their reason retained here. */
    public List<String> diagnostics() { return List.copyOf(diagnostics); }

    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, SourceInputs inputs,
                                           ProgramIndex index) throws IOException {
        SourceCodePosition.ColumnUnit unit = compiler == null ? null : compiler.getSourcePositionColumnUnit();
        return capture(memory, inputs, index, unit == null ? SourceCodePosition.ColumnUnit.UNKNOWN : unit);
    }

    /** Publish source metadata into the session's index; user annotations and other owners survive. */
    static SourceAddressMap capture(MemoryContext<?> memory, SourceInputs inputs, ProgramIndex index,
                                    SourceCodePosition.ColumnUnit columnUnit) throws IOException {
        Objects.requireNonNull(columnUnit);
        if (memory != null && index == null)
            throw new IllegalArgumentException("Source capture with memory requires its program index");
        ProgramIndex.Snapshot base = index == null ? null : index.snapshot();
        SourceAddressMap result = new SourceAddressMap(memory, index, base);
        for (SourceText input : inputs.files().values()) {
            // Stamp before reading, so a write racing this check always changes the stamp.
            FileStamp stamp = FileStamp.of(input.path);
            if (!Files.readString(input.path).equals(input.text))
                throw new IOException("Source changed during compilation: " + input.path);
            result.sources.put(input.path, new Source(input, stamp));
        }
        if (result.annotations != null) result.collectEntries(columnUnit);
        result.collectSymbols();
        if (index != null) result.publish(base);
        return result;
    }

    private void collectEntries(SourceCodePosition.ColumnUnit columnUnit) throws IOException {
        NavigableMap<Integer, Set<SourceCodeAnnotation>> mapped = new TreeMap<>(annotations.getAll(SourceCodeAnnotation.class));
        for (Set<SourceCodeAnnotation> at : mapped.values()) {
            for (SourceCodeAnnotation annotation : at) {
                Path path = SourceInputs.normalize(Path.of(annotation.getPosition().fileName));
                if (!sources.containsKey(path))
                    throw new IOException("Source was not captured before compilation: " + path);
            }
        }
        if (!mapped.isEmpty() && columnUnit == SourceCodePosition.ColumnUnit.UNKNOWN) {
            diagnostics.add("Compiler does not declare a source column convention; mappings excluded. "
                    + "Declare getSourcePositionColumnUnit() as UTF16 or CODE_POINT (line 1, column 0).");
            return;
        }
        for (Map.Entry<Integer, Set<SourceCodeAnnotation>> at : mapped.entrySet()) {
            int address = at.getKey();
            List<SourceCodeAnnotation> ordered = new ArrayList<>(at.getValue());
            ordered.sort(Comparator.comparing((SourceCodeAnnotation a) -> a.getPosition().fileName)
                    .thenComparingInt(a -> a.getPosition().line).thenComparingInt(a -> a.getPosition().column)
                    .thenComparingLong(SourceCodeAnnotation::getPluginId));
            for (SourceCodeAnnotation annotation : ordered) {
                SourceCodePosition position = annotation.getPosition();
                Source source = sources.get(SourceInputs.normalize(Path.of(position.fileName)));
                int offset = source.text.compilerOffset(position, columnUnit);
                if (address < 0 || address >= layout.addressSpaceSize() || offset < 0) {
                    diagnostics.add("Unsupported source mapping " + position + " at cell " + address
                            + ": address or source coordinate is outside captured content (EOF has no extent)");
                    continue;
                }
                entries.add(new Entry(entries.size(), address, annotation, source, source.text.span(offset, revision)));
            }
        }
    }

    private void publish(ProgramIndex.Snapshot base) throws IOException {
        try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
            update.removeMappings(MAPPING_OWNER);
            update.removeAnnotations(MAPPING_OWNER);
            for (Entry entry : entries) {
                // Compiler metadata proves a single generated cell, not an instruction's full coverage.
                update.putMapping(MAPPING_OWNER, entry.id,
                        new ProgramIndex.Range(entry.location.address, entry.location.address + 1),
                        entry.span);
            }
            for (int i = 0; i < declarations.size(); i++) {
                Declaration declaration = declarations.get(i);
                if (declaration.entry != null && !declaration.name.isEmpty()) {
                    Location location = declaration.entry.location;
                    update.putAnnotation(MAPPING_OWNER, i,
                            new ProgramIndex.Range(location.address, location.address + 1),
                            ProgramIndex.Kinds.LABEL, declaration.name);
                }
            }
            if (update.commit() != ProgramIndex.CommitResult.PUBLISHED)
                throw new IOException("Source mappings discarded: memory or metadata changed during capture");
        }
    }

    /** Retries until published; callers must hold off memory revisions, or the retry can starve. */
    static void clearPublished(ProgramIndex index) {
        index.editContent((base, update) -> {
            update.removeMappings(MAPPING_OWNER).removeAnnotations(MAPPING_OWNER);
            return true;
        });
    }

    public boolean matches(Path path, String text) {
        Source source = sources.get(SourceInputs.normalize(path));
        return source != null && source.text.sameContent(text);
    }

    public Set<Path> sourceFiles() { return Set.copyOf(sources.keySet()); }

    /** An unchanged editor buffer must not hide externally saved changes to a compiler input. */
    public boolean inputsUnchanged() {
        for (Source source : sources.values())
            if (!source.unchangedOnDisk()) return false;
        return true;
    }

    public Optional<Location> addressOf(SourceCodePosition position) {
        Source source = sources.get(SourceInputs.normalize(Path.of(position.fileName)));
        if (source == null || source.text.editorOffset(position) < 0 || index == null) return Optional.empty();
        ProgramIndex.Snapshot snapshot = index.snapshot();
        Entry entry = unique(source, position.line, snapshot);
        if (entry != null) return Optional.of(entry.location);
        entry = source.labels.get(position.line);
        return live(entry, snapshot) ? Optional.of(entry.location) : Optional.empty();
    }

    public Optional<Location> sourceAt(int address) {
        if (!sameLayout() || address < 0 || address >= layout.addressSpaceSize())
            return Optional.empty();
        ProgramIndex.Snapshot snapshot = index.snapshot();
        List<Entry> matches = new ArrayList<>();
        snapshot.forEachMappingAt(address, mapping -> {
            Entry entry = entryFor(mapping);
            if (live(entry, mapping, snapshot)) matches.add(entry);
        });
        return matches.size() == 1 ? Optional.of(matches.get(0).location) : Optional.empty();
    }

    public List<Symbol> symbols() {
        List<Symbol> result = new ArrayList<>();
        ProgramIndex.Snapshot snapshot = index == null ? null : index.snapshot();
        for (Declaration declaration : declarations) {
            Entry entry = declaration.entry;
            result.add(new Symbol(declaration.name, declaration.position,
                    live(entry, snapshot) ? Optional.of(entry.location) : Optional.empty(), declaration.sourceOnly));
        }
        return List.copyOf(result);
    }

    private void collectSymbols() {
        // Capture-time grouping supports label-only lines. It is discarded after publication;
        // current address/source queries always use ProgramIndex.
        Map<Source, NavigableMap<Integer, List<Entry>>> lines = new HashMap<>();
        for (Entry entry : entries)
            lines.computeIfAbsent(entry.source, source -> new TreeMap<>())
                    .computeIfAbsent(entry.location.position.line, line -> new ArrayList<>()).add(entry);
        for (Source source : sources.values())
            collectSymbols(source, lines.getOrDefault(source, Collections.emptyNavigableMap()));
    }

    private void collectSymbols(Source source, NavigableMap<Integer, List<Entry>> lines) {
        for (Token token : source.text.tokens()) {
            if (token.getType() != Token.SYMBOL_DEFINITION || token.getSymbolName() == null) continue;
            SourceCodePosition position = source.text.position(token.getOffset());
            if (position == null) continue;
            // Constants, macros and macro parameters are navigable source symbols, not addresses.
            boolean label = token.getText().endsWith(":");
            Entry entry = null;
            if (label) {
                List<Entry> onLine = lines.getOrDefault(position.line, List.of());
                if (onLine.size() == 1) entry = onLine.get(0);
                if (onLine.isEmpty()) {
                    Map.Entry<Integer, List<Entry>> next = lines.higherEntry(position.line);
                    if (next != null && next.getValue().size() == 1) {
                        Entry candidate = next.getValue().get(0);
                        int from = token.getOffset() + token.getText().length();
                        if (source.text.onlyCommentsOrWhitespace(from, candidate.span.startOffset)) entry = candidate;
                    }
                    if (entry != null) source.labels.put(position.line, entry);
                }
            }
            declarations.add(new Declaration(token.getSymbolName(), position, entry, !label));
        }
    }

    private boolean sameLayout() {
        return memory != null && memory.annotations() == annotations && index.snapshot().layout() == layout;
    }

    private Entry unique(Source source, int line, ProgramIndex.Snapshot snapshot) {
        ProgramIndex.SourceSpan span = source.text.lineSpan(line, revision);
        List<Entry> matches = new ArrayList<>();
        if (span != null && snapshot != null) snapshot.forEachMappingOverlappingSource(span, mapping -> {
            Entry entry = entryFor(mapping);
            if (live(entry, mapping, snapshot)) matches.add(entry);
        });
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private Entry entryFor(ProgramIndex.Mapping mapping) {
        if (mapping.ownerId() != MAPPING_OWNER || mapping.entryId() < 0 || mapping.entryId() >= entries.size()) return null;
        Entry entry = entries.get((int) mapping.entryId());
        return entry.span.equals(mapping.source()) ? entry : null;
    }

    private boolean live(Entry entry, ProgramIndex.Snapshot snapshot) {
        if (entry == null || snapshot == null) return false;
        boolean[] found = {false};
        snapshot.forEachMappingAt(entry.location.address, mapping -> {
            if (entryFor(mapping) == entry && live(entry, mapping, snapshot)) found[0] = true;
        });
        return found[0];
    }

    private boolean live(Entry entry, ProgramIndex.Mapping mapping, ProgramIndex.Snapshot snapshot) {
        if (entry == null || !sameLayout() || mapping.memoryRevision() != snapshot.memoryRevision()) return false;
        Set<SourceCodeAnnotation> current = annotations.get(entry.location.address, SourceCodeAnnotation.class);
        for (SourceCodeAnnotation annotation : current) if (annotation == entry.annotation) return true;
        return false;
    }

    public static final class Location {
        public final int address;
        public final SourceCodePosition position;
        public final boolean instruction;

        private Location(int address, SourceCodePosition position, boolean instruction) {
            this.address = address;
            this.position = position;
            this.instruction = instruction;
        }
    }

    public static final class Symbol {
        public final String name;
        public final SourceCodePosition position;
        public final Optional<Location> location;
        public final boolean sourceOnly;

        private Symbol(String name, SourceCodePosition position, Optional<Location> location, boolean sourceOnly) {
            this.name = name;
            this.position = position;
            this.location = location;
            this.sourceOnly = sourceOnly;
        }

        @Override public String toString() {
            String at = location.map(value -> String.format("%04X", value.address))
                    .orElse(sourceOnly ? "source only" : "address unavailable");
            return name + " — " + Path.of(position.fileName).getFileName() + ":" + position.line + " — " + at;
        }
    }

    private static final class Entry {
        final long id;
        final Location location;
        final SourceCodeAnnotation annotation;
        final Source source;
        final ProgramIndex.SourceSpan span;

        Entry(long id, int address, SourceCodeAnnotation annotation, Source source, ProgramIndex.SourceSpan span) {
            this.id = id;
            location = new Location(address, source.text.position(span.startOffset),
                    source.text.instructionAt(span.startOffset));
            this.annotation = annotation;
            this.source = source;
            this.span = span;
        }
    }

    private static final class Declaration {
        final String name;
        final SourceCodePosition position;
        final Entry entry;
        final boolean sourceOnly;

        Declaration(String name, SourceCodePosition position, Entry entry, boolean sourceOnly) {
            this.name = name;
            this.position = position;
            this.entry = entry;
            this.sourceOnly = sourceOnly;
        }
    }

    private static final class Source {
        final SourceText text;
        final Map<Integer, Entry> labels = new HashMap<>();
        private volatile FileStamp stamp;

        Source(SourceText text, FileStamp stamp) {
            this.text = text;
            this.stamp = stamp;
        }

        /** Unchanged size and modification time skip the read; a touched file still compares content. */
        boolean unchangedOnDisk() {
            try {
                FileStamp current = FileStamp.of(text.path);
                if (current.equals(stamp)) return true;
                if (!Files.readString(text.path).equals(text.text)) return false;
                stamp = current;
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static final class FileStamp {
        final long size;
        final FileTime modified;

        private FileStamp(long size, FileTime modified) {
            this.size = size;
            this.modified = modified;
        }

        static FileStamp of(Path path) throws IOException {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return new FileStamp(attributes.size(), attributes.lastModifiedTime());
        }

        @Override public boolean equals(Object other) {
            return other instanceof FileStamp && ((FileStamp) other).size == size
                    && ((FileStamp) other).modified.equals(modified);
        }

        @Override public int hashCode() { return Objects.hash(size, modified); }
    }
}
