/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import java.util.function.UnaryOperator;

/**
 * Text-only transformations, independent of compiler and filesystem services.
 */
final class TextEdits {
    private TextEdits() {
    }

    static void lines(RSyntaxTextArea pane, UnaryOperator<String> operation, boolean wholeFile) {
        try {
            int start = wholeFile && pane.getSelectionStart() == pane.getSelectionEnd() ? 0 : pane.getLineStartOffset(pane.getLineOfOffset(pane.getSelectionStart()));
            int selectedEnd = pane.getSelectionEnd();
            int end = wholeFile && pane.getSelectionStart() == selectedEnd ? pane.getDocument().getLength() : pane.getLineEndOffset(pane.getLineOfOffset(Math.max(start, selectedEnd - 1)));
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
}
