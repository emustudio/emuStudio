/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.*;
import javax.swing.text.BadLocationException;
import java.util.*;
import java.util.function.Supplier;

/** Generic compiler block tokens, multiline comments, indentation, and explicit editor regions. */
final class SourceFoldParser implements FoldParser {
    @Override public List<Fold> getFolds(RSyntaxTextArea pane) {
        List<int[]> spans = new ArrayList<>();
        String text = pane.getText();
        @SuppressWarnings("unchecked")
        Supplier<LexicalAnalyzer> supplier = (Supplier<LexicalAnalyzer>) pane.getClientProperty("sourceLexer");
        if (supplier != null) {
            try {
            LexicalAnalyzer lexer = supplier.get();
            char[] chars = text.toCharArray(); lexer.reset(chars, 0, chars.length);
            Deque<Integer> blocks = new ArrayDeque<>();
            Deque<Integer> regions = new ArrayDeque<>();
            while (lexer.hasNext()) {
                Token token = lexer.next();
                if (token.getOffset() < 0 || token.getOffset() + token.getText().length() > text.length()) continue;
                if (token.getType() == Token.BLOCK_START) blocks.push(lineStart(text, token.getOffset()));
                else if (token.getType() == Token.BLOCK_END && !blocks.isEmpty()) {
                    spans.add(new int[]{blocks.pop(), token.getOffset() + token.getText().length()});
                } else if (token.getType() == Token.COMMENT) {
                    if (token.getText().contains("\n")) spans.add(new int[]{token.getOffset(), token.getOffset() + token.getText().length() - 1});
                    String marker = token.getText().trim().toLowerCase(Locale.ROOT);
                    if (marker.matches(".*\\bregion(?:\\s+.*)?")) regions.push(lineStart(text, token.getOffset()));
                    else if (marker.matches(".*\\bendregion")) {
                        if (!regions.isEmpty()) spans.add(new int[]{regions.pop(), token.getOffset() + token.getText().length() - 1});
                    }
                }
            }
            } catch (RuntimeException e) {
                // A plugin may reject incomplete source while typing. Indentation still works.
                spans.clear();
            }
        }
        String[] lines = text.split("\n", -1);
        int[] offsets = new int[lines.length], indents = new int[lines.length];
        Deque<Integer> plainRegions = new ArrayDeque<>();
        for (int line = 0, offset = 0; line < lines.length; line++) {
            offsets[line] = offset; offset += lines[line].length() + 1;
            String value = lines[line].stripLeading();
            indents[line] = TextEdits.spaces(lines[line].substring(0, lines[line].length() - value.length()), pane.getTabSize()).length();
            // Explicit editor markers also work in plain text without a lexer.
            if (supplier == null && value.startsWith("#region")) plainRegions.push(line);
            else if (supplier == null && value.equals("#endregion") && !plainRegions.isEmpty()) spans.add(new int[]{offsets[plainRegions.pop()], offset - 1});
        }
        for (int line = 0; line + 1 < lines.length; line++) {
            if (lines[line].isBlank()) continue;
            int next = line + 1;
            while (next < lines.length && lines[next].isBlank()) next++;
            if (next >= lines.length || indents[next] <= indents[line]) continue;
            int end = next;
            while (end + 1 < lines.length && (lines[end + 1].isBlank() || indents[end + 1] > indents[line])) end++;
            if (end > line) spans.add(new int[]{offsets[line], Math.min(text.length(), offsets[end] + lines[end].length())});
        }
        spans.sort(Comparator.<int[]>comparingInt(span -> span[0]).thenComparing((a, b) -> Integer.compare(b[1], a[1])));
        List<Fold> folds = new ArrayList<>(); Deque<Fold> parents = new ArrayDeque<>();
        try {
            for (int[] span : spans) {
                if (span[0] < 0 || span[1] >= text.length()) span[1] = Math.max(span[0], text.length() - 1);
                if (pane.getLineOfOffset(span[0]) >= pane.getLineOfOffset(span[1])) continue;
                while (!parents.isEmpty() && span[0] >= parents.peek().getEndOffset()) parents.pop();
                if (!parents.isEmpty() && (span[1] > parents.peek().getEndOffset()
                        || pane.getLineOfOffset(span[0]) == parents.peek().getStartLine())) continue;
                Fold fold = parents.isEmpty() ? new Fold(FoldType.CODE, pane, span[0])
                        : parents.peek().createChild(FoldType.CODE, span[0]);
                fold.setEndOffset(span[1]);
                if (parents.isEmpty()) folds.add(fold);
                parents.push(fold);
            }
        } catch (BadLocationException e) { return List.of(); }
        return folds;
    }
    private static int lineStart(String text, int offset) { return text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1; }
}
