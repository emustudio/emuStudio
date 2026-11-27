/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.plugin.compiler.*;
import net.emustudio.emulib.application.ui.Dialogs;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import javax.swing.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class EditingFeaturesTest extends AbstractSwingTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();
    private REditor editor(String text) {
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class)));
        runOnEdt(() -> editor.textPane().setText(text)); return editor;
    }
    private void command(REditor editor, String name) {
        runOnEdt(() -> editor.commands().action(name).actionPerformed(new ActionEvent(editor.textPane(), 0, name)));
    }
    @Test public void tokenColorDialogKeepsChangesAcrossCategoriesUntilConfirmed() throws Exception {
        Path settingsFile = files.newFile("emuStudio.toml").toPath();
        Color originalCommentColor = new Color(0x80000000, true);
        Files.writeString(settingsFile, "tokenColorReserved = " + Color.BLACK.getRGB()
                + "\ntokenColorComment = " + originalCommentColor.getRGB() + "\n");
        AppSettings settings = AppSettings.fromFile(settingsFile, false);
        REditor editor = editor("NOP");
        runOnEdt(() -> editor.configure(new EditorState(settings)));
        JFrame frame = showInFrame(editor.getView());
        runOnEdt(() -> frame.setVisible(true));

        JDialog dialog = openTokenColors(editor);
        JSplitPane split = findComponent(dialog, JSplitPane.class, pane -> pane.getRightComponent() instanceof JColorChooser);
        JList<?> categories = findComponent((Container) split.getLeftComponent(), JList.class, list -> true);
        JColorChooser chooser = (JColorChooser) split.getRightComponent();
        JList<?> swatches = findComponent(chooser, JList.class,
                list -> "Swatches".equals(list.getAccessibleContext().getAccessibleName()));
        JList<?> recent = findComponent(chooser, JList.class,
                list -> "Recent colors".equals(list.getAccessibleContext().getAccessibleName()));
        TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            assertEquals(JSplitPane.HORIZONTAL_SPLIT, split.getOrientation());
            assertEquals("Reserved words", categories.getSelectedValue());
            assertEquals(pane.getSyntaxScheme().getStyle(TokenTypes.RESERVED_WORD).foreground, chooser.getColor());
            assertEquals(Color.BLACK, swatches.getSelectedValue());
            swatches.setSelectedValue(Color.MAGENTA, true);
            assertEquals(Color.MAGENTA, chooser.getColor());
            categories.setSelectedValue("Comments", true);
            assertEquals(originalCommentColor, chooser.getColor());
            assertTrue(swatches.isSelectionEmpty());
            assertEquals(originalCommentColor, recent.getSelectedValue());
            chooser.setColor(Color.ORANGE);
            categories.setSelectedValue("Reserved words", true);
            assertEquals(Color.MAGENTA, chooser.getColor());
            assertEquals(Color.MAGENTA, swatches.getSelectedValue());
            assertTrue(recent.isSelectionEmpty());
            assertEquals(Color.BLACK.getRGB(), settings.getInt("tokenColorReserved").orElseThrow().intValue());
        });
        triggerButton(findButton(dialog, "OK"));
        runOnEdt(() -> {
            assertEquals(Color.MAGENTA, pane.getSyntaxScheme().getStyle(TokenTypes.RESERVED_WORD).foreground);
            assertEquals(Color.ORANGE, pane.getSyntaxScheme().getStyle(TokenTypes.COMMENT_EOL).foreground);
        });
        AppSettings saved = AppSettings.fromFile(settingsFile, false);
        assertEquals(Color.MAGENTA.getRGB(), saved.getInt("tokenColorReserved").orElseThrow().intValue());
        assertEquals(Color.ORANGE.getRGB(), saved.getInt("tokenColorComment").orElseThrow().intValue());
        String confirmedSettings = Files.readString(settingsFile);

        JDialog reopened = openTokenColors(editor);
        JColorChooser reopenedChooser = findComponent(reopened, JColorChooser.class, picker -> true);
        runOnEdt(() -> {
            assertEquals(Color.MAGENTA, reopenedChooser.getColor());
            reopenedChooser.setColor(Color.CYAN);
        });
        triggerButton(findButton(reopened, "Cancel"));
        assertEquals(Color.MAGENTA, onEdt(() -> pane.getSyntaxScheme().getStyle(TokenTypes.RESERVED_WORD).foreground));
        assertEquals(confirmedSettings, Files.readString(settingsFile));
    }

    private JDialog openTokenColors(REditor editor) {
        SwingUtilities.invokeLater(() -> command(editor, "Token colors..."));
        return waitForWindow(JDialog.class, dialog -> "Token colors".equals(dialog.getTitle()));
    }

    @Test public void compilerSyntaxProvidesCommentRulesWithOldSyntaxOverride() throws Exception {
        Path source = Files.writeString(files.newFile("program.asm").toPath(), "block\n  body\nend\n");
        String encodedPath = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(source.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
        AppSettings settings = new AppSettings(Config.inMemory(), false);
        settings.set("editorSyntax" + encodedPath, "text/plain");
        LexicalAnalyzer lexer = new TokenLexer(List.of(new SourceToken(Token.BLOCK_START, 0, "block", null, null, null),
                new SourceToken(Token.BLOCK_END, 13, "end", null, null, null))) {
            public String[] getLineCommentStartAndEnd() { return new String[]{"##", null}; }
        };
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(lexer)));
        runOnEdt(() -> {
            editor.configure(new EditorState(settings));
            assertTrue(editor.openFile(source));
        });
        runOnEdt(() -> editor.textPane().select(0, 5)); command(editor, "Toggle comment");
        assertTrue(onEdt(editor.textPane()::getText).startsWith("##block\n"));
    }
    @Test public void compilerSyntaxMatchesNestedBracketsInBothDirections() {
        String source = "([{}])";
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < source.length(); i++)
            tokens.add(new SourceToken(Token.SEPARATOR, i, source.substring(i, i + 1), null, null, null));
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(new TokenLexer(tokens))));
        runOnEdt(() -> editor.textPane().setText(source));
        for (int i = 0; i < source.length(); i++) {
            final int bracket = i;
            java.awt.Point match = onEdt(() -> {
                editor.textPane().setCaretPosition(bracket + 1);
                return org.fife.ui.rsyntaxtextarea.RSyntaxUtilities.getMatchingBracketPosition(editor.textPane(), null);
            });
            assertEquals(new java.awt.Point(i, source.length() - 1 - i), match);
        }
        REditor comment = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(new TokenLexer(
                List.of(new SourceToken(Token.COMMENT, 0, "()", null, null, null))))));
        runOnEdt(() -> { comment.textPane().setText("()"); comment.textPane().setCaretPosition(1); });
        assertEquals(-1, onEdt(() -> org.fife.ui.rsyntaxtextarea.RSyntaxUtilities
                .getMatchingBracketPosition(comment.textPane(), null).y).intValue());
    }
    @Test public void multipleSelectionsTypeAndUndoTogether() {
        REditor editor = editor("one two one"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> { pane.discardAllEdits(); editor.commands().selections().set(List.of(new MultipleSelection.Range(0, 3), new MultipleSelection.Range(8, 11))); pane.replaceSelection("X"); });
        assertEquals("X two X", onEdt(pane::getText));
        runOnEdt(pane::undoLastAction);
        assertEquals("one two one", onEdt(pane::getText));
    }
    @Test public void multipleCaretsBackspaceTogether() {
        REditor editor = editor("abc\nabc"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> { editor.commands().selections().set(List.of(new MultipleSelection.Range(3, 3), new MultipleSelection.Range(7, 7))); pane.getActionMap().get(pane.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))).actionPerformed(new ActionEvent(pane, 0, "")); });
        assertEquals("ab\nab", onEdt(pane::getText));
    }
    @Test public void consecutiveTypingKeepsMultipleCarets() {
        REditor editor = editor("a\na"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            editor.commands().selections().set(List.of(new MultipleSelection.Range(1, 1), new MultipleSelection.Range(3, 3)));
            pane.replaceSelection("b"); pane.replaceSelection("c");
        });
        assertEquals("abc\nabc", onEdt(pane::getText));
    }
    @Test public void multipleCaretsDeleteCompleteUnicodeCharacters() {
        REditor editor = editor("a😀\nb😀"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            editor.commands().selections().set(List.of(new MultipleSelection.Range(3, 3), new MultipleSelection.Range(7, 7)));
            pane.getActionMap().get(pane.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))).actionPerformed(new ActionEvent(pane, 0, ""));
        });
        assertEquals("a\nb", onEdt(pane::getText));
    }
    @Test public void rectanglePadsRowsToTheSelectedColumn() {
        REditor editor = editor("abcd\na\nabcd");
        runOnEdt(() -> { editor.commands().selections().rectangle(3, 10); editor.textPane().replaceSelection("!"); });
        assertEquals("abc!d\na  !\nabc!d", onEdt(editor.textPane()::getText));
    }

    @Test public void rectangleCountsTabsAtTabStops() {
        REditor editor = editor("a\tb\nx\n    c");
        runOnEdt(() -> {
            editor.textPane().setTabSize(4);
            editor.commands().selections().rectangle(2, 10);
            editor.textPane().replaceSelection("!");
        });
        assertEquals("a\t!b\nx   !\n    !c", onEdt(editor.textPane()::getText));
    }
    @Test public void workspaceSearchShortcutsUseTheInlineQueryAndWrap() {
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null));
        runOnEdt(() -> { tabs.activeEditor().textPane().setText("one two one"); tabs.searchBar().find.setText("one"); });
        assertTrue(onEdt(() -> tabs.findNext().orElse(false)));
        assertEquals(0, onEdt(() -> tabs.activeEditor().textPane().getSelectionStart()).intValue());
        onEdt(tabs::findNext);
        assertEquals(8, onEdt(() -> tabs.activeEditor().textPane().getSelectionStart()).intValue());
        onEdt(tabs::findPrevious);
        assertEquals(0, onEdt(() -> tabs.activeEditor().textPane().getSelectionStart()).intValue());
    }
    @Test public void columnPasteDistributesClipboardRows() {
        REditor editor = editor("a\nb"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            editor.commands().selections().set(List.of(new MultipleSelection.Range(1, 1), new MultipleSelection.Range(3, 3)));
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection("X\nY"), null);
            pane.getActionMap().get(pane.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_V, RTextArea.getDefaultModifier())))
                    .actionPerformed(new ActionEvent(pane, 0, ""));
        });
        assertEquals("aX\nbY", onEdt(pane::getText));
    }
    @Test public void rectangularSelectionClampsShortLinesAndEditsRows() {
        REditor editor = editor("abc\na\nabc"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> pane.select(1, 8));
        command(editor, "Column Selection Mode");
        runOnEdt(() -> pane.replaceSelection("!"));
        assertEquals("a!c\na!\na!c", onEdt(pane::getText));
    }
    @Test public void columnCaretsStayOnBlankAndShortRowsWhenMovingHorizontally() {
        REditor editor = editor("abc\n\nx\nabc");
        TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            pane.setSize(400, 180);
            pane.setCaretPosition(1);
            columnSelectionItem(editor).doClick(0);
            KeyStroke extendDown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK);
            for (int line = 0; line < 3; line++) editorKey(pane, extendDown);

            KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
            editorKey(pane, right);
            List<MultipleSelection.Range> carets = editor.commands().selections().ranges();
            assertEquals(4, carets.size());
            assertEquals(4, carets.get(1).start);
            assertEquals(6, carets.get(2).start);

            editorKey(pane, right);
            editorKey(pane, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
            pane.replaceSelection("!");
            assertEquals("ab!c\n  !\nx !\nab!c", pane.getText());
        });
    }
    @Test public void columnSelectionModeRestoresTheOriginalKeyboardBindingWhenDisabled() {
        REditor editor = editor("abcd\na\nabcd");
        REditor other = editor("other");
        TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            JCheckBoxMenuItem mode = columnSelectionItem(editor);
            pane.setSize(400, 180);
            pane.setCaretPosition(3);
            assertFalse(mode.isSelected());
            KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK);
            Object linearSelection = pane.getInputMap().get(down);
            mode.doClick(0);
            assertTrue(mode.isSelected());
            assertFalse(columnSelectionItem(other).isSelected());

            editorKey(pane, down);
            editorKey(pane, down);
            editorKey(pane, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK));
            pane.replaceSelection("!");
            assertEquals("abc!\na  !\nabc!", pane.getText());

            editorKey(pane, mode.getAccelerator());
            assertFalse(mode.isSelected());
            assertEquals(linearSelection, pane.getInputMap().get(down));
            assertTrue(editor.commands().selections().ranges().isEmpty());
        });
    }
    @Test public void columnSelectionModeMakesPlainMouseDragRectangular() {
        REditor editor = editor("abc\ndef\nghi");
        TextEditorPane pane = editor.textPane();
        onEdt(() -> {
            pane.setSize(400, 180);
            columnSelectionItem(editor).doClick(0);
            Rectangle start = pane.modelToView2D(1).getBounds();
            Rectangle end = pane.modelToView2D(10).getBounds();
            long time = System.currentTimeMillis();
            pane.dispatchEvent(new MouseEvent(pane, MouseEvent.MOUSE_PRESSED, time, InputEvent.BUTTON1_DOWN_MASK,
                    start.x, start.y + start.height / 2, 1, false, MouseEvent.BUTTON1));
            pane.dispatchEvent(new MouseEvent(pane, MouseEvent.MOUSE_DRAGGED, time, InputEvent.BUTTON1_DOWN_MASK,
                    end.x, end.y + end.height / 2, 0, false, MouseEvent.NOBUTTON));
            pane.dispatchEvent(new MouseEvent(pane, MouseEvent.MOUSE_RELEASED, time, 0,
                    end.x, end.y + end.height / 2, 1, false, MouseEvent.BUTTON1));
            pane.replaceSelection("!");
            assertEquals("a!c\nd!f\ng!i", pane.getText());
            return null;
        });
    }
    private JCheckBoxMenuItem columnSelectionItem(REditor editor) {
        return editor.commands().editMenuItems().stream()
                .filter(JCheckBoxMenuItem.class::isInstance)
                .map(JCheckBoxMenuItem.class::cast)
                .filter(item -> "Column Selection Mode".equals(item.getText()))
                .findFirst().orElseThrow();
    }
    private void editorKey(TextEditorPane pane, KeyStroke key) {
        Action action = pane.getActionMap().get(pane.getInputMap().get(key));
        action.actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, ""));
    }
    @Test public void selectionSearchReplaceNeverTouchesOutsideScope() {
        REditor editor = editor("one one one"); InlineSearchBar bar = onEdt(() -> new InlineSearchBar(() -> editor));
        runOnEdt(() -> { editor.textPane().select(4, 7); bar.selection.doClick(); bar.find.setText("one"); bar.replace.setText("two"); bar.replaceAll(); });
        assertEquals("one two one", onEdt(editor.textPane()::getText));
        runOnEdt(() -> bar.find.setText("two"));
        assertEquals(1, onEdt(() -> bar.matches().size()).intValue());
    }
    @Test public void regexGroupsAndCasePreservationAreAppliedIndependently() {
        String source = "ONE One one other";
        List<EditorSearch.Match> matches = EditorSearch.find(source, "one", "new", false, false, true, true, 0, source.length());
        assertEquals("NEW New new other", EditorSearch.apply(source, matches));
        source = "ab12 cd34";
        matches = EditorSearch.find(source, "([a-z]+)([0-9]+)", "$2-$1", true, true, false, false, 0, source.length());
        assertEquals("12-ab 34-cd", EditorSearch.apply(source, matches));
    }
    @Test public void invalidRegexReplacementIsRejectedBeforeEditing() {
        assertThrows(IllegalArgumentException.class, () -> EditorSearch.find("x", "(x)", "$9", true, true, false, false, 0, 1));
    }
    @Test public void sourceEditorOmitsInheritedFoldingControls() {
        REditor editor = editor("block\n  body\nend\n");
        TextEditorPane pane = editor.textPane();
        runOnEdt(() -> {
            assertFalse(pane.isCodeFoldingEnabled());
            assertFalse(((RTextScrollPane) editor.getView()).isFoldIndicatorEnabled());
            assertFalse(Arrays.stream(pane.getPopupMenu().getComponents()).anyMatch(JMenu.class::isInstance));
            int shortcut = RTextArea.getDefaultModifier();
            for (int key : new int[]{KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT, KeyEvent.VK_DIVIDE, KeyEvent.VK_MULTIPLY}) {
                Object binding = pane.getInputMap().get(KeyStroke.getKeyStroke(key, shortcut));
                assertTrue(binding == null || !binding.toString().contains("Fold"));
            }
        });
    }

    @Test public void compilerHighlightingSurvivesOpeningSplittingAndUnsplit() throws Exception {
        Path source = files.newFile("generic.asm").toPath();
        Files.writeString(source, "nop\n");
        LexicalAnalyzer lexer = new TokenLexer(List.of(new SourceToken(Token.RESERVED, 0, "nop", null, null, null)));
        TabbedEditor workspace = onEdt(() -> new TabbedEditor(mock(Dialogs.class), new CompilerStub(lexer)));
        assertTrue(onEdt(() -> workspace.openFile(source)));
        REditor original = workspace.activeEditor();
        assertEquals(TokenTypes.RESERVED_WORD, onEdt(() -> original.textPane().getTokenListForLine(0).getType()).intValue());

        runOnEdt(() -> workspace.split(true));
        JSplitPane split = onEdt(() -> (JSplitPane) ((JTabbedPane) workspace.getView()).getSelectedComponent());
        TextEditorPane mirror = findComponent((java.awt.Container) split.getRightComponent(), TextEditorPane.class, pane -> true);
        assertEquals(TokenTypes.RESERVED_WORD, onEdt(() -> mirror.getTokenListForLine(0).getType()).intValue());

        runOnEdt(workspace::unsplit);
        assertEquals(TokenTypes.RESERVED_WORD, onEdt(() -> original.textPane().getTokenListForLine(0).getType()).intValue());
    }
    @Test public void commentToggleUsesAnUnknownCompilersDelimiter() {
        LexicalAnalyzer lexer = new TokenLexer(List.of()) {
            public String[] getLineCommentStartAndEnd() { return new String[]{"##", null}; }
        };
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(lexer)));
        runOnEdt(() -> { editor.textPane().setText("first\nsecond\n"); editor.textPane().select(0, 12); });
        command(editor, "Toggle comment");
        assertEquals("##first\n##second\n", onEdt(editor.textPane()::getText));
        command(editor, "Toggle comment");
        assertEquals("first\nsecond\n", onEdt(editor.textPane()::getText));
    }
    @Test public void commentShortcutDoesNotInsertTheSlashFromTheTypedEvent() {
        LexicalAnalyzer lexer = new TokenLexer(List.of()) {
            public String[] getLineCommentStartAndEnd() { return new String[]{";", null}; }
        };
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(lexer)));
        TextEditorPane pane = editor.textPane();
        String line = "dcx sp        ; stack initialization (0FFFFh)";
        int shortcut = RTextArea.getDefaultModifier();

        runOnEdt(() -> {
            pane.setText(line);
            pane.setCaretPosition(0);
            typeSlash(pane, shortcut);
            assertEquals(";" + line, pane.getText());
            typeSlash(pane, shortcut);
            assertEquals(line, pane.getText());

            pane.setCaretPosition(0);
            typeSlash(pane, 0);
            assertEquals("/" + line, pane.getText());
        });
    }

    private void typeSlash(TextEditorPane pane, int modifiers) {
        KeyboardFocusManager keyboard = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        long time = System.currentTimeMillis();
        keyboard.redispatchEvent(pane, new KeyEvent(pane, KeyEvent.KEY_PRESSED, time, modifiers, KeyEvent.VK_SLASH, '/'));
        keyboard.redispatchEvent(pane, new KeyEvent(pane, KeyEvent.KEY_TYPED, time, modifiers, KeyEvent.VK_UNDEFINED, '/'));
        keyboard.redispatchEvent(pane, new KeyEvent(pane, KeyEvent.KEY_RELEASED, time, modifiers, KeyEvent.VK_SLASH, '/'));
    }

    @Test public void sameFileSplitSharesDocumentAndKeepsTabCount() {
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null));
        REditor original = tabs.activeEditor();
        runOnEdt(() -> { original.textPane().setText("source"); tabs.split(true); });
        JTabbedPane view = (JTabbedPane) tabs.getView();
        assertEquals(1, onEdt(view::getTabCount).intValue());
        JSplitPane split = onEdt(() -> (JSplitPane) view.getSelectedComponent());
        assertTrue(onEdt(() -> split.getLeftComponent().isVisible()));
        assertTrue(onEdt(() -> split.getRightComponent().isVisible()));
        TextEditorPane second = findComponent((java.awt.Container) split.getRightComponent(), TextEditorPane.class, pane -> true);
        runOnEdt(() -> second.append(" edited"));
        assertEquals("source edited", onEdt(original.textPane()::getText));
        runOnEdt(tabs::unsplit);
        assertEquals(1, onEdt(view::getTabCount).intValue());
        runOnEdt(() -> {
            original.commands().selections().set(List.of(new MultipleSelection.Range(0, 0), new MultipleSelection.Range(6, 6)));
            original.textPane().replaceSelection("!");
        });
        assertEquals("!source! edited", onEdt(original.textPane()::getText));
    }
    @Test public void differentFilesSplitSideBySideAndTheirTabsRemainUsable() {
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null));
        runOnEdt(() -> { tabs.activeEditor().textPane().setText("first"); tabs.newFile(); tabs.activeEditor().textPane().setText("second"); });
        JFrame frame = showInFrame(tabs.getView());
        runOnEdt(() -> { frame.setSize(1000, 600); frame.setVisible(true); });
        SwingUtilities.invokeLater(() -> tabs.split(false));
        JDialog chooser = waitForWindow(JDialog.class, dialog -> "Split editor".equals(dialog.getTitle()));
        triggerButton(findButton(chooser, "OK"));
        JTabbedPane view = (JTabbedPane) tabs.getView();
        JSplitPane split = onEdt(() -> (JSplitPane) view.getSelectedComponent());
        assertTrue(onEdt(() -> split.getLeftComponent().isVisible()));
        assertTrue(onEdt(() -> split.getRightComponent().isVisible()));
        assertEquals(2, onEdt(view::getTabCount).intValue());
        runOnEdt(() -> { view.setSelectedIndex(0); tabs.unsplit(); });
        assertEquals("first", onEdt(tabs.activeEditor().textPane()::getText));
        assertEquals(0, onEdt(view::getSelectedIndex).intValue());
    }
    @Test public void changedFileAbortsEntireReplacementBatch() throws Exception {
        Path first = Files.writeString(files.newFile("first.asm").toPath(), "one"), second = Files.writeString(files.newFile("second.asm").toPath(), "one");
        SearchWorkspace.Snapshot a = new SearchWorkspace.Snapshot(first, null, "one"), b = new SearchWorkspace.Snapshot(second, null, "one");
        Files.writeString(second, "external");
        Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> edits = new LinkedHashMap<>();
        edits.put(a, List.of(new EditorSearch.Match(0, 3, "one", "two"))); edits.put(b, List.of(new EditorSearch.Match(0, 3, "one", "two")));
        assertThrows(java.io.IOException.class, () -> SearchWorkspace.apply(edits));
        assertEquals("one", Files.readString(first)); assertEquals("external", Files.readString(second));
    }
    @Test public void renameUsesSemanticScopesAndLeavesCommentsAndShadowedNamesAlone() throws Exception {
        String source = "name name name comment";
        LexicalAnalyzer support = new TokenLexer(List.of(new SourceToken(Token.SYMBOL_DEFINITION, 0, "name", "name", "", ""),
                new SourceToken(Token.SYMBOL_REFERENCE, 5, "name", "name", "", ""),
                new SourceToken(Token.SYMBOL_DEFINITION, 10, "name", "name", "local", "local"),
                new SourceToken(Token.COMMENT, 15, "comment", null, null, null)));
        SearchWorkspace.Snapshot snapshot = new SearchWorkspace.Snapshot(null, null, source);
        Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> edits = SourceRefactoring.rename(snapshot, List.of(snapshot), () -> support, 5, "other");
        assertEquals("other other name comment", EditorSearch.apply(source, edits.get(snapshot)));
    }
    @Test public void renameFollowsGenericIncludesAndPrefersUnsavedIncludedBuffers() throws Exception {
        Path rootPath = files.newFile("root.txt").toPath(), includedPath = files.newFile("included.txt").toPath();
        String rootText = "include name", childText = "name name";
        Files.writeString(includedPath, "disk");
        SearchWorkspace.Snapshot root = new SearchWorkspace.Snapshot(rootPath, null, rootText);
        SearchWorkspace.Snapshot child = new SearchWorkspace.Snapshot(includedPath, null, childText);
        LexicalAnalyzer support = new TokenLexer(List.of()) {
            public List<Token> analyze(String source) {
                if (source.equals(rootText)) return List.of(new SourceToken(Token.INCLUDE_PATH, 0, "include", null, "", null, "included.txt"),
                        new SourceToken(Token.SYMBOL_REFERENCE, 8, "name", "name", "", ""));
                assertEquals(childText, source);
                return List.of(new SourceToken(Token.SYMBOL_DEFINITION, 0, "name", "name", "", ""),
                        new SourceToken(Token.SYMBOL_REFERENCE, 5, "name", "name", "", ""));
            }
        };
        Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> changes = SourceRefactoring.rename(root, List.of(root, child), () -> support, 8, "other");
        assertEquals("include other", EditorSearch.apply(rootText, changes.get(root)));
        assertEquals("other other", EditorSearch.apply(childText, changes.get(child)));
    }
    @Test public void renameRejectsANameThatWouldCaptureAnUnresolvedReference() {
        String source = "name other";
        LexicalAnalyzer support = new TokenLexer(List.of(new SourceToken(Token.SYMBOL_DEFINITION, 0, "name", "name", "", ""),
                new SourceToken(Token.SYMBOL_REFERENCE, 5, "other", "other", "", "")));
        SearchWorkspace.Snapshot root = new SearchWorkspace.Snapshot(null, null, source);
        assertThrows(IllegalArgumentException.class, () -> SourceRefactoring.rename(root, List.of(root), () -> support, 0, "other"));
    }
    private static class TokenLexer implements LexicalAnalyzer {
        final List<Token> tokens; List<Token> current; int index;
        TokenLexer(List<Token> tokens) { this.tokens = tokens; }
        public Token next() { return current.get(index++); }
        public boolean hasNext() { return index < current.size(); }
        public void reset(char[] chars, int offset, int length) {
            index = 0;
            String text = new String(chars, offset, length);
            current = tokens.stream().filter(token -> token.getOffset() >= 0
                    && token.getOffset() + token.getText().length() <= length
                    && text.startsWith(token.getText(), token.getOffset())).collect(java.util.stream.Collectors.toList());
        }
        public boolean isValidSymbolName(String name) { return name.matches("[a-z]+"); }
    }
}
