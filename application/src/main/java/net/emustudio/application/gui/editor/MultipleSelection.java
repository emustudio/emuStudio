/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.TextMode;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;

/** Adds disjoint selections to the existing Swing editor, keeping one shared undo history. */
final class MultipleSelection {
    private static final int[] ARROW_KEYS = {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN};

    /** Routes selection gestures before Swing's single-caret mouse handler. */
    static final class Pane extends TextEditorPane {
        private MultipleSelection selections;

        Pane() {
            super(TextMode.INSERT);
            int shortcut = RTextArea.getDefaultModifier();
            // Mask the fold shortcuts inherited from RSyntaxTextArea.
            for (int key : new int[]{KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT, KeyEvent.VK_DIVIDE, KeyEvent.VK_MULTIPLY}) {
                getInputMap().put(KeyStroke.getKeyStroke(key, shortcut), "none");
            }
        }

        @Override protected void appendFoldingMenu(JPopupMenu popup, int index) {
            // Omit RSyntaxTextArea's built-in folding submenu.
        }

        @Override protected void processMouseEvent(MouseEvent e) {
            if (selections == null || !selections.mouse(e)) super.processMouseEvent(e);
        }
        @Override protected void processMouseMotionEvent(MouseEvent e) {
            if (selections == null || !selections.mouse(e)) super.processMouseMotionEvent(e);
        }
    }

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
    private boolean columnSelectionMode;
    private boolean draggingRectangle;
    private Point rectangleAnchor;
    private Point rectangleLead;

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
        ((Pane) pane).selections = this;
        int shortcut = RTextArea.getDefaultModifier();
        bind(KeyEvent.VK_C, shortcut, "copySelections", () -> copy(false));
        bind(KeyEvent.VK_X, shortcut, "cutSelections", () -> copy(true));
        bind(KeyEvent.VK_V, shortcut, "pasteSelections", this::paste);
        bind(KeyEvent.VK_ESCAPE, 0, "clearSelections", this::clear);
        for (int key : ARROW_KEYS) {
            bind(key, 0, "moveSelections" + key, () -> move(key));
            pane.getActionMap().put("extendRectangle" + key, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    extendRectangle(key);
                }
            });
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

    boolean toggleColumnSelectionMode() {
        columnSelectionMode = !columnSelectionMode;
        for (int key : ARROW_KEYS) {
            KeyStroke stroke = KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK);
            if (columnSelectionMode) pane.getInputMap().put(stroke, "extendRectangle" + key);
            else pane.getInputMap().remove(stroke);
        }
        if (columnSelectionMode && ranges.isEmpty() && pane.getSelectionStart() != pane.getSelectionEnd()) {
            rectangle(pane.getCaret().getMark(), pane.getCaretPosition());
        } else if (!columnSelectionMode) {
            draggingRectangle = false;
            clear();
        }
        return columnSelectionMode;
    }

    boolean mouse(MouseEvent e) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (!SwingUtilities.isLeftMouseButton(e)) return false;
                boolean columnDrag = e.isAltDown() ? e.isShiftDown()
                        : columnSelectionMode && !e.isControlDown() && !e.isMetaDown();
                if (!columnDrag && !e.isAltDown()) return false;
                pane.requestFocusInWindow();
                int offset = pane.viewToModel2D(e.getPoint());
                draggingRectangle = columnDrag;
                if (columnDrag) {
                    rectangle(offset, offset);
                } else {
                    List<Range> next = new ArrayList<>(ranges);
                    if (next.isEmpty()) next.add(new Range(pane.getSelectionStart(), pane.getSelectionEnd()));
                    next.add(new Range(offset, offset));
                    set(next);
                }
                return true;
            case MouseEvent.MOUSE_DRAGGED:
                if (draggingRectangle && rectangleAnchor != null) {
                    try {
                        rectangle(rectangleAnchor, columnPosition(pane.viewToModel2D(e.getPoint())));
                    } catch (BadLocationException ex) { clear(); }
                    return true;
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (draggingRectangle && SwingUtilities.isLeftMouseButton(e)) {
                    draggingRectangle = false;
                    return true;
                }
                break;
            default:
                break;
        }
        return e.isAltDown();
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
        rectangleAnchor = rectangleLead = null;
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
                int[] span = span(old.get(i), deleteDirection, originalText);
                int caret = span[0] + delta + replacements.get(i).length();
                next.add(new Range(caret, caret));
                delta += replacements.get(i).length() - (span[1] - span[0]);
            }
            clear();
            for (int i = old.size() - 1; i >= 0; i--) {
                int[] span = span(old.get(i), deleteDirection, originalText);
                ((AbstractDocument) pane.getDocument()).replace(span[0], span[1] - span[0], replacements.get(i), null);
            }
        } finally { pane.endAtomicEdit(); editing = false; }
        set(next);
    }

    /** An empty range deletes one code point in the given direction. */
    private static int[] span(Range r, int deleteDirection, String text) {
        int start = r.start, end = r.end;
        if (start == end && deleteDirection < 0 && start > 0) start = text.offsetByCodePoints(start, -1);
        if (start == end && deleteDirection > 0 && end < text.length()) end = text.offsetByCodePoints(end, 1);
        return new int[]{start, end};
    }

    void rectangle(int anchor, int dot) {
        try {
            rectangle(columnPosition(anchor), columnPosition(dot));
        } catch (BadLocationException e) { throw new IllegalArgumentException(e); }
    }

    private Point columnPosition(int offset) throws BadLocationException {
        int line = pane.getLineOfOffset(offset);
        int start = pane.getLineStartOffset(line);
        int column = TextEdits.spaces(pane.getText(start, offset - start), pane.getTabSize()).length();
        return new Point(column, line);
    }

    private int rectangle(Point anchor, Point lead) throws BadLocationException {
        int top = Math.min(anchor.y, lead.y), bottom = Math.max(anchor.y, lead.y);
        int left = Math.min(anchor.x, lead.x), right = Math.max(anchor.x, lead.x);
        int leadOffset = 0;
        List<Range> next = new ArrayList<>();
        for (int line = top; line <= bottom; line++) {
            int start = pane.getLineStartOffset(line);
            int end = Math.min(pane.getDocument().getLength(), pane.getLineEndOffset(line));
            if (end > start && pane.getText(end - 1, 1).equals("\n")) end--;
            String row = pane.getText(start, end - start);
            int width = TextEdits.spaces(row, pane.getTabSize()).length();
            int from = start + offsetForColumn(row, left), to = start + offsetForColumn(row, right);
            next.add(new Range(from, to, Math.max(0, left - width)));
            if (line == lead.y) leadOffset = lead.x == left ? from : to;
        }
        set(next);
        rectangleAnchor = anchor;
        rectangleLead = lead;
        return leadOffset;
    }

    private void extendRectangle(int key) {
        try {
            Point anchor = rectangleAnchor == null ? columnPosition(pane.getCaret().getMark()) : rectangleAnchor;
            Point lead = rectangleLead == null ? columnPosition(pane.getCaretPosition()) : new Point(rectangleLead);
            switch (key) {
                case KeyEvent.VK_LEFT: lead.x = Math.max(0, lead.x - 1); break;
                case KeyEvent.VK_RIGHT: lead.x++; break;
                case KeyEvent.VK_UP: lead.y = Math.max(0, lead.y - 1); break;
                case KeyEvent.VK_DOWN: lead.y = Math.min(pane.getLineCount() - 1, lead.y + 1); break;
                default: throw new IllegalArgumentException("Expected an arrow key");
            }
            Rectangle2D visible = pane.modelToView2D(rectangle(anchor, lead));
            if (visible != null) pane.scrollRectToVisible(visible.getBounds());
        } catch (BadLocationException e) { clear(); }
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
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    next.add(moveHorizontally(r, text, key == KeyEvent.VK_RIGHT));
                    continue;
                }
                int line = pane.getLineOfOffset(r.end), column = r.end - pane.getLineStartOffset(line);
                int nextLine = Math.max(0, Math.min(pane.getLineCount() - 1, line + (key == KeyEvent.VK_UP ? -1 : 1)));
                int caret = Math.min(pane.getLineEndOffset(nextLine) - (nextLine + 1 < pane.getLineCount() ? 1 : 0),
                        pane.getLineStartOffset(nextLine) + column);
                next.add(new Range(caret, caret));
            }
            set(next);
        } catch (BadLocationException e) { clear(); }
    }

    private Range moveHorizontally(Range range, String text, boolean right) {
        int caret = right ? range.end : range.start;
        if (range.start != range.end) return new Range(caret, caret);

        int adjacent = right ? caret : caret - 1;
        boolean hasCharacter = adjacent >= 0 && adjacent < text.length();
        if (columnSelectionMode) {
            if (!right && range.padding > 0) return new Range(caret, caret, range.padding - 1);
            if (!hasCharacter || text.charAt(adjacent) == '\n') {
                // Keep the caret on its row. Typing fills any columns beyond the line end.
                return new Range(caret, caret, right ? range.padding + 1 : 0);
            }
        }
        if (hasCharacter) caret = text.offsetByCodePoints(caret, right ? 1 : -1);
        return new Range(caret, caret);
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
