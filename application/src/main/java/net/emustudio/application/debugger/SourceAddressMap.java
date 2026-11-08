/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** Source navigation captured after a successful compilation, using CPU-visible addresses. */
public final class SourceAddressMap {
    private final MemoryContext<?> memory;
    private final MemoryAnnotations annotations;
    private final ProgramIndex index;
    private final ProgramIndex.Layout layout;
    private final long revision;
    private final long memoryRevision;
    private final Map<Path, Source> sources = new LinkedHashMap<>();
    private final List<String> diagnostics = new ArrayList<>();
    private final SourceCodePosition.ColumnUnit columnUnit;
    private volatile Projection cachedProjection;

    private SourceAddressMap(MemoryContext<?> memory, ProgramIndex index, ProgramIndex.Snapshot base,
                             MemoryAnnotations annotations, SourceCodePosition.ColumnUnit columnUnit) {
        this.memory = memory;
        this.index = index;
        this.annotations = annotations;
        this.columnUnit = columnUnit;
        layout = base == null ? null : base.layout();
        revision = base == null ? 0 : Math.incrementExact(base.contentRevision());
        memoryRevision = base == null ? 0 : base.memoryRevision();
    }

    public static SourceAddressMap empty() {
        return new SourceAddressMap(null, null, null, null, SourceCodePosition.ColumnUnit.UNKNOWN);
    }

    public ProgramIndex programIndex() { return index; }

