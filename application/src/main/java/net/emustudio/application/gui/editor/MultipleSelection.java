/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/** Adds disjoint selections to the existing Swing editor, keeping one shared undo history. */
final class MultipleSelection {
    static final class Range {
        final int start, end, padding;
        Range(int start, int end) { this(start, end, 0); }
        Range(int start, int end, int padding) {
            this.start = Math.min(start, end); this.end = Math.max(start, end); this.padding = padding;
        }
    }
    private final TextEditorPane pane;
    private final List<Range> ranges = new ArrayList<>();
    private final List<Object> highlights = new ArrayList<>();
    private boolean editing;
    private int rectangleAnchor = -1;
    private final MouseAdapter mouse;

    void installFilter() {
        ((AbstractDocument) pane.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributes) throws BadLocationException { replace(fb, offset, 0, text, attributes); }
            @Override public void remove(FilterBypass fb, int offset, int length) throws BadLocationException { replace(fb, offset, length, "", null); }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                MultipleSelection current = (MultipleSelection) pane.getDocument().getProperty("activeSelections");
                if (current == null) current = MultipleSelection.this;
                current.filter(fb, offset, length, text, attributes);
            }
        });
    }

    private void filter(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
        if (editing || ranges.size() < 2) { fb.replace(offset, length, text, attributes); return; }
        int caret = pane.getCaretPosition();
        Range primary = ranges.stream().filter(r -> r.start == pane.getSelectionStart()
                && r.end == pane.getSelectionEnd()).findFirst().orElse(null);
        boolean backspace = primary != null && offset + length == primary.start
                && length > 0 && (text == null || text.isEmpty());
        if (primary == null || !backspace && (offset < primary.start || offset > primary.end)) {
            clear(); fb.replace(offset, length, text, attributes); return;
        }
        int direction = primary.start == primary.end && length > 0 && (text == null || text.isEmpty())
                ? (offset < caret ? -1 : 1) : 0;
        edit(Collections.nCopies(ranges.size(), text == null ? "" : text), direction);
    }

    MultipleSelection(TextEditorPane pane) {
        this.pane = pane;
        installFilter();
        pane.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { pane.getDocument().putProperty("activeSelections", MultipleSelection.this); }
        });
        pane.addCaretListener(e -> { if (!editing && !ranges.isEmpty()) clear(); });
        mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                pane.requestFocusInWindow();
                int offset = pane.viewToModel2D(e.getPoint());
                if (e.isAltDown() && e.isShiftDown()) {
                    rectangleAnchor = offset;
                    set(List.of(new Range(offset, offset)));
                } else if (e.isAltDown()) {
                    List<Range> next = new ArrayList<>(ranges);
                    if (next.isEmpty()) next.add(new Range(pane.getSelectionStart(), pane.getSelectionEnd()));
                    next.add(new Range(offset, offset)); set(next);
                } else { rectangleAnchor = -1; clear(); }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (rectangleAnchor >= 0 && e.isAltDown() && e.isShiftDown()) {
                    rectangle(rectangleAnchor, pane.viewToModel2D(e.getPoint()));
                }
            }
            @Override public void mouseReleased(MouseEvent e) { rectangleAnchor = -1; }
        };
        ((EditingTextPane) pane).selections = this;
        bind(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, "copySelections", () -> copy(false));
        bind(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK, "cutSelections", () -> copy(true));
        bind(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, "pasteSelections", this::paste);
        bind(KeyEvent.VK_ESCAPE, 0, "clearSelections", this::clear);
        for (int key : new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN}) {
            final int direction = key;
            bind(key, 0, "moveSelections" + key, () -> move(direction));
        }
    }

    private void bind(int key, int modifiers, String name, Runnable action) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, modifiers);
        Object originalKey = pane.getInputMap().get(stroke);
        Action original = originalKey == null ? null : pane.getActionMap().get(originalKey);
        pane.getInputMap().put(stroke, name);
        pane.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (ranges.size() > 1 || key == KeyEvent.VK_ESCAPE) action.run();
                else if (original != null) original.actionPerformed(event);
            }
        });
    }

    List<Range> ranges() { return List.copyOf(ranges); }

    boolean mouse(MouseEvent e) {
        if (!e.isAltDown() && rectangleAnchor < 0) return false;
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED: mouse.mousePressed(e); break;
            case MouseEvent.MOUSE_DRAGGED: mouse.mouseDragged(e); break;
            case MouseEvent.MOUSE_RELEASED: mouse.mouseReleased(e); break;
            default: break;
        }
        return true;
    }

    void set(List<Range> selections) {
        pane.getDocument().putProperty("activeSelections", this);
        editing = true;
        try {
            clear();
            List<Range> sorted = new ArrayList<>(selections);
            sorted.sort(Comparator.comparingInt(r -> r.start));
            for (Range r : sorted) {
                if (r.start < 0 || r.end > pane.getDocument().getLength()) continue;
                if (!ranges.isEmpty()) {
                    Range last = ranges.get(ranges.size() - 1);
                    if (r.start < last.end || r.start == last.start && r.end == last.end) {
                        ranges.set(ranges.size() - 1, new Range(last.start, Math.max(last.end, r.end))); continue;
                    }
                }
                ranges.add(r);
            }
            if (!ranges.isEmpty()) pane.select(ranges.get(0).start, ranges.get(0).end);
            paint();
        } finally { editing = false; }
    }

    void clear() {
        highlights.forEach(pane.getHighlighter()::removeHighlight);
        highlights.clear(); ranges.clear();
    }

    private void paint() {
        try {
            for (Range range : ranges) {
                Highlighter.HighlightPainter painter = range.start == range.end ? (g, p0, p1, bounds, component) -> {
                    try {
                        Rectangle rectangle = component.modelToView2D(p0).getBounds();
                        g.setColor(component.getCaretColor());
                        g.fillRect(rectangle.x, rectangle.y, 2, rectangle.height);
                    } catch (BadLocationException ignored) { }
                } : new DefaultHighlighter.DefaultHighlightPainter(pane.getSelectionColor());
                highlights.add(pane.getHighlighter().addHighlight(range.start, range.end, painter));
            }
        } catch (BadLocationException e) { clear(); }
    }

    void edit(List<String> replacements, int deleteDirection) throws BadLocationException {
        String originalText = pane.getText();
        List<Range> old = List.copyOf(ranges), next = new ArrayList<>();
        replacements = new ArrayList<>(replacements);
        for (int i = 0; i < old.size(); i++) {
            if (!replacements.get(i).isEmpty() && old.get(i).padding > 0) replacements.set(i, " ".repeat(old.get(i).padding) + replacements.get(i));
        }
        editing = true; pane.beginAtomicEdit();
        try {
            int delta = 0;
            for (int i = 0; i < old.size(); i++) {
                Range r = old.get(i);
                int start = r.start, end = r.end;
                if (start == end && deleteDirection < 0 && start > 0) start = originalText.offsetByCodePoints(start, -1);
                if (start == end && deleteDirection > 0 && end < originalText.length()) end = originalText.offsetByCodePoints(end, 1);
                int caret = start + delta + replacements.get(i).length();
                next.add(new Range(caret, caret));
                delta += replacements.get(i).length() - (end - start);
            }
            clear();
            for (int i = old.size() - 1; i >= 0; i--) {
                Range r = old.get(i);
                int start = r.start, end = r.end;
                if (start == end && deleteDirection < 0 && start > 0) start = originalText.offsetByCodePoints(start, -1);
                if (start == end && deleteDirection > 0 && end < originalText.length()) end = originalText.offsetByCodePoints(end, 1);
                ((AbstractDocument) pane.getDocument()).replace(start, end - start, replacements.get(i), null);
            }
        } finally { pane.endAtomicEdit(); editing = false; }
        set(next);
    }

    void rectangle(int anchor, int dot) {
        try {
            int firstLine = pane.getLineOfOffset(anchor), lastLine = pane.getLineOfOffset(dot);
            int firstColumn = TextEdits.spaces(pane.getText(pane.getLineStartOffset(firstLine), anchor - pane.getLineStartOffset(firstLine)), pane.getTabSize()).length();
            int lastColumn = TextEdits.spaces(pane.getText(pane.getLineStartOffset(lastLine), dot - pane.getLineStartOffset(lastLine)), pane.getTabSize()).length();
            List<Range> next = new ArrayList<>();
            for (int line = Math.min(firstLine, lastLine); line <= Math.max(firstLine, lastLine); line++) {
                int start = pane.getLineStartOffset(line);
                int end = Math.min(pane.getDocument().getLength(), pane.getLineEndOffset(line));
                if (end > start && pane.getText(end - 1, 1).equals("\n")) end--;
                String row = pane.getText(start, end - start);
                int width = TextEdits.spaces(row, pane.getTabSize()).length();
                int left = Math.min(firstColumn, lastColumn), right = Math.max(firstColumn, lastColumn);
                next.add(new Range(start + offsetForColumn(row, left), start + offsetForColumn(row, right), Math.max(0, left - width)));
            }
            set(next);
        } catch (BadLocationException e) { throw new IllegalArgumentException(e); }
    }

    private int offsetForColumn(String row, int target) {
        int column = 0, offset = 0;
        while (offset < row.length() && column < target) {
            column += row.charAt(offset) == '\t' ? pane.getTabSize() - column % pane.getTabSize() : 1;
            offset++;
        }
        return offset;
    }

    private void move(int key) {
        try {
            String text = pane.getText();
            List<Range> next = new ArrayList<>();
            for (Range r : ranges) {
                int caret = r.end;
                if (key == KeyEvent.VK_LEFT) caret = r.start == r.end && r.start > 0 ? text.offsetByCodePoints(r.start, -1) : r.start;
                else if (key == KeyEvent.VK_RIGHT) caret = r.start == r.end && r.end < text.length() ? text.offsetByCodePoints(r.end, 1) : r.end;
                else {
                    int line = pane.getLineOfOffset(caret), column = caret - pane.getLineStartOffset(line);
                    int nextLine = Math.max(0, Math.min(pane.getLineCount() - 1, line + (key == KeyEvent.VK_UP ? -1 : 1)));
                    caret = Math.min(pane.getLineEndOffset(nextLine) - (nextLine + 1 < pane.getLineCount() ? 1 : 0),
                            pane.getLineStartOffset(nextLine) + column);
                }
                next.add(new Range(caret, caret));
            }
            set(next);
        } catch (BadLocationException e) { clear(); }
    }

    private void copy(boolean cut) {
        try {
            List<String> parts = new ArrayList<>();
            for (Range r : ranges) parts.add(pane.getText(r.start, r.end - r.start));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.join("\n", parts)), null);
            if (cut) edit(Collections.nCopies(ranges.size(), ""), 0);
        } catch (BadLocationException | IllegalStateException e) { UIManager.getLookAndFeel().provideErrorFeedback(pane); }
    }

    private void paste() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            String[] rows = text.replace("\r\n", "\n").split("\n", -1);
            edit(rows.length == ranges.size() ? Arrays.asList(rows) : Collections.nCopies(ranges.size(), text), 0);
        } catch (Exception e) { UIManager.getLookAndFeel().provideErrorFeedback(pane); }
    }
}
