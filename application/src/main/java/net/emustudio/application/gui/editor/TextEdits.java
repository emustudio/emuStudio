/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import java.util.*;
import java.util.function.UnaryOperator;

/** Text-only transformations, independent of compiler and filesystem services. */
final class TextEdits {
    private TextEdits() { }

    static void lines(RSyntaxTextArea pane, UnaryOperator<String> operation, boolean wholeFile) {
        try {
            int start = wholeFile && pane.getSelectionStart() == pane.getSelectionEnd() ? 0
                    : pane.getLineStartOffset(pane.getLineOfOffset(pane.getSelectionStart()));
            int selectedEnd = pane.getSelectionEnd();
            int end = wholeFile && pane.getSelectionStart() == selectedEnd ? pane.getDocument().getLength()
                    : pane.getLineEndOffset(pane.getLineOfOffset(Math.max(start, selectedEnd - 1)));
            end = Math.min(end, pane.getDocument().getLength());
            replace(pane, start, end, operation.apply(pane.getText(start, end - start)));
        } catch (BadLocationException e) {
            throw new IllegalArgumentException("Invalid selection", e);
        }
    }

    static void replace(RSyntaxTextArea pane, int start, int end, String text) {
        pane.beginAtomicEdit();
        try {
            pane.select(start, end);
            pane.replaceSelection(text);
            pane.select(start, start + text.length());
        } finally {
            pane.endAtomicEdit();
        }
    }

    static String reorder(String text, String operation) {
        boolean newline = text.endsWith("\n");
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (newline) lines.remove(lines.size() - 1);
        switch (operation) {
            case "Sort lines": Collections.sort(lines); break;
            case "Reverse lines": Collections.reverse(lines); break;
            case "Remove duplicate lines": lines = new ArrayList<>(new LinkedHashSet<>(lines)); break;
            default: throw new IllegalArgumentException(operation);
        }
        return String.join("\n", lines) + (newline ? "\n" : "");
    }

    static String spaces(String text, int tabSize) {
        StringBuilder result = new StringBuilder();
        int column = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '\t') {
                int count = tabSize - column % tabSize;
                result.append(" ".repeat(count));
                column += count;
            } else {
                result.append(ch);
                column = ch == '\n' ? 0 : column + 1;
            }
        }
        return result.toString();
    }

    static String tabs(String text, int tabSize) {
        return text.replaceAll("(?m)^ +", "").equals(text) ? text : mapLines(text, line -> {
            int count = 0;
            while (count < line.length() && line.charAt(count) == ' ') count++;
            return "\t".repeat(count / tabSize) + " ".repeat(count % tabSize) + line.substring(count);
        });
    }

    static String mapLines(String text, UnaryOperator<String> operation) {
        return Arrays.stream(text.split("\n", -1)).map(operation).collect(java.util.stream.Collectors.joining("\n"));
    }
}
