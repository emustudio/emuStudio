/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class REditorTest extends AbstractSwingTest {

    @Test
    public void compilerBackedEditorUsesDirectTokenMakerIntegration() {
        Dialogs dialogs = mock(Dialogs.class);
        REditor editor = createEditor(dialogs, compiler(
                lexerFor(
                        token(net.emustudio.emulib.plugins.compiler.Token.RESERVED, 0, "MOV"),
                        token(net.emustudio.emulib.plugins.compiler.Token.EOF, 3, "")
                ),
                List.of(new FileExtension("asm", "Assembly"))
        ));

        TextEditorPane textPane = textPane(editor);
        runOnEdt(() -> textPane.setText("MOV"));

        Token token = onEdt(() -> textPane.getTokenListForLine(0));

        assertEquals(Token.RESERVED_WORD, token.getType());
        assertEquals("MOV", token.getLexeme());
        assertTrue(editor.getView() instanceof RTextScrollPane);
    }

    @Test
    public void saveFileOnNewEditorUsesDialogPathAndWritesContent() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Path output = Files.createTempDirectory("reditor-save-new").resolve("program.asm");
        when(dialogs.chooseFile(eq("Save file"), eq("Save"), any(Path.class), eq(true), anyList()))
                .thenReturn(Optional.of(output));

        REditor editor = createEditor(dialogs, compiler(
                lexerFor(token(net.emustudio.emulib.plugins.compiler.Token.EOF, 0, "")),
                List.of(new FileExtension("asm", "Assembly"))
        ));

        runOnEdt(() -> textPane(editor).setText("hello"));

        assertTrue(editor.saveFile());
        assertEquals("hello", Files.readString(output));
        assertEquals(output.toFile(), editor.getCurrentFile().orElseThrow());

        ArgumentCaptor<Path> baseDirectory = ArgumentCaptor.forClass(Path.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> filters = ArgumentCaptor.forClass(List.class);
        verify(dialogs).chooseFile(eq("Save file"), eq("Save"), baseDirectory.capture(), eq(true), filters.capture());
        assertEquals(Path.of(System.getProperty("user.dir")), baseDirectory.getValue());
        assertEquals(1, filters.getValue().size());
    }

    @Test
    public void saveFilePersistsChangesToCurrentlyOpenedWritableFile() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Path file = Files.createTempFile("reditor-save", ".asm");
        Files.writeString(file, "old");

        REditor editor = createEditor(dialogs, null);
        assertTrue(editor.openFile(file));

        runOnEdt(() -> textPane(editor).setText("updated"));

        assertTrue(editor.isDirty());
        assertTrue(editor.saveFile());
        assertEquals("updated", Files.readString(file));
        assertEquals(file.toFile(), editor.getCurrentFile().orElseThrow());
    }

    @Test
    public void saveFileAsUsesCurrentFileParentDirectoryForExistingFile() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Path directory = Files.createTempDirectory("reditor-save-as");
        Path opened = directory.resolve("opened.asm");
        Path saved = directory.resolve("saved.asm");
        Files.writeString(opened, "source");
        when(dialogs.chooseFile(eq("Save file"), eq("Save"), eq(directory), eq(true), anyList()))
                .thenReturn(Optional.of(saved));

        REditor editor = createEditor(dialogs, compiler(
                lexerFor(token(net.emustudio.emulib.plugins.compiler.Token.EOF, 0, "")),
                List.of(
                        new FileExtension("asm", "Assembly"),
                        new FileExtension("inc", "Include")
                )
        ));
        assertTrue(editor.openFile(opened));
        runOnEdt(() -> textPane(editor).setText("saved content"));

        assertTrue(editor.saveFileAs());
        assertEquals("saved content", Files.readString(saved));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> filters = ArgumentCaptor.forClass(List.class);
        verify(dialogs).chooseFile(eq("Save file"), eq("Save"), eq(directory), eq(true), filters.capture());
        assertEquals(2, filters.getValue().size());
    }

    @Test
    public void openFileUsesCompilerFiltersAndLoadsSelectedFile() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Path directory = Files.createTempDirectory("reditor-open");
        Path current = directory.resolve("current.asm");
        Path target = directory.resolve("target.asm");
        Files.writeString(current, "current");
        Files.writeString(target, "target content");
        when(dialogs.chooseFile(eq("Open a file"), eq("Open"), eq(directory), eq(false), anyList()))
                .thenReturn(Optional.of(target));

        REditor editor = createEditor(dialogs, compiler(
                lexerFor(token(net.emustudio.emulib.plugins.compiler.Token.EOF, 0, "")),
                List.of(
                        new FileExtension("asm", "Assembly"),
                        new FileExtension("inc", "Include")
                )
        ));
        assertTrue(editor.openFile(current));

        assertTrue(editor.openFile());
        assertEquals("target content", onEdt(() -> textPane(editor).getText()));
        assertEquals(target.toFile(), editor.getCurrentFile().orElseThrow());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> filters = ArgumentCaptor.forClass(List.class);
        verify(dialogs).chooseFile(eq("Open a file"), eq("Open"), eq(directory), eq(false), filters.capture());
        assertEquals(1, filters.getValue().size());
        FileExtensionsFilter filter = (FileExtensionsFilter) filters.getValue().get(0);
        assertEquals("All source files", filter.getDescription());
        assertEquals(List.of("asm", "inc"), filter.getExtensions());
    }

    @Test
    public void openFileFailureShowsErrorAndNewFileClearsEditorState() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        REditor editor = createEditor(dialogs, null);
        TextEditorPane textPane = textPane(editor);
        Path directory = Files.createTempDirectory("reditor-missing");

        runOnEdt(() -> textPane.setText("dirty"));
        assertFalse(editor.openFile(directory));
        verify(dialogs).showError("Could not open file: " + directory + ". Please see log file for details.");

        editor.newFile();
        assertEquals("", onEdt(textPane::getText));
        assertFalse(editor.isDirty());
        assertTrue(editor.getCurrentFile().isEmpty());
    }

    @Test
    public void cancelledFileDialogsReturnFalseAndUseEmptyOpenFiltersWithoutCompiler() {
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.chooseFile(eq("Save file"), eq("Save"), any(Path.class), eq(true), anyList()))
                .thenReturn(Optional.empty());
        when(dialogs.chooseFile(eq("Open a file"), eq("Open"), any(Path.class), eq(false), anyList()))
                .thenReturn(Optional.empty());

        REditor editor = createEditor(dialogs, null);

        assertFalse(editor.saveFileAs());
        assertFalse(editor.openFile());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> filters = ArgumentCaptor.forClass(List.class);
        verify(dialogs).chooseFile(eq("Open a file"), eq("Open"), any(Path.class), eq(false), filters.capture());
        assertTrue(filters.getValue().isEmpty());
    }

    @Test
    public void searchOperationsCaretPositionAndSelectionBehaveAsExpected() {
        Dialogs dialogs = mock(Dialogs.class);
        REditor editor = createEditor(dialogs, null);
        TextEditorPane textPane = textPane(editor);
        runOnEdt(() -> textPane.setText("alpha beta alpha"));

        assertTrue(editor.findNext().isEmpty());
        assertTrue(editor.findPrevious().isEmpty());

        SearchContext markAll = new SearchContext("alpha");
        markAll.setMarkAll(true);
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.MARK_ALL, markAll)));
        assertEquals(2, onEdt(() -> textPane.getMarkAllHighlightRanges().size()).intValue());
        runOnEdt(editor::clearMarkedOccurences);

        SearchContext find = new SearchContext("alpha");
        find.setSearchForward(true);
        runOnEdt(() -> textPane.setCaretPosition(0));
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.FIND, find)));
        assertEquals("alpha", onEdt(editor::getSelectedText));
        assertTrue(onEdt(editor::findNext).orElse(false));
        assertTrue(onEdt(editor::findPrevious).orElse(false));

        SearchContext replace = new SearchContext("beta");
        replace.setReplaceWith("gamma");
        replace.setSearchForward(true);
        runOnEdt(() -> textPane.setCaretPosition(0));
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.REPLACE, replace)));
        assertEquals("alpha gamma alpha", onEdt(textPane::getText));

        SearchContext replaceAll = new SearchContext("alpha");
        replaceAll.setReplaceWith("omega");
        replaceAll.setSearchForward(true);
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.REPLACE_ALL, replaceAll)));
        assertEquals("omega gamma omega", onEdt(textPane::getText));
        verify(dialogs).showInfo("2 occurrences replaced.", "Replace all");

        runOnEdt(() -> editor.setPosition(SourceCodePosition.of(1, 6, "test.asm")));
        assertEquals(6, onEdt(textPane::getCaretPosition).intValue());
        runOnEdt(() -> editor.setPosition(SourceCodePosition.of(-1, 0, "test.asm")));
        assertEquals(6, onEdt(textPane::getCaretPosition).intValue());

        runOnEdt(editor::grabFocus);
    }

    @Test
    public void searchOperationsHandleMissingMatchesWithoutChangingText() {
        Dialogs dialogs = mock(Dialogs.class);
        REditor editor = createEditor(dialogs, null);
        TextEditorPane textPane = textPane(editor);
        runOnEdt(() -> {
            textPane.setText("alpha");
            textPane.setCaretPosition(0);
        });

        SearchContext find = new SearchContext("missing");
        find.setSearchForward(true);
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.FIND, find)));
        assertNull(onEdt(editor::getSelectedText));

        SearchContext replace = new SearchContext("missing");
        replace.setReplaceWith("beta");
        replace.setSearchForward(true);
        runOnEdt(() -> editor.searchEvent(new SearchEvent(this, SearchEvent.Type.REPLACE, replace)));
        assertEquals("alpha", onEdt(textPane::getText));
    }

    @Test
    public void setPositionSupportsNegativeColumnAndIgnoresInvalidLine() {
        REditor editor = createEditor(mock(Dialogs.class), null);
        TextEditorPane textPane = textPane(editor);
        runOnEdt(() -> textPane.setText("alpha\nbeta"));

        runOnEdt(() -> editor.setPosition(SourceCodePosition.of(2, -1, "test.asm")));
        assertEquals(onEdt(() -> {
            try {
                return textPane.getLineStartOffset(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).intValue(), onEdt(textPane::getCaretPosition).intValue());

        runOnEdt(() -> editor.setPosition(SourceCodePosition.of(99, 0, "test.asm")));
        assertEquals(6, onEdt(textPane::getCaretPosition).intValue());
    }

    @Test
    public void ctrlMouseWheelZoomsAndEscapeTriggersClearAction() {
        Dialogs dialogs = mock(Dialogs.class);
        TrackingEditor editor = createTrackingEditor(dialogs);
        TextEditorPane textPane = textPane(editor);
        RTextScrollPane scrollPane = scrollPane(editor);

        float originalSize = onEdt(() -> textPane.getFont().getSize2D());
        fireMouseWheel(scrollPane, MouseWheelEvent.CTRL_DOWN_MASK, -1);
        assertEquals(originalSize + 1.0f, onEdt(() -> textPane.getFont().getSize2D()), 0.01f);

        fireMouseWheel(scrollPane, MouseWheelEvent.CTRL_DOWN_MASK, 1);
        assertEquals(originalSize, onEdt(() -> textPane.getFont().getSize2D()), 0.01f);

        fireMouseWheel(scrollPane, MouseWheelEvent.CTRL_DOWN_MASK, 0);
        assertEquals(originalSize, onEdt(() -> textPane.getFont().getSize2D()), 0.01f);

        fireMouseWheel(scrollPane, 0, 1);
        assertEquals(originalSize, onEdt(() -> textPane.getFont().getSize2D()), 0.01f);

        KeyEvent escape = new KeyEvent(textPane, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        KeyEvent other = new KeyEvent(textPane, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');
        runOnEdt(() -> {
            for (java.awt.event.KeyListener listener : textPane.getKeyListeners()) {
                listener.keyPressed(other);
                listener.keyPressed(escape);
            }
        });
        assertEquals(1, editor.clearCalls);
    }

    private REditor createEditor(Dialogs dialogs, Compiler compiler) {
        return onEdt(() -> compiler == null ? new REditor(dialogs) : new REditor(dialogs, compiler));
    }

    private TrackingEditor createTrackingEditor(Dialogs dialogs) {
        return onEdt(() -> new TrackingEditor(dialogs));
    }

    private TextEditorPane textPane(REditor editor) {
        return getField(editor, "textPane", TextEditorPane.class);
    }

    private RTextScrollPane scrollPane(REditor editor) {
        return getField(editor, "scrollPane", RTextScrollPane.class);
    }

    private <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = REditor.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void fireMouseWheel(RTextScrollPane scrollPane, int modifiersEx, int rotation) {
        MouseWheelEvent event = new MouseWheelEvent(
                scrollPane,
                MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                modifiersEx,
                10,
                10,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                rotation
        );
        runOnEdt(() -> {
            for (MouseWheelListener listener : scrollPane.getMouseWheelListeners()) {
                listener.mouseWheelMoved(event);
            }
        });
    }

    private Compiler compiler(LexicalAnalyzer lexer, List<FileExtension> fileExtensions) {
        return new CompilerStub(lexer, fileExtensions);
    }

    private LexicalAnalyzer lexerFor(net.emustudio.emulib.plugins.compiler.Token... tokens) {
        return new TrackingLexicalAnalyzer(tokens);
    }

    private static net.emustudio.emulib.plugins.compiler.Token token(int type, int offset, String text) {
        return new net.emustudio.emulib.plugins.compiler.Token() {
            @Override
            public int getType() {
                return type;
            }

            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }

    private static final class TrackingEditor extends REditor {
        private int clearCalls;

        private TrackingEditor(Dialogs dialogs) {
            super(dialogs);
        }

        @Override
        public void clearMarkedOccurences() {
            clearCalls++;
            super.clearMarkedOccurences();
        }
    }
}
