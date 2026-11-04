/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;

import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/** Source navigation captured after a successful compilation; never changes CPU or memory banks. */
public final class SourceAddressMap {
    // One compiler capture belongs to the application, not to a plugin-owned annotation store.
    private static final long MAPPING_OWNER = 2;
    private final MemoryContext<?> memory;
    private final MemoryContextAnnotations annotations;
    private final int size, bankCount, common;
    private final ProgramIndex index;
    private ProgramIndex.Snapshot capturedSnapshot;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<Path, Source> sources = new LinkedHashMap<>();
    private final List<Declaration> declarations = new ArrayList<>();
    private final List<String> diagnostics = new ArrayList<>();

    private SourceAddressMap(MemoryContext<?> memory, ProgramIndex index) {
        this.memory = memory;
        this.index = index;
        annotations = memory == null ? null : memory.annotations();
        size = memory == null ? 0 : memory.getAddressSpaceSize();
        bankCount = memory == null ? 0 : memory.getBanksCount();
        common = memory == null ? 0 : memory.getCommonBoundary();
    }

    public static SourceAddressMap empty() { return new SourceAddressMap(null, null); }

    public ProgramIndex programIndex() { return index; }

    public ProgramIndex.Snapshot capturedSnapshot() { return capturedSnapshot; }

    /** Unsupported compiler positions are excluded from bindings, with their reason retained here. */
    public List<String> diagnostics() { return List.copyOf(diagnostics); }

