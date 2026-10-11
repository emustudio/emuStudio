/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rtextarea.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Local editing commands. Knows generic token categories, never compiler keywords.
 */
final class EditorCommands {
    private final REditor editor;
    private final TextEditorPane pane;
    private final RTextScrollPane scroll;
    private final Compiler compiler;
    private final MultipleSelection selections;
    private EditorState state = new EditorState(null);
    private final Map<GutterIconInfo, String> bookmarkNames = new IdentityHashMap<>();
    private final Deque<int[]> selectionHistory = new ArrayDeque<>();
    private final Map<String, Action> actions = new LinkedHashMap<>();

    EditorCommands(REditor editor, TextEditorPane pane, RTextScrollPane scroll, Compiler compiler) {
        this.editor = editor;
        this.pane = pane;
        this.scroll = scroll;
        this.compiler = compiler;
        selections = new MultipleSelection(pane);
        scroll.getGutter().setBookmarkIcon(new Icon() {
            public int getIconWidth() {
                return 12;
            }

            public int getIconHeight() {
                return 14;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(new Color(0x3578ba));
                g.fillPolygon(new int[]{x + 2, x + 10, x + 10, x + 6, x + 2}, new int[]{y + 1, y + 1, y + 13, y + 9, y + 13}, 5);
            }
        });
        scroll.getGutter().setBookmarkingEnabled(true);
        add("Duplicate line / selection", KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, this::duplicate);
        for (String name : List.of("Sort lines", "Reverse lines", "Remove duplicate lines")) {
            add(name, 0, 0, () -> lines(text -> TextEdits.reorder(text, name), true));
        }
        add("Uppercase", 0, 0, () -> selection(text -> text.toUpperCase(Locale.ROOT)));
        add("Lowercase", 0, 0, () -> selection(text -> text.toLowerCase(Locale.ROOT)));
        add("Invert case", 0, 0, () -> selection(text -> {
            StringBuilder result = new StringBuilder();
            text.codePoints().forEach(cp -> result.appendCodePoint(Character.isUpperCase(cp) ? Character.toLowerCase(cp) : Character.toUpperCase(cp)));
            return result.toString();
        }));
        add("Trim trailing whitespace", 0, 0, () -> lines(text -> text.replaceAll("(?m)[ \\t]+$", ""), true));
        add("Tabs to spaces", 0, 0, () -> lines(text -> TextEdits.spaces(text, pane.getTabSize()), true));
        add("Leading spaces to tabs", 0, 0, () -> lines(text -> TextEdits.tabs(text, pane.getTabSize()), true));
        add("Reindent", 0, 0, () -> lines(this::reindent, true));
        add("Align columns", 0, 0, this::align);
        add("Toggle comment", KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK, () -> invoke(RSyntaxTextAreaEditorKit.rstaToggleCommentAction));
        add("Select next occurrence", KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK, () -> selectOccurrences(false));
        add("Select all occurrences", KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, () -> selectOccurrences(true));
        add("Column selection", 0, 0, () -> selections.rectangle(pane.getSelectionStart(), pane.getSelectionEnd()));
        add("Expand selection", KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, this::expand);
        add("Shrink selection", KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, this::shrink);
        add("Go to line / column", KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK, this::goTo);
        add("Toggle bookmark", KeyEvent.VK_F2, InputEvent.CTRL_DOWN_MASK, () -> {
            invoke(RTextAreaEditorKit.rtaToggleBookmarkAction);
            persistBookmarks();
        });
        add("Next bookmark", KeyEvent.VK_F2, 0, () -> invoke(RTextAreaEditorKit.rtaNextBookmarkAction));
        add("Previous bookmark", KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK, () -> invoke(RTextAreaEditorKit.rtaPrevBookmarkAction));
        add("Name / number bookmark", 0, 0, this::nameBookmark);
        for (int i = 0; i <= 9; i++) {
            final String number = Integer.toString(i);
            add("Go to bookmark " + number, KeyEvent.VK_0 + i, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK, () -> jumpNamed(number));
        }
        add("Start recording", 0, 0, RTextArea::beginRecordingMacro);
        add("Stop recording", 0, 0, RTextArea::endRecordingMacro);
        add("Replay recording", KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, pane::playbackLastMacro);
        add("Collapse all", 0, 0, () -> fold(true));
        add("Expand all", 0, 0, () -> fold(false));
        add("Indentation settings", 0, 0, this::indentationSettings);
        add("Token colors", 0, 0, this::tokenColors);
    }

