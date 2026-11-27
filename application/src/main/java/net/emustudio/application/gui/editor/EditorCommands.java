/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugin.compiler.Compiler;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaEditorKit;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Local editing commands. Knows generic token categories, never compiler keywords.
 */
final class EditorCommands {
    private static final String COLUMN_SELECTION_MODE = "Column Selection Mode";

    private final TextEditorPane pane;
    private final Compiler compiler;
    private final MultipleSelection selections;
    private EditorState state = new EditorState(null);
    private final Map<String, Action> actions = new LinkedHashMap<>();

    EditorCommands(TextEditorPane pane, Compiler compiler) {
        this.pane = pane;
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
        add("Reindent", KeyEvent.VK_I, ctrl | alt, () -> lines(this::reindent, true));
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
        pane.setLineWrap(state.flag("Wrap", false));
        applyColors();
    }

    EditorState state() {
        return state;
    }

    List<JMenuItem> editMenuItems() {
        JMenu text = menu("Text", "Duplicate line / selection", "Toggle comment", "Reindent");
        JCheckBoxMenuItem columnSelection = new JCheckBoxMenuItem(actions.get(COLUMN_SELECTION_MODE));
        JMenuItem goTo = new JMenuItem(actions.get("Go to line..."));
        JMenuItem indentation = new JMenuItem(actions.get("Indentation..."));
        JMenuItem tokenColors = new JMenuItem(actions.get("Token colors..."));
        JCheckBoxMenuItem softWrap = new JCheckBoxMenuItem("Soft wrap", pane.getLineWrap());
        softWrap.addActionListener(e -> {
            boolean enabled = softWrap.isSelected();
            pane.setLineWrap(enabled);
            state.setFlag("Wrap", enabled);
        });
        return List.of(text, columnSelection, goTo, indentation, tokenColors, softWrap);
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

    private void indentationSettings() {
        JSpinner size = new JSpinner(new SpinnerNumberModel(pane.getTabSize(), 1, 16, 1));
        JCheckBox spaces = new JCheckBox("Insert spaces", pane.getTabsEmulated());
        if (JOptionPane.showConfirmDialog(pane, new Object[]{"Tab size", size, spaces},
                "Indentation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        pane.setTabSize((Integer) size.getValue());
        pane.setTabsEmulated(spaces.isSelected());
        state.setNumber("TabSize", pane.getTabSize());
        state.setFlag("Spaces", spaces.isSelected());
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
