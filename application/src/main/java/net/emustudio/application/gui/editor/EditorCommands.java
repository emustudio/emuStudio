/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugin.compiler.Compiler;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Local editing commands. Knows generic token categories, never compiler keywords.
 */
final class EditorCommands {
    private static final String COLUMN_SELECTION_MODE = "Column Selection Mode";

    private final TextEditorPane pane;
    private final RTextScrollPane scroll;
    private final Compiler compiler;
    private final MultipleSelection selections;
    private EditorState state = new EditorState(null);
    private final Map<String, Action> actions = new LinkedHashMap<>();

    EditorCommands(TextEditorPane pane, RTextScrollPane scroll, Compiler compiler) {
        this.pane = pane;
        this.scroll = scroll;
        this.compiler = compiler;
        selections = new MultipleSelection(pane);
        int shortcut = RTextArea.getDefaultModifier();
        boolean mac = RTextArea.isOSX();
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        int shift = InputEvent.SHIFT_DOWN_MASK;
        int alt = InputEvent.ALT_DOWN_MASK;

        InputMap input = pane.getInputMap();
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | shift), RTextAreaEditorKit.rtaRedoAction);
        input.put(KeyStroke.getKeyStroke(mac ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_Y, shortcut), RTextAreaEditorKit.rtaDeleteLineAction);
        if (mac) {
            input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, alt | shift), RTextAreaEditorKit.rtaLineUpAction);
            input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, alt | shift), RTextAreaEditorKit.rtaLineDownAction);
        }

        add("Duplicate line / selection", KeyEvent.VK_D, shortcut, this::duplicate);
        for (String name : List.of("Sort lines", "Reverse lines", "Remove duplicate lines")) {
            add(name, 0, 0, () -> lines(text -> TextEdits.reorder(text, name), true));
        }
        add("Uppercase", 0, 0, () -> changeCase(RTextAreaEditorKit.rtaUpperSelectionCaseAction));
        add("Lowercase", 0, 0, () -> changeCase(RTextAreaEditorKit.rtaLowerSelectionCaseAction));
        add("Invert case", 0, 0, () -> changeCase(RTextAreaEditorKit.rtaInvertSelectionCaseAction));
        add("Trim trailing whitespace", 0, 0, () -> lines(text -> text.replaceAll("(?m)[ \\t]+$", ""), true));
        add("Tabs to spaces", 0, 0, () -> lines(text -> TextEdits.spaces(text, pane.getTabSize()), true));
        add("Leading spaces to tabs", 0, 0, () -> lines(text -> TextEdits.tabs(text, pane.getTabSize()), true));
        add("Reindent", KeyEvent.VK_I, ctrl | alt, () -> lines(this::reindent, true));
        add("Align columns", 0, 0, this::align);
        add("Toggle comment", KeyEvent.VK_SLASH, shortcut, () -> invoke(RSyntaxTextAreaEditorKit.rstaToggleCommentAction));
        pane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent event) {
                // X11 also sends a typed slash, and Swing ignores modifiers for typed bindings.
                if (event.getKeyChar() == '/' && event.getModifiersEx() == shortcut) {
                    event.consume();
                }
            }
        });
        add(COLUMN_SELECTION_MODE, mac ? KeyEvent.VK_8 : KeyEvent.VK_INSERT, (mac ? shortcut : alt) | shift,
                () -> actions.get(COLUMN_SELECTION_MODE).putValue(Action.SELECTED_KEY, selections.toggleColumnSelectionMode()));
        actions.get(COLUMN_SELECTION_MODE).putValue(Action.SELECTED_KEY, false);
        add("Go to line...", mac ? KeyEvent.VK_L : KeyEvent.VK_G, shortcut, this::goTo);
        add("Indentation...", 0, 0, this::indentationSettings);
        add("Token colors...", 0, 0, this::tokenColors);
    }

    void configure(EditorState state) {
        this.state = state;
        pane.setTabSize(Math.max(1, Math.min(16, state.number("TabSize", 4))));
        pane.setTabsEmulated(state.flag("Spaces", true));
        pane.setWhitespaceVisible(state.flag("Whitespace", false));
        pane.setPaintTabLines(state.flag("IndentGuides", false));
        pane.setLineWrap(state.flag("Wrap", false));
        applyColors();
    }

    EditorState state() {
        return state;
    }

    List<JMenuItem> editMenuItems() {
        JMenu edit = menu("Text", "Duplicate line / selection", "Sort lines", "Reverse lines", "Remove duplicate lines", "Uppercase", "Lowercase", "Invert case", "Toggle comment", "Reindent", "Align columns", "Tabs to spaces", "Leading spaces to tabs", "Trim trailing whitespace");
        JCheckBoxMenuItem columnSelection = new JCheckBoxMenuItem(actions.get(COLUMN_SELECTION_MODE));
        JMenuItem goTo = new JMenuItem(actions.get("Go to line..."));
        JMenuItem indentation = new JMenuItem(actions.get("Indentation..."));
        JMenuItem tokenColors = new JMenuItem(actions.get("Token colors..."));
        JMenu view = new JMenu("Editor display");
        toggle(view, "Whitespace", "Visible whitespace", pane::isWhitespaceVisible, pane::setWhitespaceVisible);
        toggle(view, "IndentGuides", "Indentation guides", pane::getPaintTabLines, pane::setPaintTabLines);
        toggle(view, "Wrap", "Soft wrap", pane::getLineWrap, pane::setLineWrap);
        return List.of(edit, columnSelection, goTo, indentation, tokenColors, view);
    }

    private JMenu menu(String title, String... names) {
        JMenu menu = new JMenu(title);
        for (String name : names) menu.add(actions.get(name));
        return menu;
    }

    private void add(String name, int key, int modifiers, Runnable runnable) {
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runnable.run();
                } catch (IllegalArgumentException | UnsupportedOperationException ex) {
                    JOptionPane.showMessageDialog(pane, ex.getMessage(), name, JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        if (key != 0) {
            KeyStroke stroke = KeyStroke.getKeyStroke(key, modifiers);
            action.putValue(Action.ACCELERATOR_KEY, stroke);
            pane.getInputMap().put(stroke, name);
            pane.getActionMap().put(name, action);
        }
        actions.put(name, action);
    }

    Action action(String name) {
        return actions.get(name);
    }

    MultipleSelection selections() {
        return selections;
    }

    private void invoke(String key) {
        Action action = pane.getActionMap().get(key);
        if (action != null) action.actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, key));
    }

    private void changeCase(String key) {
        List<MultipleSelection.Range> ranges = selections.ranges();
        pane.beginAtomicEdit();
        try {
            if (ranges.isEmpty()) {
                invoke(key);
                return;
            }
            selections.clear();
            for (int i = ranges.size() - 1; i >= 0; i--) {
                MultipleSelection.Range range = ranges.get(i);
                pane.select(range.start, range.end);
                invoke(key);
            }
        } finally {
            pane.endAtomicEdit();
        }
    }

    private void lines(UnaryOperator<String> operation, boolean wholeFile) {
        selections.clear();
        TextEdits.lines(pane, operation, wholeFile);
    }

    private void selection(UnaryOperator<String> operation) {
        if (pane.getSelectedText() != null) {
            selections.clear();
            TextEdits.replace(pane, pane.getSelectionStart(), pane.getSelectionEnd(), operation.apply(pane.getSelectedText()));
        }
    }

    private void duplicate() {
        if (pane.getSelectionStart() == pane.getSelectionEnd())
            lines(text -> text.endsWith("\n") ? text + text : text + "\n" + text, false);
        else selection(text -> text + text);
    }

    private String reindent(String text) {
        int level = 0;
        Map<Integer, Integer> deltas = new HashMap<>();
        if (compiler != null) {
            for (net.emustudio.emulib.plugin.compiler.Token token : compiler.createLexer().analyze(text)) {
                if (token.getType() == net.emustudio.emulib.plugin.compiler.Token.BLOCK_START || token.getType() == net.emustudio.emulib.plugin.compiler.Token.BLOCK_END) {
                    int line = (int) text.substring(0, token.getOffset()).chars().filter(ch -> ch == '\n').count();
                    deltas.merge(line, token.getType() == net.emustudio.emulib.plugin.compiler.Token.BLOCK_START ? 1 : -1, Integer::sum);
                }
            }
        }
        List<String> result = new ArrayList<>();
        String indent = pane.getTabsEmulated() ? " ".repeat(pane.getTabSize()) : "\t";
        int line = 0;
        for (String value : text.split("\n", -1)) {
            int delta = deltas.getOrDefault(line++, 0);
            if (delta < 0) level = Math.max(0, level + delta);
            result.add(value.isBlank() ? "" : indent.repeat(level) + value.strip());
            if (delta > 0) level += delta;
        }
        return String.join("\n", result);
    }

    private void align() {
        // Preserve token contents, including quoted strings and comments. Only gaps change.
        lines(text -> {
            if (compiler == null) throw new IllegalArgumentException("Select compiler syntax to align token columns");
            List<net.emustudio.emulib.plugin.compiler.Token> tokens = compiler.createLexer().analyze(text);
            List<String> result = new ArrayList<>();
            int offset = 0;
            for (String line : text.split("\n", -1)) {
                final int start = offset, end = offset + line.length();
                List<net.emustudio.emulib.plugin.compiler.Token> row = new ArrayList<>();
                for (net.emustudio.emulib.plugin.compiler.Token token : tokens) {
                    if (token.getOffset() >= start && token.getOffset() < end && token.getType() != net.emustudio.emulib.plugin.compiler.Token.WHITESPACE && token.getType() != net.emustudio.emulib.plugin.compiler.Token.EOF)
                        row.add(token);
                }
                if (row.isEmpty()) result.add(line);
                else {
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < row.size(); i++) {
                        net.emustudio.emulib.plugin.compiler.Token token = row.get(i);
                        int target = i == 0 ? 0 : token.getType() == net.emustudio.emulib.plugin.compiler.Token.COMMENT ? state.number("CommentColumn", 40) : i == 1 ? state.number("OperandColumn", 16) : -1;
                        if (target >= 0 && formatted.length() > 0)
                            formatted.append(" ".repeat(Math.max(1, target - formatted.length())));
                        else if (i > 0) {
                            net.emustudio.emulib.plugin.compiler.Token previous = row.get(i - 1);
                            int gap = token.getOffset() - previous.getOffset() - previous.getText().length();
                            if (gap > 0) formatted.append(" ".repeat(gap));
                        }
                        formatted.append(token.getText());
                    }
                    result.add(formatted.toString());
                }
                offset = end + 1;
            }
            return String.join("\n", result);
        }, true);
    }

    private void goTo() {
        String value = JOptionPane.showInputDialog(pane, "Line[:column] (starts at 1)", "Go to", JOptionPane.QUESTION_MESSAGE);
        if (value == null) return;
        try {
            String[] parts = value.trim().split(":", -1);
            int line = Integer.parseInt(parts[0]) - 1, column = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
            if (parts.length > 2 || line < 0 || line >= pane.getLineCount() || column < 0)
                throw new IllegalArgumentException("Invalid line or column");
            int start = pane.getLineStartOffset(line), end = Math.min(pane.getDocument().getLength(), pane.getLineEndOffset(line));
            pane.setCaretPosition(Math.min(start + column, end > start && line + 1 < pane.getLineCount() ? end - 1 : end));
        } catch (BadLocationException | NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid line[:column]");
        }
    }

    private void toggle(JMenu menu, String key, String label, java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, get.getAsBoolean());
        item.addActionListener(e -> {
            set.accept(item.isSelected());
            state.setFlag(key, item.isSelected());
        });
        menu.add(item);
    }

    private void indentationSettings() {
        JSpinner size = new JSpinner(new SpinnerNumberModel(pane.getTabSize(), 1, 16, 1));
        JCheckBox spaces = new JCheckBox("Insert spaces", pane.getTabsEmulated());
        JSpinner operand = new JSpinner(new SpinnerNumberModel(state.number("OperandColumn", 16), 1, 120, 1));
        JSpinner comment = new JSpinner(new SpinnerNumberModel(state.number("CommentColumn", 40), 1, 200, 1));
        if (JOptionPane.showConfirmDialog(pane, new Object[]{"Indent size", size, spaces, "Second token column", operand, "Comment column", comment}, "Indentation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        pane.setTabSize((Integer) size.getValue());
        pane.setTabsEmulated(spaces.isSelected());
        state.setNumber("TabSize", pane.getTabSize());
        state.setFlag("Spaces", spaces.isSelected());
        state.setNumber("OperandColumn", (Integer) operand.getValue());
        state.setNumber("CommentColumn", (Integer) comment.getValue());
    }

    private enum TokenColor {
        RESERVED("Reserved", "Reserved words", Token.RESERVED_WORD),
        IDENTIFIER("Identifier", "Identifiers", Token.IDENTIFIER),
        LABEL("Label", "Labels / definitions", Token.ANNOTATION),
        PREPROCESSOR("Preprocessor", "Directives / blocks", Token.PREPROCESSOR),
        COMMENT("Comment", "Comments", Token.COMMENT_MARKUP),
        LITERAL("Literal", "Literals", Token.LITERAL_NUMBER_DECIMAL_INT),
        REGISTER("Register", "Registers", Token.RESERVED_WORD_2),
        OPERATOR("Operator", "Operators", Token.OPERATOR),
        SEPARATOR("Separator", "Separators", Token.SEPARATOR),
        ERROR("Error", "Errors", Token.ERROR_IDENTIFIER);

        private final String keySuffix;
        private final String label;
        private final int type;

        TokenColor(String keySuffix, String label, int type) {
            this.keySuffix = keySuffix;
            this.label = label;
            this.type = type;
        }
    }

    private void tokenColors() {
        TokenColor[] options = TokenColor.values();
        String[] labels = Arrays.stream(options).map(option -> option.label).toArray(String[]::new);
        SyntaxScheme scheme = pane.getSyntaxScheme();
        Color[] original = Arrays.stream(options)
                .map(option -> Objects.requireNonNullElse(scheme.getStyle(option.type).foreground, pane.getForeground()))
                .toArray(Color[]::new);
        Color[] colors = original.clone();

        JList<String> categories = new JList<>(labels);
        categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categories.setVisibleRowCount(labels.length);
        categories.setSelectedIndex(0);
        categories.getAccessibleContext().setAccessibleName("Token category");
        JColorChooser chooser = new JColorChooser(colors[0]);
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        panels[0] = new TokenColorSwatches();
        chooser.setChooserPanels(panels);
        categories.addListSelectionListener(event -> {
            int index = categories.getSelectedIndex();
            if (!event.getValueIsAdjusting() && index >= 0) chooser.setColor(colors[index]);
        });
        chooser.getSelectionModel().addChangeListener(event -> {
            int index = categories.getSelectedIndex();
            if (index >= 0) colors[index] = chooser.getColor();
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(categories), chooser);
        split.setBorder(null);
        int result = JOptionPane.showConfirmDialog(pane, split, "Token colors", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        for (int i = 0; i < options.length; i++) {
            if (!colors[i].equals(original[i])) state.setTokenColor(options[i].keySuffix, colors[i].getRGB());
        }
        applyColors();
    }

    private void applyColors() {
        SyntaxScheme scheme = (SyntaxScheme) pane.getSyntaxScheme().clone();
        for (TokenColor category : TokenColor.values()) {
            Optional<Integer> color = state.tokenColor(category.keySuffix);
            if (color.isEmpty()) continue;
            int type = category.type;
            Color value = new Color(color.get(), true);
            scheme.getStyle(type).foreground = value;
            int first = type, last = type;
            if (type == Token.LITERAL_NUMBER_DECIMAL_INT) {
                first = Token.LITERAL_BOOLEAN;
                last = Token.LITERAL_BACKQUOTE;
            }
            if (type == Token.COMMENT_MARKUP) {
                first = Token.COMMENT_EOL;
                last = Token.COMMENT_MARKUP;
            }
            if (type == Token.ERROR_IDENTIFIER) {
                first = Token.ERROR_IDENTIFIER;
                last = Token.ERROR_STRING_DOUBLE;
            }
            for (int related = first; related <= last; related++) scheme.getStyle(related).foreground = value;
        }
        pane.setSyntaxScheme(scheme);
    }
}