    /** Capture compiler inputs before compilation reads them, including source-only include files. */
    public static Map<Path, String> captureInputs(Compiler compiler, Path root) throws IOException {
        Map<Path, String> inputs = new LinkedHashMap<>();
        Set<Path> visitedFiles = new HashSet<>();
        Deque<Path> pending = new ArrayDeque<>();
        pending.add(normalize(root));
        while (!pending.isEmpty()) {
            Path path = pending.removeFirst();
            if (inputs.containsKey(path)) continue;
            String text = Files.readString(path);
            inputs.put(path, text);
            // Resolve aliases only for cycle detection; retain compiler-visible paths as map keys.
            if (!visitedFiles.add(path.toRealPath())) continue;
            for (Token token : analyze(compiler, text)) {
                String included = token.getReferencedFile();
                if (token.getType() == Token.INCLUDE_PATH && included != null) {
                    Path include = Path.of(included.replace('\\', File.separatorChar).replace('/', File.separatorChar));
                    pending.add(normalize(path.getParent().resolve(include)));
                }
            }
        }
        return Collections.unmodifiableMap(inputs);
    }

    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory,
                                           Disassembler disassembler, Map<Path, String> inputs) throws IOException {
        ProgramIndex index = memory == null ? null : ProgramIndex.create("source-capture:" + UUID.randomUUID(),
                memory.getAddressSpaceSize(), memory.getBanksCount(), memory.getCommonBoundary());
        return capture(compiler, memory, disassembler, inputs, index);
    }

    /** Publish source metadata into the session's index; user annotations and other owners survive. */
    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory,
                                           Disassembler disassembler, Map<Path, String> inputs,
                                           ProgramIndex index) throws IOException {
        return capture(compiler, memory, disassembler, inputs, index, memory == null ? 0 : memory.getSelectedBank());
    }

    /** The compiler bank is captured before a subsequent CPU reset can change the selected bank. */
    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory,
                                           Disassembler disassembler, Map<Path, String> inputs,
                                           ProgramIndex index, int compiledBank) throws IOException {
        SourceCodePosition.ColumnUnit unit = compiler == null ? SourceCodePosition.ColumnUnit.UNKNOWN
                : compiler.getSourcePositionColumnUnit();
        return capture(compiler, memory, disassembler, inputs, index, compiledBank,
                unit == null ? SourceCodePosition.ColumnUnit.UNKNOWN : unit);
    }

    /** An explicit convention may be supplied by a trusted source-metadata adapter without a compiler. */
    public static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory,
                                           Disassembler disassembler, Map<Path, String> inputs,
                                           ProgramIndex index, int compiledBank,
                                           SourceCodePosition.ColumnUnit columnUnit) throws IOException {
        Objects.requireNonNull(columnUnit);
        SourceAddressMap result = new SourceAddressMap(memory, index);
        ProgramIndex.Snapshot base = index == null ? null : index.snapshot();
        if (memory != null && (base == null || base.layout().addressSpaceSize() != result.size
                || base.layout().bankCount() != result.bankCount || base.layout().commonBoundary() != result.common))
            throw new IOException("Source index layout differs from memory");
        long revision = base == null ? 0 : Math.incrementExact(base.contentRevision());
        for (Map.Entry<Path, String> input : inputs.entrySet()) {
            Path path = normalize(input.getKey());
            String text = input.getValue();
            if (!Files.readString(path).equals(text))
                throw new IOException("Source changed during compilation: " + path);
            Source previous = result.sources.putIfAbsent(path, new Source(path, text, compiler, revision));
            if (previous != null && !previous.text.equals(text))
                throw new IOException("Conflicting source inputs: " + path);
        }
        if (result.annotations == null) {
            for (Source source : result.sources.values()) result.collectSymbols(source);
            if (index != null) result.publish(base);
            return result;
        }
        if (compiledBank < 0 || compiledBank >= result.bankCount)
            throw new IOException("Compiler selected an invalid memory bank: " + compiledBank);
        Map<Integer, Set<SourceCodeAnnotation>> all = result.annotations.getAll(SourceCodeAnnotation.class);
        NavigableMap<Integer, Set<SourceCodeAnnotation>> mapped = new TreeMap<>(all);
        for (Set<SourceCodeAnnotation> at : mapped.values()) {
            for (SourceCodeAnnotation annotation : at) {
                Path path = normalize(Path.of(annotation.getPosition().fileName));
                if (!result.sources.containsKey(path))
                    throw new IOException("Source was not captured before compilation: " + path);
            }
        }
        if (!mapped.isEmpty() && columnUnit == SourceCodePosition.ColumnUnit.UNKNOWN) {
            result.diagnostics.add("Compiler does not declare a source column convention; mappings excluded. "
                    + "Declare getSourcePositionColumnUnit() as UTF16 or CODE_POINT (line 1, column 0).");
            for (Source source : result.sources.values()) result.collectSymbols(source);
            result.publish(base);
            return result;
        }
        for (Map.Entry<Integer, Set<SourceCodeAnnotation>> at : mapped.entrySet()) {
            List<SourceCodeAnnotation> ordered = new ArrayList<>(at.getValue());
            ordered.sort(Comparator.comparing((SourceCodeAnnotation a) -> a.getPosition().fileName)
                    .thenComparingInt(a -> a.getPosition().line).thenComparingInt(a -> a.getPosition().column)
                    .thenComparingLong(SourceCodeAnnotation::getPluginId));
            for (SourceCodeAnnotation annotation : ordered) {
                SourceCodePosition position = annotation.getPosition();
                Source source = result.sources.get(normalize(Path.of(position.fileName)));
                int offset = source.compilerOffset(position, columnUnit);
                if (at.getKey() < 0 || at.getKey() >= result.size || offset < 0) {
                    result.diagnostics.add("Unsupported source mapping " + position + " at cell " + at.getKey()
                            + ": address or source coordinate is outside captured content (EOF has no extent)");
                    continue;
                }
                int bank = at.getKey() < result.common ? compiledBank : 0;
                ProgramIndex.SourceSpan span = source.span(offset);
                result.entries.add(new Entry(result.entries.size(), at.getKey(), bank, annotation, source, span));
            }
        }
        for (Source source : result.sources.values()) result.collectSymbols(source);
        result.publish(base);
        return result;
    }

    private void publish(ProgramIndex.Snapshot base) throws IOException {
        try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
            update.removeMappings(MAPPING_OWNER);
            update.removeAnnotations(MAPPING_OWNER);
            for (Entry entry : entries) {
                // Compiler metadata proves a single generated cell, not an instruction's full coverage.
                update.putMapping(MAPPING_OWNER, entry.id,
                        new ProgramIndex.Range(entry.location.bank, entry.location.address, entry.location.address + 1),
                        entry.span);
            }
            for (int i = 0; i < declarations.size(); i++) {
                Declaration declaration = declarations.get(i);
                if (declaration.entry != null && !declaration.name.isEmpty()) {
                    Location location = declaration.entry.location;
                    update.putAnnotation(MAPPING_OWNER, i,
                            new ProgramIndex.Range(location.bank, location.address, location.address + 1),
                            ProgramIndex.Kinds.LABEL, declaration.name);
                }
            }
            if (update.commit() != ProgramIndex.CommitResult.PUBLISHED)
                throw new IOException("Source mappings discarded: memory or metadata changed during capture");
        }
        capturedSnapshot = index.snapshot();
    }

    static void clearPublished(ProgramIndex index) {
        for (;;) {
            ProgramIndex.Snapshot base = index.snapshot();
            try (ProgramIndex.ContentUpdate update = index.updateContent(base)) {
                update.removeMappings(MAPPING_OWNER).removeAnnotations(MAPPING_OWNER);
                if (update.commit() == ProgramIndex.CommitResult.PUBLISHED) return;
            }
        }
    }

    public boolean matches(Path path, String text) {
        Source source = sources.get(normalize(path));
        // Editors normalize CRLF on load; compiler offsets and provenance retain the raw snapshot.
        return source != null && text != null
                && source.text.replace("\r\n", "\n").equals(text.replace("\r\n", "\n"));
    }

    public Set<Path> sourceFiles() { return Set.copyOf(sources.keySet()); }

    /** An unchanged editor buffer must not hide externally saved changes to a compiler input. */
    public boolean inputsUnchanged() {
        try {
            for (Source source : sources.values())
                if (!Files.readString(source.path).equals(source.text)) return false;
            return true;
        } catch (IOException e) { return false; }
    }

    public Optional<Location> addressOf(SourceCodePosition position) {
        Source source = sources.get(normalize(Path.of(position.fileName)));
        if (source == null || source.editorOffset(position) < 0 || index == null) return Optional.empty();
        ProgramIndex.Snapshot snapshot = index.snapshot();
        Entry entry = unique(source, position.line, snapshot);
        if (entry != null) return Optional.of(entry.location);
        entry = source.labels.get(position.line);
        return live(entry, snapshot) ? Optional.of(entry.location) : Optional.empty();
    }

    public Optional<Location> sourceAt(int address, int bank) {
        if (!sameLayout() || address < 0 || address >= size || bank < 0 || bank >= bankCount)
            return Optional.empty();
        ProgramIndex.Snapshot snapshot = index.snapshot();
        List<Entry> matches = new ArrayList<>();
        snapshot.forEachMappingAt(bank, address, mapping -> {
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

    private void collectSymbols(Source source) {
        // Capture-time grouping supports label-only lines. It is discarded after publication;
        // current address/source queries always use ProgramIndex.
        NavigableMap<Integer, List<Entry>> lines = new TreeMap<>();
        for (Entry entry : entries) if (entry.source == source)
            lines.computeIfAbsent(entry.location.position.line, line -> new ArrayList<>()).add(entry);
        for (Token token : source.tokens) {
            if (token.getType() != Token.SYMBOL_DEFINITION || token.getSymbolName() == null) continue;
            SourceCodePosition position = source.position(token.getOffset());
            if (position == null) continue;
            List<Entry> onLine = lines.getOrDefault(position.line, List.of());
            Entry entry = onLine.size() == 1 ? onLine.get(0) : null;
            if (onLine.isEmpty() && token.getText().endsWith(":")) {
                Map.Entry<Integer, List<Entry>> next = lines.higherEntry(position.line);
                if (next != null && next.getValue().size() == 1) {
                    Entry candidate = next.getValue().get(0);
                    int from = token.getOffset() + token.getText().length();
                    int to = candidate.span.startOffset;
                    if (source.onlyCommentsOrWhitespace(from, to)) entry = candidate;
                }
                if (entry != null) source.labels.put(position.line, entry);
            }
            // Constants, macros and macro parameters are navigable source symbols, not addresses.
            if (!token.getText().endsWith(":")) entry = null;
            declarations.add(new Declaration(token.getSymbolName(), position, entry, !token.getText().endsWith(":")));
        }
    }

    private boolean sameLayout() {
        return memory != null && memory.annotations() == annotations && memory.getAddressSpaceSize() == size
                && memory.getBanksCount() == bankCount && memory.getCommonBoundary() == common;
    }

    private Entry unique(Source source, int line, ProgramIndex.Snapshot snapshot) {
        ProgramIndex.SourceSpan span = source.lineSpan(line);
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
        snapshot.forEachMappingAt(entry.location.bank, entry.location.address, mapping -> {
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

    private static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }

    private static List<Token> analyze(Compiler compiler, String text) {
        if (compiler != null) {
            try {
                LexicalAnalyzer lexer = compiler.createLexer();
                if (lexer != null) return List.copyOf(lexer.analyze(text));
            } catch (RuntimeException ignored) {
                // Optional semantic editing support must not disable exact source mappings.
            }
        }
        return List.of();
    }

    public static final class Location {
        public final int address, bank;
        public final SourceCodePosition position;
        public final boolean instruction;

        private Location(int address, int bank, SourceCodePosition position, boolean instruction) {
            this.address = address;
            this.bank = bank;
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
            String at = location.map(value -> String.format("bank %d:%04X", value.bank, value.address))
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

        Entry(long id, int address, int bank, SourceCodeAnnotation annotation, Source source, ProgramIndex.SourceSpan span) {
            this.id = id;
            location = new Location(address, bank, source.position(span.startOffset), source.instructionAt(span.startOffset));
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
        final Path path;
        final String text;
        final String fileId;
        final long revision;
        final List<Token> tokens;
        final int[] starts;
        final Map<Integer, Entry> labels = new HashMap<>();

        Source(Path path, String text, Compiler compiler, long revision) {
            this.path = path;
            this.text = text;
            fileId = path.toUri().toString();
            this.revision = revision;
            starts = java.util.stream.IntStream.concat(java.util.stream.IntStream.of(0),
                    java.util.stream.IntStream.range(0, text.length()).filter(i -> text.charAt(i) == '\n').map(i -> i + 1))
                    .toArray();
            tokens = analyze(compiler, text);
        }

        private int lineEnd(int line) {
            int end = line == starts.length ? text.length() : starts[line] - 1;
            if (end > starts[line - 1] && text.charAt(end - 1) == '\r') end--;
            return end;
        }

        int editorOffset(SourceCodePosition position) {
            if (position.line < 1 || position.line > starts.length || position.column < 0) return -1;
            int start = starts[position.line - 1];
            int end = lineEnd(position.line);
            return position.column < end - start ? start + position.column : -1;
        }

        // The producer declares column units; editor tokens always use Java String offsets.
        int compilerOffset(SourceCodePosition position, SourceCodePosition.ColumnUnit unit) {
            if (unit == SourceCodePosition.ColumnUnit.UTF16) return editorOffset(position);
            if (unit != SourceCodePosition.ColumnUnit.CODE_POINT) return -1;
            if (position.line < 1 || position.line > starts.length || position.column < 0) return -1;
            int start = starts[position.line - 1], end = lineEnd(position.line);
            if (position.column >= text.codePointCount(start, end)) return -1;
            return text.offsetByCodePoints(start, position.column);
        }

        ProgramIndex.SourceSpan lineSpan(int line) {
            if (line < 1 || line > starts.length || starts[line - 1] == lineEnd(line)) return null;
            return new ProgramIndex.SourceSpan(fileId, revision, starts[line - 1], lineEnd(line));
        }

        ProgramIndex.SourceSpan span(int offset) {
            Token token = tokenAt(offset);
            int end = token == null ? offset + 1 : offset + token.getText().length();
            return new ProgramIndex.SourceSpan(fileId, revision, offset, end);
        }

        SourceCodePosition position(int offset) {
            if (offset < 0 || offset >= text.length()) return null;
            int line = Arrays.binarySearch(starts, offset);
            if (line < 0) line = -line - 2;
            return SourceCodePosition.of(line + 1, offset - starts[line], path.toString());
        }

        boolean instructionAt(int offset) {
            Token token = tokenAt(offset);
            return token != null && token.getType() == Token.RESERVED;
        }

        private Token tokenAt(int offset) {
            for (Token token : tokens) {
                String tokenText = token.getText();
                if (token.getOffset() == offset && !tokenText.isEmpty() && token.getType() != Token.EOF
                        && tokenText.length() <= text.length() - offset && text.startsWith(tokenText, offset)) return token;
            }
            return null;
        }

        boolean onlyCommentsOrWhitespace(int from, int to) {
            if (from < 0 || to < from || to > text.length()) return false;
            int cursor = from;
            for (Token token : tokens) {
                int start = token.getOffset(), end = start + token.getText().length();
                if (start >= cursor && end <= to && token.getType() == Token.COMMENT) {
                    if (!text.substring(cursor, start).isBlank()) return false;
                    cursor = end;
                }
            }
            return text.substring(cursor, to).isBlank();
        }
    }
}