    /** Unsupported compiler positions are excluded from bindings, with their reason retained here. */
    public List<String> diagnostics() { return List.copyOf(diagnostics); }

    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, SourceInputs inputs,
                                           ProgramIndex index, MemoryAnnotations annotations) throws IOException {
        SourceCodePosition.ColumnUnit unit = compiler == null ? null : compiler.getSourcePositionColumnUnit();
        return capture(memory, inputs, index, unit == null ? SourceCodePosition.ColumnUnit.UNKNOWN : unit, annotations);
    }

    /** Capture source inputs, then validate the current compiler mappings. */
    static SourceAddressMap capture(MemoryContext<?> memory, SourceInputs inputs, ProgramIndex index,
                                    SourceCodePosition.ColumnUnit columnUnit, MemoryAnnotations annotations) throws IOException {
        Objects.requireNonNull(columnUnit);
        if (memory != null && index == null)
            throw new IllegalArgumentException("Source capture with memory requires its program index");
        ProgramIndex.Snapshot base = index == null ? null : index.snapshot();
        SourceAddressMap result = new SourceAddressMap(memory, index, base, annotations, columnUnit);
        long annotationRevision = annotations == null ? 0 : annotations.revision();
        for (SourceText input : inputs.files().values()) {
            // Stamp before reading, so a write racing this check always changes the stamp.
            FileStamp stamp = FileStamp.of(input.path);
            if (!Files.readString(input.path).equals(input.text))
                throw new IOException("Source changed during compilation: " + input.path);
            result.sources.put(input.path, new Source(input, stamp));
        }
        result.cachedProjection = result.project(true);
        if (index != null && (!index.snapshot().sameRevisions(base)
                || annotations != null && annotations.revision() != annotationRevision))
            throw new IOException("Source mappings discarded: memory or metadata changed during capture");
        return result;
    }

    private Projection project(boolean strict) throws IOException {
        Projection projection = new Projection(annotations == null ? 0 : annotations.revision());
        if (annotations == null || layout == null) {
            collectSymbols(projection);
            return projection;
        }
        List<MemoryAnnotations.Entry> mapped = new ArrayList<>();
        for (MemoryAnnotations.Entry entry : annotations.all())
            if (entry.kind.equals("source") && entry.source != null) mapped.add(entry);
        for (MemoryAnnotations.Entry annotation : mapped) {
            Path path = SourceInputs.normalize(Path.of(annotation.source.fileName));
            if (strict && !sources.containsKey(path))
                throw new IOException("Source was not captured before compilation: " + path);
        }
        if (!mapped.isEmpty() && columnUnit == SourceCodePosition.ColumnUnit.UNKNOWN) {
            if (strict) diagnostics.add("Compiler does not declare a source column convention; mappings excluded. "
                    + "Declare getSourcePositionColumnUnit() as UTF16 or CODE_POINT (line 1, column 0).");
            collectSymbols(projection);
            return projection;
        }
        mapped.sort(Comparator.comparingInt((MemoryAnnotations.Entry a) -> a.start)
                .thenComparing(a -> a.source.fileName).thenComparingInt(a -> a.source.line)
                .thenComparingInt(a -> a.source.column).thenComparingLong(a -> a.ownerId));
        for (MemoryAnnotations.Entry annotation : mapped) {
            int address = annotation.start;
            SourceCodePosition position = annotation.source;
            Source source = sources.get(SourceInputs.normalize(Path.of(position.fileName)));
            if (source == null) continue;
            int offset = source.text.compilerOffset(position, columnUnit);
            if (address < 0 || address >= layout.addressSpaceSize() || offset < 0) {
                if (strict) diagnostics.add("Unsupported source mapping " + position + " at cell " + address
                        + ": address or source coordinate is outside captured content (EOF has no extent)");
                continue;
            }
            projection.entries.add(new Entry(projection.entries.size(), address, annotation, source, source.text.span(offset, revision)));
        }
        collectSymbols(projection);
        return projection;
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
        if (source == null || source.text.editorOffset(position) < 0 || !sameLayout() || !inputsUnchanged()) return Optional.empty();
        for (int attempt = 0; attempt < 3; attempt++) {
            long revision = annotations.revision();
            ProgramIndex.Snapshot snapshot = index.snapshot();
            Projection projection = currentProjection();
            Entry entry = unique(source, position.line, snapshot, projection);
            if (entry == null) for (Declaration declaration : projection.declarations)
                if (SourceInputs.normalize(Path.of(declaration.position.fileName)).equals(SourceInputs.normalize(Path.of(position.fileName)))
                        && declaration.position.line == position.line && live(declaration.entry, snapshot)) {
                    entry = declaration.entry;
                    break;
                }
            if (snapshot.memoryRevision() != index.snapshot().memoryRevision()) return Optional.empty();
            if (annotations.revision() == revision) return entry == null ? Optional.empty() : Optional.of(entry.location);
        }
        return Optional.empty();
    }

    public Optional<Location> sourceAt(int address) {
        if (!sameLayout() || !inputsUnchanged() || address < 0 || address >= layout.addressSpaceSize())
            return Optional.empty();
        for (int attempt = 0; attempt < 3; attempt++) {
            long revision = annotations.revision();
            ProgramIndex.Snapshot snapshot = index.snapshot();
            List<Entry> matches = new ArrayList<>();
            for (Entry entry : currentProjection().entries)
                if (entry.location.address == address && live(entry, snapshot)) matches.add(entry);
            if (snapshot.memoryRevision() != index.snapshot().memoryRevision()) return Optional.empty();
            if (annotations.revision() == revision)
                return matches.size() == 1 ? Optional.of(matches.get(0).location) : Optional.empty();
        }
        return Optional.empty();
    }

    public List<Symbol> symbols() {
        for (int attempt = 0; attempt < 3; attempt++) {
            long revision = annotations == null ? 0 : annotations.revision();
            List<Symbol> result = new ArrayList<>();
            ProgramIndex.Snapshot snapshot = index == null || !inputsUnchanged() ? null : index.snapshot();
            for (Declaration declaration : currentProjection().declarations) {
                Entry entry = declaration.entry;
                result.add(new Symbol(declaration.name, declaration.position,
                        live(entry, snapshot) ? Optional.of(entry.location) : Optional.empty(), declaration.sourceOnly));
            }
            if (snapshot != null && snapshot.memoryRevision() != index.snapshot().memoryRevision()) continue;
            if (annotations == null || annotations.revision() == revision) return List.copyOf(result);
        }
        return List.of();
    }

    private Projection currentProjection() {
        long annotationRevision = annotations == null ? 0 : annotations.revision();
        Projection cached = cachedProjection;
        if (cached != null && cached.annotationRevision == annotationRevision) return cached;
        try {
            Projection current = project(false);
            if (annotations == null || annotations.revision() == current.annotationRevision)
                cachedProjection = current;
            return current;
        }
        catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    private void collectSymbols(Projection projection) {
        Map<Source, NavigableMap<Integer, List<Entry>>> lines = new HashMap<>();
        for (Entry entry : projection.entries)
            lines.computeIfAbsent(entry.source, source -> new TreeMap<>())
                    .computeIfAbsent(entry.location.position.line, line -> new ArrayList<>()).add(entry);
        for (Source source : sources.values())
            collectSymbols(source, lines.getOrDefault(source, Collections.emptyNavigableMap()), projection);
    }

    private void collectSymbols(Source source, NavigableMap<Integer, List<Entry>> lines, Projection projection) {
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
                }
            }
            projection.declarations.add(new Declaration(token.getSymbolName(), position, entry, !label));
        }
    }

    private boolean sameLayout() {
        return memory != null && annotations != null && index.snapshot().layout() == layout;
    }

    private Entry unique(Source source, int line, ProgramIndex.Snapshot snapshot, Projection projection) {
        ProgramIndex.SourceSpan span = source.text.lineSpan(line, revision);
        List<Entry> matches = new ArrayList<>();
        if (span != null && snapshot != null) for (Entry entry : projection.entries)
            if (entry.span.fileId.equals(span.fileId) && entry.span.sourceRevision == span.sourceRevision
                    && entry.span.startOffset < span.endOffset && span.startOffset < entry.span.endOffset
                    && live(entry, snapshot)) matches.add(entry);
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean live(Entry entry, ProgramIndex.Snapshot snapshot) {
        return entry != null && snapshot != null && sameLayout() && memoryRevision == snapshot.memoryRevision()
                && annotations.get(entry.annotation.ownerId, entry.annotation.entryId) == entry.annotation;
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
        final MemoryAnnotations.Entry annotation;
        final Source source;
        final ProgramIndex.SourceSpan span;

        Entry(long id, int address, MemoryAnnotations.Entry annotation, Source source, ProgramIndex.SourceSpan span) {
            this.id = id;
            location = new Location(address, source.text.position(span.startOffset),
                    source.text.instructionAt(span.startOffset));
            this.annotation = annotation;
            this.source = source;
            this.span = span;
        }
    }

    private static final class Projection {
        final long annotationRevision;
        final List<Entry> entries = new ArrayList<>();
        final List<Declaration> declarations = new ArrayList<>();

        Projection(long annotationRevision) {
            this.annotationRevision = annotationRevision;
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
