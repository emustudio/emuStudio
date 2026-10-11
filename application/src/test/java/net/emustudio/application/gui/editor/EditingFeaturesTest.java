/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.plugins.compiler.*;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import javax.swing.*;
import java.awt.event.*;
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
    @Test public void selectionExpandsThroughBracketsAndShrinksBack() {
        REditor editor = editor("outer(inner(word))");
        runOnEdt(() -> editor.textPane().select(12, 16));
        command(editor, "Expand selection"); assertEquals("(word)", onEdt(editor::getSelectedText));
        command(editor, "Expand selection"); assertEquals("(inner(word))", onEdt(editor::getSelectedText));
        command(editor, "Shrink selection"); assertEquals("(word)", onEdt(editor::getSelectedText));
    }
    @Test public void workspaceSearchShortcutsUseTheInlineQueryAndWrap() {
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(), paths -> { }));
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
            pane.getActionMap().get(pane.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)))
                    .actionPerformed(new ActionEvent(pane, 0, ""));
        });
        assertEquals("aX\nbY", onEdt(pane::getText));
    }
    @Test public void recordedTypingCanBeReplayed() {
        REditor editor = editor(""); TextEditorPane pane = editor.textPane();
        command(editor, "Start recording");
        try {
            runOnEdt(() -> pane.getActionMap().get(javax.swing.text.DefaultEditorKit.defaultKeyTypedAction)
                    .actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, "x")));
        } finally { command(editor, "Stop recording"); }
        command(editor, "Replay recording");
        assertEquals("xx", onEdt(pane::getText));
    }
    @Test public void rectangularSelectionClampsShortLinesAndEditsRows() {
        REditor editor = editor("abc\na\nabc"); TextEditorPane pane = editor.textPane();
        runOnEdt(() -> { editor.commands().selections().rectangle(1, 8); pane.replaceSelection("!"); });
        assertEquals("a!c\na!\na!c", onEdt(pane::getText));
    }
    @Test public void selectingAllOccurrencesCreatesEditableSelections() {
        REditor editor = editor("same same same");
        runOnEdt(() -> editor.textPane().select(0, 4)); command(editor, "Select all occurrences");
        runOnEdt(() -> editor.textPane().replaceSelection("x"));
        assertEquals("x x x", onEdt(editor.textPane()::getText));
    }
    @Test public void transformsOnlySelectedLinesAndPreservesFinalNewline() {
        REditor editor = editor("z\nb\na\nz\n");
        runOnEdt(() -> editor.textPane().select(2, 6)); command(editor, "Sort lines");
        assertEquals("z\na\nb\nz\n", onEdt(editor.textPane()::getText));
        runOnEdt(() -> editor.textPane().setCaretPosition(0)); command(editor, "Remove duplicate lines");
        assertEquals("z\na\nb\n", onEdt(editor.textPane()::getText));
    }
    @Test public void indentConversionsRespectTabStops() {
        assertEquals("a   b\n    c", TextEdits.spaces("a\tb\n\tc", 4));
        assertEquals("\t  x\ny", TextEdits.tabs("      x\ny", 4));
    }
    @Test public void selectionSearchReplaceNeverTouchesOutsideScope() {
        REditor editor = editor("one one one"); InlineSearchBar bar = onEdt(() -> new InlineSearchBar(() -> editor, () -> { }));
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
    @Test public void bookmarksAndPreferencesRestoreThroughExistingSettings() throws Exception {
        Path source = Files.writeString(files.newFile("source.asm").toPath(), "first\nsecond\n");
        AppSettings settings = new AppSettings(Config.inMemory(), false, false);
        REditor first = editor("");
        runOnEdt(() -> { first.configure(new EditorState(settings)); first.openFile(source); first.textPane().setCaretPosition(6); });
        command(first, "Toggle bookmark");
        REditor second = editor("");
        runOnEdt(() -> { second.configure(new EditorState(settings)); second.openFile(source); });
        assertTrue(onEdt(() -> second.commands().bookmarks().containsKey(1)));
    }
    @Test public void foldingUsesUnknownCompilerBlockTokens() {
        LexicalAnalyzer lexer = new TokenLexer(List.of(new SourceToken(Token.BLOCK_START, 0, "anything", null, null, null),
                new SourceToken(Token.BLOCK_END, 16, "done", null, null, null)));
        REditor editor = onEdt(() -> new REditor(mock(Dialogs.class), new CompilerStub(lexer)));
        runOnEdt(() -> editor.textPane().setText("anything\n  body\ndone\n"));
        assertTrue(onEdt(() -> editor.textPane().getParserCount() > 0));
        List<Fold> folds = onEdt(() -> new SourceFoldParser().getFolds(editor.textPane()));
        assertEquals(1, folds.size()); assertEquals(0, folds.get(0).getStartLine()); assertEquals(2, folds.get(0).getEndLine());
    }
    @Test public void compilerFoldingSurvivesOpeningSplittingAndSyntaxChanges() throws Exception {
        Path source = files.newFile("generic.asm").toPath();
        Files.writeString(source, "anything\n  body\ndone\n");
        LexicalAnalyzer lexer = new TokenLexer(List.of(new SourceToken(Token.BLOCK_START, 0, "anything", null, null, null),
                new SourceToken(Token.BLOCK_END, 16, "done", null, null, null)));
        TabbedEditor workspace = onEdt(() -> new TabbedEditor(mock(Dialogs.class), new CompilerStub(lexer), List.of(source), paths -> { }));
        REditor original = workspace.activeEditor();
        runOnEdt(() -> original.textPane().getFoldManager().reparse());
        assertEquals(1, onEdt(() -> original.textPane().getFoldManager().getFoldCount()).intValue());

        runOnEdt(() -> workspace.split(true));
        JSplitPane split = onEdt(() -> (JSplitPane) ((JTabbedPane) workspace.getView()).getSelectedComponent());
        TextEditorPane mirror = findComponent((java.awt.Container) split.getRightComponent(), TextEditorPane.class, pane -> true);
        runOnEdt(() -> mirror.getFoldManager().reparse());
        assertEquals(1, onEdt(() -> mirror.getFoldManager().getFoldCount()).intValue());
        assertEquals(FoldSupport.COMPILER_STYLE, onEdt(mirror::getSyntaxEditingStyle));
        assertEquals(FoldSupport.COMPILER_STYLE, onEdt(original.textPane()::getSyntaxEditingStyle));

        runOnEdt(() -> {
            workspace.unsplit();
            JMenu display = original.commands().menus().get(4);
            JMenu syntax = (JMenu) display.getItem(display.getItemCount() - 1);
            syntax.getItem(1).doClick(0);
            syntax.getItem(0).doClick(0);
            original.textPane().getFoldManager().reparse();
        });
        assertEquals(1, onEdt(() -> original.textPane().getFoldManager().getFoldCount()).intValue());
        assertEquals(FoldSupport.COMPILER_STYLE, onEdt(() -> ((org.fife.ui.rsyntaxtextarea.RSyntaxDocument) original.textPane().getDocument()).getSyntaxStyle()));
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
    @Test public void foldingFallsBackToIndentationWhenAPluginRejectsIncompleteSource() {
        LexicalAnalyzer lexer = new TokenLexer(List.of()) {
            public void reset(char[] chars, int offset, int length) { throw new IllegalArgumentException("incomplete"); }
        };
        REditor editor = editor("heading\n  body\nnext\n");
        runOnEdt(() -> editor.textPane().putClientProperty("sourceLexer", (java.util.function.Supplier<LexicalAnalyzer>) () -> lexer));
        List<Fold> folds = onEdt(() -> new SourceFoldParser().getFolds(editor.textPane()));
        assertEquals(1, folds.size()); assertEquals(1, folds.get(0).getEndLine());
    }
    @Test public void sameFileSplitSharesDocumentAndKeepsTabCount() {
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(), paths -> { }));
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
        TabbedEditor tabs = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(), paths -> { }));
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
    @Test public void folderSearchFiltersFilesAndUsesUnsavedBuffer() throws Exception {
        Path root = files.getRoot().toPath(), file = Files.writeString(files.newFile("code.asm").toPath(), "disk");
        Files.writeString(files.newFile("skip.txt").toPath(), "skip");
        REditor editor = editor(""); runOnEdt(() -> { editor.openFile(file); editor.textPane().setText("unsaved"); });
        List<SearchWorkspace.Snapshot> snapshots = SearchWorkspace.folder(root, "*.asm", "", onEdt(() -> SearchWorkspace.open(List.of(editor))));
        assertEquals(1, snapshots.size()); assertEquals("unsaved", snapshots.get(0).text);
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
