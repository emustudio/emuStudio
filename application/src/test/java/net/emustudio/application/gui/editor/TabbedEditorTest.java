/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.FocusEvent;
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
    public void updatesTitleWhileTabHeaderIsAbsent() throws Exception {
        Path file = source("header.asm", "NOP");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(file), files -> { }));
        JTabbedPane tabs = (JTabbedPane) editor.getView();

        runOnEdt(() -> {
            Component header = tabs.getTabComponentAt(0);
            tabs.setTabComponentAt(0, null);
            try {
                assertTrue(editor.saveFile());
                assertEquals("header.asm", tabs.getTitleAt(0));
            } finally {
                tabs.setTabComponentAt(0, header);
            }
            editor.activeEditor().textPane().append("\nHLT");
        });

        assertEquals("header.asm*", onEdt(() -> tabs.getTitleAt(0)));
        assertEquals("header.asm*", findLabel((Container) onEdt(() -> tabs.getTabComponentAt(0)), "header.asm*").getText());
    }

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
        assertEquals("Untitled*", findLabel((Container) onEdt(() -> tabs.getTabComponentAt(0)), "Untitled*").getText());
        assertFalse(onEdt(editor::closeCurrent));
        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertTrue(onEdt(editor::closeCurrent));
        assertEquals("Untitled", onEdt(() -> tabs.getTitleAt(0)));
        verify(dialogs, times(2)).ask("File is not saved yet. Do you want to save it?");
    }

    @Test
    public void closeButtonsTargetTheirFileAfterTabIndicesShift() throws Exception {
        Path first = source("first.asm", "NOP");
        Path second = source("second.asm", "HLT");
        Path third = source("third.asm", "RET");
        List<List<Path>> persisted = new ArrayList<>();
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null,
                List.of(first, second, third), files -> persisted.add(List.copyOf(files))));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        JButton closeFirst = closeButton(tabs, 0);
        JButton closeSecond = closeButton(tabs, 1);

        triggerButton(closeFirst);
        assertEquals(2, onEdt(tabs::getTabCount).intValue());
        assertEquals(third, editor.getCurrentFile().orElseThrow().toPath());
        triggerButton(closeSecond);
        assertEquals(List.of(third), persisted.get(persisted.size() - 1));
        assertEquals("third.asm", onEdt(() -> tabs.getTitleAt(0)));

        triggerButton(closeButton(tabs, 0));
        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertEquals("Untitled", onEdt(() -> tabs.getTitleAt(0)));
        assertNotNull(closeButton(tabs, 0));
    }

    @Test
    public void closingDirtyInactiveTabCanBeCanceledOrSaved() throws Exception {
        Path first = source("first.asm", "NOP");
        Path second = source("second.asm", "HLT");
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.ask(anyString())).thenReturn(Dialogs.DialogAnswer.ANSWER_CANCEL, Dialogs.DialogAnswer.ANSWER_YES);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(first, second), files -> { }));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        REditor firstEditor = editor.editors().get(0);
        runOnEdt(() -> firstEditor.textPane().append("\nRET"));
        JButton close = closeButton(tabs, 0);

        triggerButton(close);
        assertEquals(2, onEdt(tabs::getTabCount).intValue());
        assertEquals(second, editor.getCurrentFile().orElseThrow().toPath());
        assertEquals("NOP", Files.readString(first));
        triggerButton(close);
        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertEquals(second, editor.getCurrentFile().orElseThrow().toPath());
        assertEquals("NOP\nRET", Files.readString(first));
    }

    @Test
    public void closeButtonClosesSplitDocumentAndRestoresOtherTab() throws Exception {
        Path first = source("first.asm", "NOP");
        Path second = source("second.asm", "HLT");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(first, second), files -> { }));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        runOnEdt(() -> editor.split(true));
        triggerButton(closeButton(tabs, 1));
        assertEquals(1, onEdt(tabs::getTabCount).intValue());
        assertEquals(first, editor.getCurrentFile().orElseThrow().toPath());
        assertSame(editor.activeEditor().getView(), onEdt(() -> tabs.getComponentAt(0)));
    }

    @Test
    public void extendedEditMenuPreservesStandardItemsAndUsesSelectedEditor() {
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(), files -> { }));
        REditor first = editor.activeEditor();
        runOnEdt(() -> {
            first.textPane().setText("first");
            editor.newFile();
            editor.activeEditor().textPane().setText("second");
        });
        JMenu menu = onEdt(() -> {
            JMenu edit = new JMenu("Edit");
            edit.add("Undo");
            editor.extendEditingMenu(edit);
            return edit;
        });
        int count = onEdt(menu::getMenuComponentCount);
        runOnEdt(() -> {
            for (int i = 0; i < 2; i++) {
                for (javax.swing.event.MenuListener listener : menu.getMenuListeners()) {
                    listener.menuSelected(new javax.swing.event.MenuEvent(menu));
                }
            }
        });
        assertEquals(count, onEdt(menu::getMenuComponentCount).intValue());
        assertEquals("Undo", onEdt(() -> menu.getItem(0).getText()));
        JMenu text = onEdt(() -> (JMenu) menu.getItem(2));
        assertEquals("Text", onEdt(text::getText));
        runOnEdt(() -> text.getItem(0).doClick(0));
        assertEquals("first", onEdt(() -> first.textPane().getText()));
        assertEquals("second\nsecond", onEdt(() -> editor.activeEditor().textPane().getText()));
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

    @Test
    public void tabVisitThenDebuggerVisitReturnsToTheSelectedFileInSourceHistory() throws Exception {
        Path first = source("a.asm", "nop\nhlt\n");
        Path second = source("b.asm", "nop\nret\n");
        Path third = source("c.asm", "nop\ninr a\n");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null,
                List.of(first, second), paths -> {}));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        runOnEdt(() -> {
            tabs.setSelectedIndex(0);
            editor.setPosition(SourceCodePosition.of(2, 0, first.toString()));
            tabs.setSelectedIndex(1); // No native-focus event needed to remember this visit.
            editor.visitPosition(SourceCodePosition.of(2, 0, third.toString()));
            JMenu menu = new JMenu("Edit");
            editor.extendEditingMenu(menu);
            JMenu navigate = (JMenu) java.util.Arrays.stream(menu.getMenuComponents())
                    .filter(c -> c instanceof JMenu && "Navigate".equals(((JMenu) c).getText())).findFirst().orElseThrow();
            for (Component component : navigate.getMenuComponents())
                if (component instanceof JMenuItem && "Back".equals(((JMenuItem) component).getText()))
                    ((JMenuItem) component).doClick(0);
        });
        assertEquals(second, onEdt(() -> editor.getCurrentFile().orElseThrow().toPath()));
    }

    @Test
    public void synchronizedPositionsKeepSplitAndDoNotEmitSourceFeedback() throws Exception {
        Path file = source("split.asm", "nop\ninr a\nhlt\n");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(file), paths -> {}));
        List<SourceCodePosition> selected = new ArrayList<>();
        runOnEdt(() -> {
            editor.setSourcePositionChanged(selected::add);
            editor.split(true);
        });
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        Component split = onEdt(tabs::getSelectedComponent);
        runOnEdt(() -> editor.synchronizePosition(SourceCodePosition.of(3, 0, file.toString())));
        assertSame(split, onEdt(tabs::getSelectedComponent));
        assertTrue(selected.isEmpty());
        assertEquals(3, onEdt(() -> editor.currentSourcePosition().orElseThrow().line).intValue());
    }

    @Test
    public void splitFocusAndDebuggerFollowingKeepTheSearchTargetOnTheActivePane() throws Exception {
        Path file = source("panes.asm", "nop\ninr a\nhlt\n");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(file), paths -> {}));
        runOnEdt(() -> editor.split(true));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        JSplitPane split = onEdt(() -> (JSplitPane) tabs.getSelectedComponent());
        TextEditorPane right = findComponent((Container) split.getRightComponent(), TextEditorPane.class, pane -> true);
        findLabel((Container) split.getLeftComponent(), "Left — panes.asm — Active");
        runOnEdt(() -> {
            for (java.awt.event.FocusListener listener : right.getFocusListeners())
                if (listener.getClass().getEnclosingClass() == TabbedEditor.class)
                    listener.focusGained(new FocusEvent(right, FocusEvent.FOCUS_GAINED));
        });
        findLabel((Container) split.getRightComponent(), "Right — panes.asm — Active");
        InlineSearchBar search = onEdt(editor::searchBar);
        runOnEdt(() -> { search.find.setText("inr"); search.next(true); });
        assertEquals("inr", onEdt(right::getSelectedText));
        List<SourceCodePosition> notifications = new ArrayList<>();
        runOnEdt(() -> {
            editor.setSourcePositionChanged(notifications::add);
            editor.synchronizePosition(SourceCodePosition.of(3, 0, file.toString()));
        });
        assertSame(split, onEdt(tabs::getSelectedComponent));
        assertEquals(2, onEdt(() -> right.getLineOfOffset(right.getCaretPosition())).intValue());
        assertTrue(notifications.isEmpty());
        runOnEdt(editor::unsplit);
    }

    @Test
    public void switchingTabsPreservesExistingSplit() throws Exception {
        Path splitFile = source("split.asm", "nop\n"), otherFile = source("other.asm", "hlt\n");
        TabbedEditor editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), null,
                List.of(splitFile, otherFile), paths -> {}));
        JTabbedPane tabs = (JTabbedPane) editor.getView();
        runOnEdt(() -> { tabs.setSelectedIndex(0); editor.split(true); });
        Component split = onEdt(tabs::getSelectedComponent);
        runOnEdt(() -> tabs.setSelectedIndex(1));
        assertEquals(otherFile, onEdt(() -> editor.getCurrentFile().orElseThrow().toPath()));
        runOnEdt(() -> tabs.setSelectedIndex(0));
        assertSame(split, onEdt(tabs::getSelectedComponent));
    }

    private Path source(String name, String text) throws Exception {
        Path file = temporaryFolder.newFile(name).toPath().toAbsolutePath();
        return Files.writeString(file, text);
    }

    private JButton closeButton(JTabbedPane tabs, int index) {
        return findButtonByTooltip((Container) onEdt(() -> tabs.getTabComponentAt(index)), "Close tab (Ctrl+W)");
    }
}
