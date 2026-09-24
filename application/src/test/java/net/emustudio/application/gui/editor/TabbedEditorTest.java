/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TabbedEditorTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void opensFilesInTabsAndRoutesToSelectedTab() throws Exception {
        Path first = source("first.asm", "NOP");
        Path second = source("second.asm", "HLT");
        List<List<Path>> persisted = new ArrayList<>();
        TabbedEditor editor = onEdt(() -> new TabbedEditor(
                mock(Dialogs.class), null, List.of(), files -> persisted.add(List.copyOf(files))
        ));

        assertTrue(onEdt(() -> editor.openFile(first)));
        assertTrue(onEdt(() -> editor.openFile(second)));

        JTabbedPane tabs = (JTabbedPane) editor.getView();
        assertEquals(2, onEdt(tabs::getTabCount).intValue());
        assertEquals("second.asm", onEdt(() -> tabs.getTitleAt(1)));
        assertEquals(second.toAbsolutePath(), editor.getCurrentFile().orElseThrow().toPath());
        assertEquals(List.of(first.toAbsolutePath(), second.toAbsolutePath()), persisted.get(persisted.size() - 1));

        runOnEdt(() -> tabs.setSelectedIndex(0));
        assertEquals(first.toAbsolutePath(), editor.getCurrentFile().orElseThrow().toPath());
    }

    @Test
    public void marksDirtyTabsAndPromptsBeforeClose() {
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.ask("File is not saved yet. Do you want to save it?"))
                .thenReturn(Dialogs.DialogAnswer.ANSWER_CANCEL, Dialogs.DialogAnswer.ANSWER_NO);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {
        }));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        TextEditorPane textPane = findComponent(
                (Container) onEdt(tabs::getSelectedComponent), TextEditorPane.class, pane -> true
        );

        runOnEdt(() -> textPane.setText("NOP"));

        assertEquals("Untitled*", onEdt(() -> tabs.getTitleAt(0)));
        assertFalse(onEdt(editor::closeCurrent));
        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertTrue(onEdt(editor::closeCurrent));
        assertEquals("Untitled", onEdt(() -> tabs.getTitleAt(0)));
        verify(dialogs, times(2)).ask("File is not saved yet. Do you want to save it?");
    }

    @Test
    public void restoresExistingFilesAndSkipsMissingOnes() throws Exception {
        Path existing = source("existing.asm", "NOP");
        Path missing = temporaryFolder.getRoot().toPath().resolve("missing.asm");

        TabbedEditor editor = onEdt(() -> new TabbedEditor(
                mock(Dialogs.class), null, List.of(existing, missing), files -> {
                }
        ));
        JTabbedPane tabs = (JTabbedPane) editor.getView();

        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertEquals("existing.asm", onEdt(() -> tabs.getTitleAt(0)));
    }

    @Test
    public void ctrlClickingIncludeOpensItInNewTab() throws Exception {
        Path include = source("library.asm", "NOP");
        Path main = source("main.asm", "include \"library.asm\"");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(main), files -> {
        }));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        showInFrame(tabs);
        TextEditorPane textPane = findComponent(
                (Container) onEdt(tabs::getSelectedComponent), TextEditorPane.class, pane -> true
        );
        int offset = "include \"".length();
        Rectangle2D location = onEdt(() -> textPane.modelToView2D(offset));

        runOnEdt(() -> textPane.dispatchEvent(new MouseEvent(
                textPane,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                InputEvent.CTRL_DOWN_MASK,
                (int) location.getCenterX(),
                (int) location.getCenterY(),
                1,
                false,
                MouseEvent.BUTTON1
        )));

        assertEquals(2, onEdt(tabs::getTabCount).intValue());
        assertEquals("library.asm", onEdt(() -> tabs.getTitleAt(1)));
        assertEquals(include, editor.getCurrentFile().orElseThrow().toPath());
    }

    private Path source(String name, String text) throws Exception {
        Path file = temporaryFolder.newFile(name).toPath().toAbsolutePath();
        return Files.writeString(file, text);
    }
}