    void configure(EditorState state) {
        this.state = state;
        pane.setTabSize(Math.max(1, Math.min(16, state.number("TabSize", 4))));
        pane.setTabsEmulated(state.flag("Spaces", true));
        pane.setWhitespaceVisible(state.flag("Whitespace", false));
        pane.setEOLMarkersVisible(state.flag("EndOfLine", false));
        pane.setPaintTabLines(state.flag("IndentGuides", false));
        pane.setLineWrap(state.flag("Wrap", false));
        pane.setCodeFoldingEnabled(state.flag("Folding", true));
        restoreBookmarks();
        applyColors();
    }

    EditorState state() {
        return state;
    }

    List<JMenu> menus() {
        JMenu edit = menu("Text", "Duplicate line / selection", "Sort lines", "Reverse lines", "Remove duplicate lines", "Uppercase", "Lowercase", "Invert case", "Toggle comment", "Reindent", "Align columns", "Tabs to spaces", "Leading spaces to tabs", "Trim trailing whitespace");
        JMenu select = menu("Selection", "Select next occurrence", "Select all occurrences", "Column selection", "Expand selection", "Shrink selection");
        JMenu navigate = menu("Navigate", "Go to line / column", "Toggle bookmark", "Next bookmark", "Previous bookmark", "Name / number bookmark");
        JMenu macros = menu("Editing macros", "Start recording", "Stop recording", "Replay recording");
        JMenu view = menu("Editor display", "Collapse all", "Expand all", "Indentation settings", "Token colors");
        toggle(view, "Whitespace", "Visible whitespace", pane::isWhitespaceVisible, pane::setWhitespaceVisible);
        toggle(view, "EndOfLine", "End-of-line markers", pane::getEOLMarkersVisible, pane::setEOLMarkersVisible);
        toggle(view, "IndentGuides", "Indentation guides", pane::getPaintTabLines, pane::setPaintTabLines);
        toggle(view, "Wrap", "Soft wrap", pane::getLineWrap, pane::setLineWrap);
        toggle(view, "Folding", "Code folding", pane::isCodeFoldingEnabled, pane::setCodeFoldingEnabled);
        JMenu syntax = new JMenu("File syntax");
        for (String[] choice : new String[][]{{"Compiler", SourceFoldParser.COMPILER_STYLE}, {"Plain text", SyntaxConstants.SYNTAX_STYLE_NONE}, {"Java", SyntaxConstants.SYNTAX_STYLE_JAVA}, {"C", SyntaxConstants.SYNTAX_STYLE_C}, {"JSON", SyntaxConstants.SYNTAX_STYLE_JSON}, {"XML", SyntaxConstants.SYNTAX_STYLE_XML}, {"Shell", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL}}) {
            JMenuItem item = new JMenuItem(choice[0]);
            item.setEnabled(compiler != null || !choice[0].equals("Compiler"));
            item.addActionListener(e -> {
                pane.setSyntaxEditingStyle(choice[1]);
                applyColors();
            });
            syntax.add(item);
        }
        view.add(syntax);
        return List.of(edit, select, navigate, macros, view);
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
            for (net.emustudio.emulib.plugins.compiler.Token token : compiler.createLexer().analyze(text)) {
                if (token.getType() == net.emustudio.emulib.plugins.compiler.Token.BLOCK_START || token.getType() == net.emustudio.emulib.plugins.compiler.Token.BLOCK_END) {
                    int line = (int) text.substring(0, token.getOffset()).chars().filter(ch -> ch == '\n').count();
                    deltas.merge(line, token.getType() == net.emustudio.emulib.plugins.compiler.Token.BLOCK_START ? 1 : -1, Integer::sum);
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
            List<net.emustudio.emulib.plugins.compiler.Token> tokens = compiler.createLexer().analyze(text);
            List<String> result = new ArrayList<>();
            int offset = 0;
            for (String line : text.split("\n", -1)) {
                final int start = offset, end = offset + line.length();
                List<net.emustudio.emulib.plugins.compiler.Token> row = new ArrayList<>();
                for (net.emustudio.emulib.plugins.compiler.Token token : tokens) {
                    if (token.getOffset() >= start && token.getOffset() < end && token.getType() != net.emustudio.emulib.plugins.compiler.Token.WHITESPACE && token.getType() != net.emustudio.emulib.plugins.compiler.Token.EOF)
                        row.add(token);
                }
                if (row.isEmpty()) result.add(line);
                else {
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < row.size(); i++) {
                        net.emustudio.emulib.plugins.compiler.Token token = row.get(i);
                        int target = i == 0 ? 0 : token.getType() == net.emustudio.emulib.plugins.compiler.Token.COMMENT ? state.number("CommentColumn", 40) : i == 1 ? state.number("OperandColumn", 16) : -1;
                        if (target >= 0 && formatted.length() > 0)
                            formatted.append(" ".repeat(Math.max(1, target - formatted.length())));
                        else if (i > 0) {
                            net.emustudio.emulib.plugins.compiler.Token previous = row.get(i - 1);
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

    private void selectOccurrences(boolean all) {
        String selected = pane.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            pane.select(pane.getSelectionStart(), pane.getSelectionEnd());
            try {
                pane.select(Utilities.getWordStart(pane, pane.getCaretPosition()), Utilities.getWordEnd(pane, pane.getCaretPosition()));
            } catch (BadLocationException e) {
                return;
            }
            selected = pane.getSelectedText();
        }
        if (selected == null || selected.isBlank()) return;
        List<EditorSearch.Match> matches = EditorSearch.find(pane.getText(), selected, "", false, true, false, false, 0, pane.getDocument().getLength());
        List<MultipleSelection.Range> next = new ArrayList<>(selections.ranges());
        if (next.isEmpty()) next.add(new MultipleSelection.Range(pane.getSelectionStart(), pane.getSelectionEnd()));
        int last = next.stream().mapToInt(r -> r.end).max().orElse(0);
        if (all) next.clear();
        for (int i = 0; i < matches.size(); i++) {
            EditorSearch.Match match = matches.get(i);
            if (all || match.start >= last) {
                next.add(new MultipleSelection.Range(match.start, match.end));
                if (!all) break;
            }
        }
        selections.set(next);
    }

    private void expand() {
        int start = pane.getSelectionStart(), end = pane.getSelectionEnd();
        selectionHistory.push(new int[]{start, end});
        selections.clear();
        try {
            if (start == end) {
                pane.select(Utilities.getWordStart(pane, start), Utilities.getWordEnd(pane, end));
                return;
            }
            int[] bracket = enclosingBrackets(start, end);
            if (bracket != null) {
                pane.select(bracket[0], bracket[1]);
                return;
            }
            int firstLine = pane.getLineOfOffset(start), lastLine = pane.getLineOfOffset(Math.max(start, end - 1));
            int lineStart = pane.getLineStartOffset(firstLine), lineEnd = Math.min(pane.getDocument().getLength(), pane.getLineEndOffset(lastLine));
            if (start != lineStart || end != lineEnd) {
                pane.select(lineStart, lineEnd);
                return;
            }
            Fold fold = pane.getFoldManager().getDeepestFoldContaining(start);
            if (fold != null && fold.getEndOffset() >= end) {
                pane.select(fold.getStartOffset(), fold.getEndOffset() + 1);
                return;
            }
            pane.selectAll();
        } catch (BadLocationException e) {
            selectionHistory.pop();
        }
    }

    private int[] enclosingBrackets(int start, int end) {
        Deque<int[]> stack = new ArrayDeque<>();
        int[] smallest = null;
        for (int line = 0; line < pane.getLineCount(); line++) {
            for (Token token = pane.getTokenListForLine(line); token != null && token.isPaintable(); token = token.getNextToken()) {
                if (token.isComment() || token.getType() >= Token.LITERAL_BOOLEAN && token.getType() <= Token.LITERAL_BACKQUOTE)
                    continue;
                for (int i = 0; i < token.length(); i++) {
                    char ch = token.charAt(i);
                    int offset = token.getOffset() + i;
                    int opening = "([{".indexOf(ch), closing = ")]}".indexOf(ch);
                    if (opening >= 0 && opening < 3) stack.push(new int[]{offset, opening});
                    else if (closing >= 0 && !stack.isEmpty()) {
                        int[] match = stack.pop();
                        if (match[1] == closing && match[0] <= start && offset + 1 >= end && (match[0] < start || offset + 1 > end) && (smallest == null || offset + 1 - match[0] < smallest[1] - smallest[0]))
                            smallest = new int[]{match[0], offset + 1};
                    }
                }
            }
        }
        return smallest;
    }

    private void shrink() {
        if (!selectionHistory.isEmpty()) {
            int[] previous = selectionHistory.pop();
            pane.select(Math.min(previous[0], pane.getDocument().getLength()), Math.min(previous[1], pane.getDocument().getLength()));
        }
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

    void clearBookmarks() {
        for (GutterIconInfo bookmark : scroll.getGutter().getBookmarks())
            scroll.getGutter().removeTrackingIcon(bookmark);
        bookmarkNames.clear();
    }

    void persistBookmarks() {
        editor.getCurrentFile().ifPresent(file -> {
            List<String> values = new ArrayList<>();
            for (GutterIconInfo mark : scroll.getGutter().getBookmarks()) {
                try {
                    values.add(pane.getLineOfOffset(mark.getMarkedOffset()) + ":" + Base64.getEncoder().encodeToString(bookmarkNames.getOrDefault(mark, "").getBytes(StandardCharsets.UTF_8)));
                } catch (BadLocationException ignored) {
                }
            }
            state.bookmarks(file.toPath(), String.join(";", values));
        });
    }

    void restoreBookmarks() {
        clearBookmarks();
        editor.getCurrentFile().ifPresent(file -> {
            for (String value : state.bookmarks(file.toPath()).split(";")) {
                if (value.isEmpty()) continue;
                try {
                    String[] parts = value.split(":", 2);
                    int line = Integer.parseInt(parts[0]);
                    if (line >= 0 && line < pane.getLineCount()) {
                        scroll.getGutter().toggleBookmark(line);
                        for (GutterIconInfo mark : scroll.getGutter().getBookmarks()) {
                            if (pane.getLineOfOffset(mark.getMarkedOffset()) == line)
                                bookmarkNames.put(mark, new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8));
                        }
                    }
                } catch (IllegalArgumentException | BadLocationException ignored) {
                }
            }
        });
    }

    Map<Integer, String> bookmarks() {
        Map<Integer, String> result = new TreeMap<>();
        for (GutterIconInfo mark : scroll.getGutter().getBookmarks()) {
            try {
                result.put(pane.getLineOfOffset(mark.getMarkedOffset()), bookmarkNames.getOrDefault(mark, ""));
            } catch (BadLocationException ignored) {
            }
        }
        return result;
    }

    private void nameBookmark() {
        String value = JOptionPane.showInputDialog(pane, "Bookmark name or digit (0–9)");
        if (value == null) return;
        try {
            int line = pane.getLineOfOffset(pane.getCaretPosition());
            if (!bookmarks().containsKey(line)) scroll.getGutter().toggleBookmark(line);
            for (GutterIconInfo mark : scroll.getGutter().getBookmarks()) {
                if (value.equals(bookmarkNames.get(mark))) bookmarkNames.remove(mark);
                if (pane.getLineOfOffset(mark.getMarkedOffset()) == line) bookmarkNames.put(mark, value);
            }
            persistBookmarks();
        } catch (BadLocationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    void jumpNamed(String name) {
        bookmarks().entrySet().stream().filter(entry -> entry.getValue().equals(name)).findFirst().ifPresent(entry -> {
            try {
                pane.setCaretPosition(pane.getLineStartOffset(entry.getKey()));
            } catch (BadLocationException ignored) {
            }
        });
    }

    private void fold(boolean collapsed) {
        for (int i = 0; i < pane.getFoldManager().getFoldCount(); i++)
            setCollapsed(pane.getFoldManager().getFold(i), collapsed);
    }

    private void setCollapsed(Fold fold, boolean collapsed) {
        for (int i = 0; i < fold.getChildCount(); i++) setCollapsed(fold.getChild(i), collapsed);
        fold.setCollapsed(collapsed);
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

    private final int[] colorTypes = {Token.RESERVED_WORD, Token.IDENTIFIER, Token.ANNOTATION, Token.PREPROCESSOR, Token.COMMENT_MARKUP, Token.LITERAL_NUMBER_DECIMAL_INT, Token.RESERVED_WORD_2, Token.OPERATOR, Token.SEPARATOR, Token.ERROR_IDENTIFIER};

    private void tokenColors() {
        String[] labels = {"Reserved words", "Identifiers", "Labels / definitions", "Directives / blocks", "Comments", "Literals", "Registers", "Operators", "Separators", "Errors"};
        String choice = (String) JOptionPane.showInputDialog(pane, "Token category", "Token colors", JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (choice == null) return;
        int type = colorTypes[Arrays.asList(labels).indexOf(choice)];
        Color value = JColorChooser.showDialog(pane, choice, pane.getSyntaxScheme().getStyle(type).foreground);
        if (value != null) {
            state.setNumber("Color" + type, value.getRGB());
            applyColors();
        }
    }

    private void applyColors() {
        SyntaxScheme scheme = (SyntaxScheme) pane.getSyntaxScheme().clone();
        for (int type : colorTypes) {
            int color = state.number("Color" + type, Integer.MIN_VALUE);
            if (color == Integer.MIN_VALUE) continue;
            Color value = new Color(color, true);
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
