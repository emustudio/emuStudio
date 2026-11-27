/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.framework.EmuStudioGui;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;


/** Inline search scoped to the active source view or a captured selection. */
public final class InlineSearchBar extends JPanel {
    private static final String ICONS = "/net/emustudio/application/gui/icons/";
    private static final String TOOLBAR_BUTTON_SIZE = "w 22px!, h 22px!";

    final JTextField find, replace;
    final JToggleButton matchCase = option("Aa", "Match case"), word = option("W", "Whole word"), regex = option(".*", "Regex"),
            preserve = option("Aa", "Preserve case"), selection = option("[]", "Search only the captured selection in this pane");
    private final JLabel count = new JLabel("0 results");
    private final JToggleButton replacementToggle;
    private final List<Component> replacementControls;
    private final Supplier<REditor> active;
    private TextEditorPane scopedPane;
    private Position scopeStart, scopeEnd;
    private boolean scopeStartsAtZero;
    private boolean moving;

    InlineSearchBar(Supplier<REditor> active) {
        super(new BorderLayout());
        this.active = active;
        count.setFont(count.getFont().deriveFont(count.getFont().getSize2D() * 0.85f));
        EmuStudioGui gui = new EmuStudioGui();
        JToolBar toolbar = gui.toolBar();
        toolbar.setLayout(new MigLayout("insets 2 4, hidemode 3, novisualpadding",
                "[pref!][80:360:520,growprio 110,grow,fill][pref!][pref!][pref!][grow,right]", "[]2[]2[]"));
        toolbar.getAccessibleContext().setAccessibleName("Find and replace");
        add(toolbar, BorderLayout.CENTER);

        find = searchField("Find", "Find text", 18);
        replace = searchField("Replace with", "Replacement text", 14);
        replacementToggle = gui.toolbarToggleButton(e -> setReplacementVisible(
                ((JToggleButton) e.getSource()).isSelected()), ICONS + "replace.png", "Show replacement controls");
        replacementToggle.setIcon(new ArrowIcon(true, true));
        JButton previous = button(gui, ICONS + "move-up.png", "Previous match", () -> next(false));
        previous.setIcon(new ArrowIcon(false, true));
        JButton next = button(gui, ICONS + "move-down.png", "Next match", () -> next(true));
        next.setIcon(new ArrowIcon(false, false));
        JButton close = button(gui, ICONS + "remove.png", "Close search", this::hideSearch);
        close.setIcon(new CloseIcon());

        toolbar.add(replacementToggle, TOOLBAR_BUTTON_SIZE);
        toolbar.add(find);
        toolbar.add(matchCase, TOOLBAR_BUTTON_SIZE + ", split 4");
        toolbar.add(word, TOOLBAR_BUTTON_SIZE);
        toolbar.add(regex, TOOLBAR_BUTTON_SIZE);
        toolbar.add(selection, TOOLBAR_BUTTON_SIZE);
        toolbar.add(new JToolBar.Separator(), "growy");
        toolbar.add(previous, TOOLBAR_BUTTON_SIZE + ", split 2");
        toolbar.add(next, TOOLBAR_BUTTON_SIZE);
        toolbar.add(close, TOOLBAR_BUTTON_SIZE + ", wrap");
        toolbar.add(count, "skip 1, span, wmin 0, wrap");

        JPanel replacements = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        replacements.setOpaque(false);
        JButton replaceOne = new JButton("Replace");
        replaceOne.addActionListener(e -> runReplacement(this::replaceCurrent));
        JButton replaceAll = new JButton("Replace All");
        replaceAll.addActionListener(e -> runReplacement(this::replaceAll));
        replacements.add(replaceOne);
        replacements.add(replaceAll);
        toolbar.add(replace, "skip 1");
        toolbar.add(preserve, TOOLBAR_BUTTON_SIZE + ", span, split 2");
        toolbar.add(replacements);
        replacementControls = List.of(replace, preserve, replacements);
        setReplacementVisible(false);
        find.addActionListener(e -> next(true)); replace.addActionListener(e -> replaceCurrent());
        DocumentListener listener = REditor.onChange(this::refresh);
        find.getDocument().addDocumentListener(listener); replace.getDocument().addDocumentListener(listener);
        for (JToggleButton option : new JToggleButton[]{matchCase, word, regex, preserve}) option.addActionListener(e -> refresh());
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

    private static JTextField searchField(String prompt, String accessibleName, int columns) {
        JTextField field = new JTextField(columns);
        field.putClientProperty("JTextField.placeholderText", prompt);
        field.putClientProperty("FlatLaf.style", "placeholderForeground: $TextField.foreground");
        field.getAccessibleContext().setAccessibleName(accessibleName);
        return field;
    }

    private static JToggleButton option(String text, String label) {
        JToggleButton button = new JToggleButton(text);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        return button;
    }
    private JButton button(EmuStudioGui gui, String icon, String tooltip, Runnable runnable) {
        JButton button = gui.toolbarButton(e -> {
            try { runnable.run(); }
            catch (IllegalArgumentException ex) { count.setText(ex.getMessage()); }
        }, icon, tooltip);
        return button;
    }
    private static final class ArrowIcon implements Icon {
        private final boolean disclosure, up;
        ArrowIcon(boolean disclosure, boolean up) { this.disclosure = disclosure; this.up = up; }
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(c.getForeground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (disclosure) {
                if (((AbstractButton) c).isSelected()) { g.drawLine(x+4,y+6,x+8,y+10); g.drawLine(x+8,y+10,x+12,y+6); }
                else { g.drawLine(x+6,y+4,x+10,y+8); g.drawLine(x+10,y+8,x+6,y+12); }
            } else {
                int tip = up ? 4 : 12, base = 8;
                g.drawLine(x+8,y+4,x+8,y+12);
                g.drawLine(x+8,y+tip,x+4,y+base); g.drawLine(x+8,y+tip,x+12,y+base);
            }
            g.dispose();
        }
    }
    private static final class CloseIcon implements Icon {
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(c.getForeground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + 4, y + 4, x + 12, y + 12);
            g.drawLine(x + 12, y + 4, x + 4, y + 12);
            g.dispose();
        }
    }
    private void runReplacement(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException ex) { count.setText(ex.getMessage()); }
    }
    private void setReplacementVisible(boolean visible) {
        replacementToggle.setSelected(visible);
        replacementToggle.setToolTipText(visible ? "Hide replacement controls" : "Show replacement controls");
        replacementToggle.getAccessibleContext().setAccessibleName(replacementToggle.getToolTipText());
        for (Component control : replacementControls) control.setVisible(visible);
        revalidate();
        repaint();
    }
    public void showFind() {
        showSearch(false);
    }
    public void showReplace() {
        showSearch(true);
    }

    public void hideSearch() {
        setVisible(false);
        active.get().grabFocus();
    }

    private void showSearch(boolean replacements) {
        setReplacementVisible(replacements);
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
            List<EditorSearch.Match> matches = matches();
            count.setText(matches.size() + (matches.size() == 1 ? " result" : " results"));
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
