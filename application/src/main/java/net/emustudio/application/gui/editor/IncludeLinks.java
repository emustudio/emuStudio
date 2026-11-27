/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugin.compiler.Token;
import org.fife.ui.rsyntaxtextarea.SquiggleUnderlineHighlightPainter;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IncludeLinks extends MouseAdapter {
    private static final Pattern QUOTED_PATH = Pattern.compile("([\"'])([^\"']+)\\1");

    private final TextEditorPane pane;
    private final Compiler compiler;
    private final Supplier<Optional<Path>> sourceFile;
    private final Consumer<Path> openFile;
    private final Highlighter.HighlightPainter underline;
    private final KeyEventDispatcher controlKey = this::controlChanged;
    private final PropertyChangeListener activeWindow = event -> clearHover();
    private NavigableMap<Integer, Reference> references;
    private Point pointer;
    private KeyboardFocusManager focusManager;
    private Reference hovered;
    private Object highlight;

    IncludeLinks(TextEditorPane pane, Compiler compiler, Supplier<Optional<Path>> sourceFile, Consumer<Path> openFile) {
        this.pane = pane;
        this.compiler = compiler;
        this.sourceFile = sourceFile;
        this.openFile = openFile;
        underline = new SquiggleUnderlineHighlightPainter(pane.getHyperlinkForeground()) {
            @Override protected void paintSquiggle(Graphics graphics, Rectangle range) {
                graphics.setColor(pane.getHyperlinkForeground());
                int y = range.y + range.height - 1;
                graphics.drawLine(range.x, y, range.x + range.width - 1, y);
            }
        };

        pane.addMouseListener(this);
        pane.addMouseMotionListener(this);
        pane.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !pane.isShowing()) stopHover();
        });
        DocumentListener changes = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { invalidateReferences(); }
            @Override public void removeUpdate(DocumentEvent event) { invalidateReferences(); }
            @Override public void changedUpdate(DocumentEvent event) { invalidateReferences(); }
        };
        pane.getDocument().addDocumentListener(changes);
        pane.addPropertyChangeListener("document", event -> {
            ((Document) event.getOldValue()).removeDocumentListener(changes);
            ((Document) event.getNewValue()).addDocumentListener(changes);
            invalidateReferences();
        });
    }

    @Override public void mouseEntered(MouseEvent event) {
        if (focusManager == null) {
            focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            focusManager.addKeyEventDispatcher(controlKey);
            focusManager.addPropertyChangeListener("activeWindow", activeWindow);
        }
        mouseMoved(event);
    }

    @Override public void mouseExited(MouseEvent event) { stopHover(); }

    @Override public void mouseDragged(MouseEvent event) {
        pointer = null;
        clearHover();
    }

    @Override public void mouseMoved(MouseEvent event) {
        pointer = event.getPoint();
        updateHover(event.isControlDown());
    }

    @Override public void mouseClicked(MouseEvent event) {
        if (SwingUtilities.isLeftMouseButton(event) && event.isControlDown()) {
            openAt(offsetAt(event.getPoint()));
            event.consume();
        }
    }

    void openAt(int offset) {
        resolve(referenceAt(offset)).ifPresent(path -> {
            if (Files.isRegularFile(path)) {
                clearHover();
                pane.setToolTipText(null);
                openFile.accept(path);
            } else {
                pane.setToolTipText("File not found: " + path);
            }
        });
    }

    private boolean controlChanged(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
            updateHover(event.getID() == KeyEvent.KEY_PRESSED || event.isControlDown());
        }
        return false;
    }

    private void updateHover(boolean controlDown) {
        Reference target = controlDown && pointer != null ? referenceAt(offsetAt(pointer)) : null;
        if (target != hovered) {
            clearHover();
            if (resolve(target).filter(Files::isRegularFile).isPresent()) {
                try {
                    highlight = pane.getHighlighter().addHighlight(target.start, target.end, underline);
                    hovered = target;
                } catch (BadLocationException ignored) {
                    // The range disappeared with an editor document replacement.
                }
            }
        }
        if (hovered != null) pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private int offsetAt(Point point) {
        int offset = pane.viewToModel2D(point);
        if (offset < 0) return -1;
        try {
            Rectangle2D caret = pane.modelToView2D(offset);
            if (caret == null || point.y < caret.getY() || point.y >= caret.getMaxY()) return -1;
            // The view returns the nearest insertion point. Use the character under the pointer.
            return point.x < caret.getX() ? offset - 1 : offset;
        } catch (BadLocationException ignored) {
            return -1;
        }
    }

    private void clearHover() {
        if (highlight == null) return;
        pane.getHighlighter().removeHighlight(highlight);
        highlight = null;
        hovered = null;
        pane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    private void stopHover() {
        pointer = null;
        clearHover();
        if (focusManager != null) {
            focusManager.removeKeyEventDispatcher(controlKey);
            focusManager.removePropertyChangeListener("activeWindow", activeWindow);
            focusManager = null;
        }
    }

    private void invalidateReferences() {
        references = null;
        clearHover();
    }

    private Reference referenceAt(int offset) {
        if (offset < 0 || sourceFile.get().isEmpty()) return null;
        // Keep mouse motion off the lexer until the document changes.
        if (references == null) references = readReferences();
        Map.Entry<Integer, Reference> entry = references.floorEntry(offset);
        return entry != null && offset < entry.getValue().hitEnd ? entry.getValue() : null;
    }

    private NavigableMap<Integer, Reference> readReferences() {
        NavigableMap<Integer, Reference> result = new TreeMap<>();
        if (compiler == null) {
            Matcher matcher = QUOTED_PATH.matcher(pane.getText());
            while (matcher.find()) {
                result.put(matcher.start(2), new Reference(matcher.start(2), matcher.end(2), matcher.end(2), matcher.group(2)));
            }
        } else {
            LexicalAnalyzer lexer = compiler.createLexer();
            char[] source = pane.getText().toCharArray();
            lexer.reset(source, 0, source.length);
            while (lexer.hasNext()) {
                Token token = lexer.next();
                String filename = token.getReferencedFile();
                if (filename == null || filename.isEmpty()) continue;
                String text = token.getText();
                int start = token.getOffset();
                int end = start + text.length();
                if (text.length() >= 2 && (text.charAt(0) == '\'' || text.charAt(0) == '"')
                        && text.charAt(0) == text.charAt(text.length() - 1)) {
                    start++;
                    end--;
                }
                if (start < end) {
                    result.put(token.getOffset(), new Reference(start, end, token.getOffset() + text.length(),
                            filename.replace('\\', File.separatorChar).replace('/', File.separatorChar)));
                }
            }
        }
        return result;
    }

    private Optional<Path> resolve(Reference reference) {
        if (reference == null) return Optional.empty();
        try {
            return sourceFile.get().map(Path::getParent).map(parent -> parent.resolve(reference.filename).normalize());
        } catch (InvalidPathException ignored) {
            return Optional.empty();
        }
    }

    private static final class Reference {
        private final int start;
        private final int end;
        private final int hitEnd;
        private final String filename;

        private Reference(int start, int end, int hitEnd, String filename) {
            this.start = start;
            this.end = end;
            this.hitEnd = hitEnd;
            this.filename = filename;
        }
    }
}
