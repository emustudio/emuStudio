/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;

import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

/** Source navigation captured after a successful compilation; never changes CPU or memory banks. */
public final class SourceAddressMap {
    private final MemoryContext<?> memory;
    private final MemoryContextAnnotations annotations;
    private final int size, bankCount, common;
    private final NavigableMap<Integer, Entry> addresses = new TreeMap<>();
    private final Map<Path, Source> sources = new LinkedHashMap<>();
    private final List<Declaration> declarations = new ArrayList<>();

    private SourceAddressMap(MemoryContext<?> memory) {
        this.memory = memory;
        annotations = memory == null ? null : memory.annotations();
        size = memory == null ? 0 : memory.getSize();
        bankCount = memory == null ? 0 : memory.getBanksCount();
        common = memory == null ? 0 : memory.getCommonBoundary();
    }

    public static SourceAddressMap empty() { return new SourceAddressMap(null); }

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
        SourceAddressMap result = new SourceAddressMap(memory);
        for (Map.Entry<Path, String> input : inputs.entrySet()) {
            Path path = normalize(input.getKey());
            String text = input.getValue();
            if (!Files.readString(path).equals(text))
                throw new IOException("Source changed during compilation: " + path);
            Source previous = result.sources.putIfAbsent(path, new Source(path, text, compiler));
            if (previous != null && !previous.text.equals(text))
                throw new IOException("Conflicting source inputs: " + path);
        }
        if (result.annotations == null) {
            for (Source source : result.sources.values()) result.collectSymbols(source);
            return result;
        }
        int compiledBank = memory.getSelectedBank();
        if (compiledBank < 0 || compiledBank >= result.bankCount) return empty();
        Map<Integer, Set<SourceCodeAnnotation>> all = result.annotations.getAll(SourceCodeAnnotation.class);
        NavigableMap<Integer, Set<SourceCodeAnnotation>> mapped = new TreeMap<>(all);
        for (Set<SourceCodeAnnotation> at : mapped.values()) {
            for (SourceCodeAnnotation annotation : at) {
                Path path = normalize(Path.of(annotation.getPosition().fileName));
                if (!result.sources.containsKey(path))
                    throw new IOException("Source was not captured before compilation: " + path);
            }
        }
        for (Map.Entry<Integer, Set<SourceCodeAnnotation>> at : mapped.entrySet()) {
            if (at.getKey() < 0 || at.getKey() >= result.size || at.getValue().size() != 1) continue;
            SourceCodeAnnotation annotation = at.getValue().iterator().next();
            SourceCodePosition position = annotation.getPosition();
            if (position.line < 1 || position.column < 0) continue;
            Path path = normalize(Path.of(position.fileName));
            Source source = result.sources.get(path);
            if (source.offset(position) < 0) continue;
            int bank = at.getKey() < result.common ? compiledBank : 0;
            Entry entry = new Entry(at.getKey(), bank, position, annotation, source);
            result.addresses.put(entry.location.address, entry);
            source.lines.computeIfAbsent(position.line, line -> new ArrayList<>()).add(entry);
        }
        for (Entry entry : result.addresses.values()) {
            int start = entry.location.address;
            // Compiler tokens distinguish instruction keywords from data/directive mappings.
            if (disassembler != null && entry.source.instructionAt(entry.location.position)) {
                try {
                    disassembler.disassemble(start);
                    int end = disassembler.getNextInstructionPosition(start);
                    Integer next = mapped.higherKey(start);
                    if (next != null) end = Math.min(end, next);
                    end = Math.min(end, start < result.common ? result.common : result.size);
                    if (end > start) entry.end = end;
                } catch (InvalidInstructionException | RuntimeException ignored) {
                    // Exact compiler mappings remain usable when no instruction span is proven.
                }
            }
        }
        for (Source source : result.sources.values()) result.collectSymbols(source);
        return result;
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
        if (source == null || position.line < 1) return Optional.empty();
        Entry entry = source.unique(position.line);
        if (entry == null) entry = source.labels.get(position.line);
        return live(entry) ? Optional.of(entry.location) : Optional.empty();
    }

    public Optional<Location> sourceAt(int address, int bank) {
        if (!sameLayout() || address < 0 || address >= size || bank < 0 || bank >= bankCount)
            return Optional.empty();
        Map.Entry<Integer, Entry> floor = addresses.floorEntry(address);
        if (floor == null) return Optional.empty();
        Entry entry = floor.getValue();
        int physicalBank = address < common ? bank : 0;
        return address < entry.end && entry.location.bank == physicalBank && live(entry)
                ? Optional.of(entry.location) : Optional.empty();
    }

    public List<Symbol> symbols() {
        List<Symbol> result = new ArrayList<>();
        for (Declaration declaration : declarations) {
            Entry entry = declaration.entry;
            result.add(new Symbol(declaration.name, declaration.position,
                    live(entry) ? Optional.of(entry.location) : Optional.empty()));
        }
        return List.copyOf(result);
    }

    private void collectSymbols(Source source) {
        for (Token token : source.tokens) {
            if (token.getType() != Token.SYMBOL_DEFINITION || token.getSymbolName() == null) continue;
            SourceCodePosition position = source.position(token.getOffset());
            if (position == null) continue;
            Entry entry = source.unique(position.line);
            if (entry == null && !source.lines.containsKey(position.line) && token.getText().endsWith(":")) {
                Map.Entry<Integer, List<Entry>> next = source.lines.higherEntry(position.line);
                if (next != null && next.getValue().size() == 1) {
                    Entry candidate = next.getValue().get(0);
                    int from = token.getOffset() + token.getText().length();
                    int to = source.offset(candidate.location.position);
                    if (source.onlyCommentsOrWhitespace(from, to)) entry = candidate;
                }
                if (entry != null) source.labels.put(position.line, entry);
            }
            // Constants, macros and macro parameters are navigable source symbols, not addresses.
            if (!token.getText().endsWith(":")) entry = null;
            declarations.add(new Declaration(token.getSymbolName(), position, entry));
        }
    }

    private boolean sameLayout() {
        return memory != null && memory.annotations() == annotations && memory.getSize() == size
                && memory.getBanksCount() == bankCount && memory.getCommonBoundary() == common;
    }

    private boolean live(Entry entry) {
        if (entry == null || !sameLayout()) return false;
        Set<SourceCodeAnnotation> current = annotations.get(entry.location.address, SourceCodeAnnotation.class);
        return current.size() == 1 && current.iterator().next() == entry.annotation;
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

        private Symbol(String name, SourceCodePosition position, Optional<Location> location) {
            this.name = name;
            this.position = position;
            this.location = location;
        }

        @Override public String toString() {
            String at = location.map(value -> String.format("bank %d:%04X", value.bank, value.address))
                    .orElse("unmapped");
            return name + " — " + Path.of(position.fileName).getFileName() + ":" + position.line + " — " + at;
        }
    }

    private static final class Entry {
        final Location location;
        final SourceCodeAnnotation annotation;
        final Source source;
        int end;

        Entry(int address, int bank, SourceCodePosition position, SourceCodeAnnotation annotation, Source source) {
            location = new Location(address, bank, SourceCodePosition.of(position.line, position.column, source.path.toString()),
                    source.instructionAt(position));
            this.annotation = annotation;
            this.source = source;
            end = address + 1;
        }
    }

    private static final class Declaration {
        final String name;
        final SourceCodePosition position;
        final Entry entry;

        Declaration(String name, SourceCodePosition position, Entry entry) {
            this.name = name;
            this.position = position;
            this.entry = entry;
        }
    }

    private static final class Source {
        final Path path;
        final String text;
        final List<Token> tokens;
        final int[] starts;
        final NavigableMap<Integer, List<Entry>> lines = new TreeMap<>();
        final Map<Integer, Entry> labels = new HashMap<>();

        Source(Path path, String text, Compiler compiler) {
            this.path = path;
            this.text = text;
            starts = java.util.stream.IntStream.concat(java.util.stream.IntStream.of(0),
                    java.util.stream.IntStream.range(0, text.length()).filter(i -> text.charAt(i) == '\n').map(i -> i + 1))
                    .toArray();
            tokens = analyze(compiler, text);
        }

        Entry unique(int line) {
            List<Entry> at = lines.get(line);
            return at != null && at.size() == 1 ? at.get(0) : null;
        }

        int offset(SourceCodePosition position) {
            if (position.line < 1 || position.line > starts.length || position.column < 0) return -1;
            int start = starts[position.line - 1];
            int end = position.line == starts.length ? text.length() : starts[position.line] - 1;
            return position.column <= end - start ? start + position.column : -1;
        }

        SourceCodePosition position(int offset) {
            if (offset < 0 || offset > text.length()) return null;
            int line = Arrays.binarySearch(starts, offset);
            if (line < 0) line = -line - 2;
            return SourceCodePosition.of(line + 1, offset - starts[line], path.toString());
        }

        boolean instructionAt(SourceCodePosition position) {
            int at = offset(position);
            return tokens.stream().anyMatch(token -> token.getOffset() == at && token.getType() == Token.RESERVED);
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
