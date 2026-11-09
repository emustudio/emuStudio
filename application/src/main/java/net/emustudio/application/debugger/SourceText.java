/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.compiler.Token;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** One immutable compiler input. Offsets are Java String (UTF-16) indices, lines are 1-based. */
final class SourceText {
    /** Exact document revision and nonempty half-open UTF-16 offset span. */
    public static final class SourceSpan {
        public final String fileId;
        public final long sourceRevision;
        public final int startOffset, endOffset;
        public SourceSpan(String fileId, long sourceRevision, int startOffset, int endOffset) {
            this.fileId = Objects.requireNonNull(fileId, "fileId");
            if (fileId.isEmpty() || sourceRevision < 0 || startOffset < 0 || endOffset <= startOffset)
                throw new IllegalArgumentException("Invalid source identity, revision, or span");
            this.sourceRevision = sourceRevision;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof SourceSpan)) return false;
            SourceSpan s = (SourceSpan) other;
            return sourceRevision == s.sourceRevision && startOffset == s.startOffset
                    && endOffset == s.endOffset && fileId.equals(s.fileId);
        }
        @Override public int hashCode() {
            int h = 31 * fileId.hashCode() + Long.hashCode(sourceRevision);
            return 31 * (31 * h + startOffset) + endOffset;
        }
    }


    final Path path;
    final String text;
    final String fileId;
    private final String normalizedText;
    private final List<Token> tokens;
    private final int[] starts;

    private SourceText(Path path, String text, List<Token> tokens) {
        this.path = path;
        this.text = text;
        fileId = path.toUri().toString();
        normalizedText = normalizeLineEndings(text);
        List<Token> sorted = new ArrayList<>(tokens);
        sorted.sort(Comparator.comparingInt(Token::getOffset));
        this.tokens = List.copyOf(sorted);
        starts = IntStream.concat(IntStream.of(0),
                IntStream.range(0, text.length()).filter(i -> text.charAt(i) == '\n').map(i -> i + 1)).toArray();
    }

    static SourceText lex(Compiler compiler, Path path, String text) {
        return new SourceText(path, text, analyze(compiler, text));
    }

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

    List<Token> tokens() { return tokens; }

    /** Editors normalize CRLF on load; compiler offsets and provenance retain the raw text. */
    boolean sameContent(String editorText) {
        return editorText != null && normalizedText.equals(normalizeLineEndings(editorText));
    }

    private static String normalizeLineEndings(String value) {
        return value.indexOf('\r') < 0 ? value : value.replace("\r\n", "\n");
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

    SourceText.SourceSpan lineSpan(int line, long revision) {
        if (line < 1 || line > starts.length || starts[line - 1] == lineEnd(line)) return null;
        return new SourceText.SourceSpan(fileId, revision, starts[line - 1], lineEnd(line));
    }

    SourceText.SourceSpan span(int offset, long revision) {
        Token token = tokenAt(offset);
        int end = token == null ? offset + 1 : offset + token.getText().length();
        return new SourceText.SourceSpan(fileId, revision, offset, end);
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
        for (int i = firstTokenAtOrAfter(offset); i < tokens.size() && tokens.get(i).getOffset() == offset; i++) {
            Token token = tokens.get(i);
            String tokenText = token.getText();
            if (!tokenText.isEmpty() && token.getType() != Token.EOF
                    && tokenText.length() <= text.length() - offset && text.startsWith(tokenText, offset)) return token;
        }
        return null;
    }

    boolean onlyCommentsOrWhitespace(int from, int to) {
        if (from < 0 || to < from || to > text.length()) return false;
        int cursor = from;
        for (int i = firstTokenAtOrAfter(from); i < tokens.size() && tokens.get(i).getOffset() <= to; i++) {
            Token token = tokens.get(i);
            int start = token.getOffset(), end = start + token.getText().length();
            if (start >= cursor && end <= to && token.getType() == Token.COMMENT) {
                if (!text.substring(cursor, start).isBlank()) return false;
                cursor = end;
            }
        }
        return text.substring(cursor, to).isBlank();
    }

    private int firstTokenAtOrAfter(int offset) {
        int low = 0, high = tokens.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (tokens.get(middle).getOffset() < offset) low = middle + 1;
            else high = middle;
        }
        return low;
    }
}
