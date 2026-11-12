/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.compiler.Token;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** Source navigation captured after a successful compilation, using CPU-visible addresses. */
public final class SourceAddressMap {
    private static final int MAX_CAPTURE_ATTEMPTS = 3;
    private static final Comparator<MemoryAnnotations.Entry> SOURCE_ORDER =
            Comparator.comparingInt((MemoryAnnotations.Entry entry) -> entry.start)
                    .thenComparing(entry -> entry.source.fileName).thenComparingInt(entry -> entry.source.line)
                    .thenComparingInt(entry -> entry.source.column).thenComparingLong(entry -> entry.ownerId);
    private final MemoryContext<?> memory;
    private final MemoryAnnotations annotations;
    private final DebuggerSession session;
    private final int addressSpaceSize;
    private final long revision;
    private final long memoryImageRevision;
    private final Map<Path, Source> sources = new LinkedHashMap<>();
    private final List<String> diagnostics = new ArrayList<>();
    private final SourceCodePosition.ColumnUnit columnUnit;
    private volatile Projection cachedProjection;

    private SourceAddressMap(MemoryContext<?> memory, DebuggerSession session,
                             MemoryAnnotations annotations, SourceCodePosition.ColumnUnit columnUnit) {
        this.memory = memory;
        this.session = session;
        this.annotations = annotations;
        this.columnUnit = columnUnit;
        addressSpaceSize = session == null ? 0 : session.addressSpaceSize();
        memoryImageRevision = session == null ? 0 : session.memoryImageRevision();
        revision = annotations == null ? 0 : annotations.revision();
    }

    public DebuggerSession session() { return session; }

