/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/** Inline search scoped to the active source view or a captured selection. */
public final class InlineSearchBar extends JPanel {
    final JTextField find = new JTextField(18), replace = new JTextField(14);
    final JCheckBox matchCase = new JCheckBox("Case"), word = new JCheckBox("Word"), regex = new JCheckBox("Regex"),
            preserve = new JCheckBox("Preserve case"), selection = new JCheckBox("Selection");
    private final JLabel count = new JLabel("0 matches");
    private final Supplier<REditor> active;
    private final Runnable preview;
    private TextEditorPane scopedPane;
    private Position scopeStart, scopeEnd;
    private boolean scopeStartsAtZero;
    private boolean moving;

    InlineSearchBar(Supplier<REditor> active, Runnable preview) {
        this.active = active; this.preview = preview;
        setLayout(new GridLayout(2, 1));
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        find.getAccessibleContext().setAccessibleName("Find text"); replace.getAccessibleContext().setAccessibleName("Replacement text");
        fields.add(new JLabel("Find")); fields.add(find); fields.add(new JLabel("Replace")); fields.add(replace);
        button(fields, "Previous", () -> next(false)); button(fields, "Next", () -> next(true));
        button(fields, "×", () -> { setVisible(false); active.get().grabFocus(); });
        for (JCheckBox option : new JCheckBox[]{matchCase, word, regex, preserve, selection}) options.add(option);
        button(options, "Replace", this::replaceCurrent); button(options, "Replace all", this::replaceAll);
        button(options, "Files…", preview); options.add(count);
        add(fields); add(options);
        find.addActionListener(e -> next(true)); replace.addActionListener(e -> replaceCurrent());
        DocumentListener listener = REditor.onChange(this::refresh);
        find.getDocument().addDocumentListener(listener); replace.getDocument().addDocumentListener(listener);
        for (JCheckBox option : new JCheckBox[]{matchCase, word, regex, preserve}) option.addActionListener(e -> refresh());
        selection.addActionListener(e -> {
            scopedPane = active.get().textPane();
            try {
                int start = scopedPane.getSelectionStart();
                scopeStartsAtZero = start == 0;
                // Anchor before the boundary so insertion at the boundary stays inside the scope.
                scopeStart = scopedPane.getDocument().createPosition(Math.max(0, start - 1));
                scopeEnd = scopedPane.getDocument().createPosition(scopedPane.getSelectionEnd());
            }
            catch (BadLocationException ex) { selection.setSelected(false); }
            refresh();
        });
        setVisible(false);
    }
    private void button(JPanel row, String label, Runnable runnable) {
        JButton button = new JButton(label); button.addActionListener(e -> {
            try { runnable.run(); }
            catch (IllegalArgumentException ex) { count.setText(ex.getMessage()); }
        }); row.add(button);
    }
    public void showSearch() {
        String selected = active.get().getSelectedText();
        if (selected != null && !selected.isEmpty()) find.setText(selected);
        setVisible(true); find.requestFocusInWindow(); find.selectAll(); refresh();
    }
    void observe(REditor editor) {
        editor.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));
        editor.textPane().addCaretListener(e -> { if (!moving) SwingUtilities.invokeLater(this::refresh); });
    }
    List<EditorSearch.Match> matches() {
        TextEditorPane pane = active.get().textPane();
        if (selection.isSelected() && scopedPane != pane) selection.setSelected(false);
        int end = selection.isSelected() && scopeEnd != null ? scopeEnd.getOffset() : pane.getDocument().getLength();
        int start = selection.isSelected() && scopeStart != null ? Math.min(end, scopeStart.getOffset() + (scopeStartsAtZero ? 0 : 1)) : 0;
        return EditorSearch.find(pane.getText(), find.getText(), replace.getText(), regex.isSelected(), matchCase.isSelected(),
                word.isSelected(), preserve.isSelected(), start, end);
    }
    void refresh() {
        if (!isVisible()) return;
        try {
            List<EditorSearch.Match> matches = matches(); TextEditorPane pane = active.get().textPane();
            int current = 0;
            for (int i = 0; i < matches.size(); i++) if (selected(pane, matches.get(i))) current = i + 1;
            count.setText(current + " of " + matches.size());
        } catch (IllegalArgumentException e) { count.setText("Invalid search: " + e.getMessage()); }
    }
    boolean next(boolean forward) {
        List<EditorSearch.Match> matches = matches(); if (matches.isEmpty()) { refresh(); return false; }
        TextEditorPane pane = active.get().textPane(); EditorSearch.Match chosen = forward ? matches.get(0) : matches.get(matches.size() - 1);
        if (forward) {
            for (EditorSearch.Match match : matches) {
                if (match.start >= pane.getSelectionEnd() && !selected(pane, match)) { chosen = match; break; }
            }
        } else {
            for (int i = matches.size() - 1; i >= 0; i--) {
                if (matches.get(i).end <= pane.getSelectionStart() && !selected(pane, matches.get(i))) { chosen = matches.get(i); break; }
            }
        }
        moving = true;
        try { pane.select(chosen.start, chosen.end); }
        finally { moving = false; }
        pane.requestFocusInWindow(); refresh();
        return true;
    }
    void replaceCurrent() {
        TextEditorPane pane = active.get().textPane();
        for (EditorSearch.Match match : matches()) {
            if (selected(pane, match)) {
                active.get().commands().selections().clear(); TextEdits.replace(pane, match.start, match.end, match.after); next(true); return;
            }
        }
        next(true);
    }
    private static boolean selected(TextEditorPane pane, EditorSearch.Match match) {
        return match.start == pane.getSelectionStart() && match.end == pane.getSelectionEnd();
    }
    void replaceAll() {
        TextEditorPane pane = active.get().textPane(); List<EditorSearch.Match> matches = matches();
        if (matches.isEmpty()) return;
        active.get().commands().selections().clear();
        EditorSearch.apply(pane, matches);
        count.setText(matches.size() + " replaced");
    }
}