    /** Unsupported compiler positions are excluded from bindings, with their reason retained here. */
    public List<String> diagnostics() { return List.copyOf(diagnostics); }

    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, SourceInputs inputs,
                                           DebuggerSession session, MemoryAnnotations annotations) throws IOException {
        SourceCodePosition.ColumnUnit unit = compiler == null ? null : compiler.getSourcePositionColumnUnit();
        return capture(memory, inputs, session, unit == null ? SourceCodePosition.ColumnUnit.UNKNOWN : unit, annotations);
    }

    /** Capture source inputs, then validate the current compiler mappings. */
    static SourceAddressMap capture(MemoryContext<?> memory, SourceInputs inputs, DebuggerSession session,
                                    SourceCodePosition.ColumnUnit columnUnit, MemoryAnnotations annotations) throws IOException {
        Objects.requireNonNull(columnUnit);
        if (memory != null && session == null)
            throw new IllegalArgumentException("Source capture with memory requires its debugger session");
        long programRevision = session == null ? 0 : session.programRevision();
        SourceAddressMap result = new SourceAddressMap(memory, session, annotations, columnUnit);
        long annotationRevision = annotations == null ? 0 : annotations.revision();
        for (SourceText input : inputs.files().values()) {
            // Stamp before reading, so a write racing this check always changes the stamp.
            FileStamp stamp = FileStamp.of(input.path);
            if (!Files.readString(input.path).equals(input.text))
                throw new IOException("Source changed during compilation: " + input.path);
            result.sources.put(input.path, new Source(input, stamp));
        }
        result.cachedProjection = result.project(true);
        if (session != null && (session.programRevision() != programRevision
                || annotations != null && annotations.revision() != annotationRevision))
            throw new IOException("Source mappings discarded: memory or metadata changed during capture");
        return result;
    }

    private Projection project(boolean strict) throws IOException {
        Projection projection = new Projection(annotations == null ? 0 : annotations.revision());
        if (annotations == null || session == null) {
            collectSymbols(projection);
            return projection;
        }
        List<MemoryAnnotations.Entry> mapped = sourceAnnotations(strict);
        if (!mapped.isEmpty() && columnUnit == SourceCodePosition.ColumnUnit.UNKNOWN) {
            if (strict) diagnostics.add("Compiler does not declare a source column convention; mappings excluded. "
                    + "Declare getSourcePositionColumnUnit() as UTF16 or CODE_POINT (line 1, column 0).");
            collectSymbols(projection);
            return projection;
        }
        mapped.sort(SOURCE_ORDER);
        for (MemoryAnnotations.Entry annotation : mapped) {
            addMapping(projection, annotation, strict);
        }
        collectSymbols(projection);
        return projection;
    }

    private List<MemoryAnnotations.Entry> sourceAnnotations(boolean strict) throws IOException {
        List<MemoryAnnotations.Entry> mapped = new ArrayList<>();
        for (MemoryAnnotations.Entry entry : annotations.all()) {
            if (entry.kind.equals("source") && entry.source != null) mapped.add(entry);
        }
        for (MemoryAnnotations.Entry annotation : mapped) {
            Path path = SourceInputs.normalize(Path.of(annotation.source.fileName));
            if (strict && !sources.containsKey(path))
                throw new IOException("Source was not captured before compilation: " + path);
        }
        return mapped;
    }

    private void addMapping(Projection projection, MemoryAnnotations.Entry annotation, boolean strict) {
        int address = annotation.start;
        SourceCodePosition position = annotation.source;
        Source source = sources.get(SourceInputs.normalize(Path.of(position.fileName)));
        if (source == null) return;
        int offset = source.text.compilerOffset(position, columnUnit);
        if (address < 0 || address >= addressSpaceSize || offset < 0) {
            if (strict) diagnostics.add("Unsupported source mapping " + position + " at cell " + address
                    + ": address or source coordinate is outside captured content (EOF has no extent)");
            return;
        }
        projection.entries.add(new Entry(address, annotation, source, source.text.span(offset, revision)));
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
        for (int attempt = 0; attempt < MAX_CAPTURE_ATTEMPTS; attempt++) {
            long annotationRevision = annotations.revision();
            long currentMemoryRevision = session.memoryRevision();
            Projection projection = currentProjection();
            Entry entry = unique(source, position.line, currentMemoryRevision, projection);
            if (entry == null) entry = declarationAt(position, currentMemoryRevision, projection);
            if (currentMemoryRevision != session.memoryRevision()) return Optional.empty();
            if (annotations.revision() == annotationRevision) return entry == null ? Optional.empty() : Optional.of(entry.location);
        }
        return Optional.empty();
    }

    public Optional<Location> sourceAt(int address) {
        if (!sameLayout() || !inputsUnchanged() || address < 0 || address >= addressSpaceSize)
            return Optional.empty();
        for (int attempt = 0; attempt < MAX_CAPTURE_ATTEMPTS; attempt++) {
            long annotationRevision = annotations.revision();
            long currentMemoryRevision = session.memoryRevision();
            List<Entry> matches = new ArrayList<>();
            for (Entry entry : currentProjection().entries)
                if (entry.location.address == address && live(entry, currentMemoryRevision)) matches.add(entry);
            if (currentMemoryRevision != session.memoryRevision()) return Optional.empty();
            if (annotations.revision() == annotationRevision)
                return matches.size() == 1 ? Optional.of(matches.get(0).location) : Optional.empty();
        }
        return Optional.empty();
    }

    public List<Symbol> symbols() {
        for (int attempt = 0; attempt < MAX_CAPTURE_ATTEMPTS; attempt++) {
            long annotationRevision = annotations == null ? 0 : annotations.revision();
            List<Symbol> result = new ArrayList<>();
            long currentMemoryRevision = session == null || !inputsUnchanged() ? -1 : session.memoryRevision();
            for (Declaration declaration : currentProjection().declarations) {
                Entry entry = declaration.entry;
                result.add(new Symbol(declaration.name, declaration.position,
                        live(entry, currentMemoryRevision) ? Optional.of(entry.location) : Optional.empty(), declaration.sourceOnly));
            }
            if (currentMemoryRevision >= 0 && currentMemoryRevision != session.memoryRevision()) continue;
            if (annotations == null || annotations.revision() == annotationRevision) return List.copyOf(result);
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
            Entry entry = label ? labelTarget(source, lines, token, position.line) : null;
            projection.declarations.add(new Declaration(token.getSymbolName(), position, entry, !label));
        }
    }

    private Entry labelTarget(Source source, NavigableMap<Integer, List<Entry>> lines, Token token, int line) {
        List<Entry> onLine = lines.getOrDefault(line, List.of());
        if (onLine.size() == 1) return onLine.get(0);
        if (!onLine.isEmpty()) return null;
        Map.Entry<Integer, List<Entry>> next = lines.higherEntry(line);
        if (next == null || next.getValue().size() != 1) return null;
        Entry candidate = next.getValue().get(0);
        int from = token.getOffset() + token.getText().length();
        return source.text.onlyCommentsOrWhitespace(from, candidate.span.startOffset) ? candidate : null;
    }

    private Entry declarationAt(SourceCodePosition position, long memoryRevision, Projection projection) {
        Path path = SourceInputs.normalize(Path.of(position.fileName));
        for (Declaration declaration : projection.declarations) {
            if (SourceInputs.normalize(Path.of(declaration.position.fileName)).equals(path)
                    && declaration.position.line == position.line && live(declaration.entry, memoryRevision))
                return declaration.entry;
        }
        return null;
    }

    private boolean sameLayout() {
        return memory != null && annotations != null && session != null
                && session.isCurrentMemoryImage(addressSpaceSize, memoryImageRevision);
    }

    private Entry unique(Source source, int line, long currentMemoryRevision, Projection projection) {
        SourceText.SourceSpan span = source.text.lineSpan(line, revision);
        List<Entry> matches = new ArrayList<>();
        if (span != null && currentMemoryRevision >= 0) for (Entry entry : projection.entries)
            if (entry.span.fileId.equals(span.fileId) && entry.span.sourceRevision == span.sourceRevision
                    && entry.span.startOffset < span.endOffset && span.startOffset < entry.span.endOffset
                    && live(entry, currentMemoryRevision)) matches.add(entry);
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean live(Entry entry, long currentMemoryRevision) {
        return entry != null && currentMemoryRevision >= 0 && sameLayout()
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
        final Location location;
        final MemoryAnnotations.Entry annotation;
        final Source source;
        final SourceText.SourceSpan span;

        Entry(int address, MemoryAnnotations.Entry annotation, Source source, SourceText.SourceSpan span) {
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
