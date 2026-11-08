/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.editor.TabbedEditor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Test;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Optional;

import static net.emustudio.application.Constants.FONT_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EditorPanelTest extends AbstractSwingTest {

    @Test
    public void inlineSearchLeavesMostOfThePanelForSourceEditing() {
        Dialogs dialogs = mock(Dialogs.class);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {}));
        EditorPanel panel = createPanel(dialogs, editor);
        JFrame frame = showInFrame(panel);
        try {
            runOnEdt(() -> {
                findComponent(panel, TextEditorPane.class, text -> true).setText("NOP\nHLT\n");
                frame.setSize(1000, 650);
                frame.validate();
            });
            assertTrue(onEdt(() -> editor.getView().getHeight() > 300));
            runOnEdt(() -> { editor.searchBar().showReplace(); frame.validate(); });
            assertTrue(onEdt(() -> editor.searchBar().getHeight() < 120));
            assertTrue(onEdt(() -> editor.getView().getHeight() > 300));
        } finally { runOnEdt(panel::dispose); }
    }

    @Test
    public void findShowsOneRowAndReplacementCanBeRevealedWithoutLosingTheQuery() {
        Dialogs dialogs = mock(Dialogs.class);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {}));
        EditorPanel panel = createPanel(dialogs, editor);
        JFrame frame = showInFrame(panel);
        try {
            runOnEdt(() -> panel.getFindAction().actionPerformed(new ActionEvent(panel, 0, "Find")));
            JTextField find = findComponent(panel, JTextField.class,
                    field -> "Find text".equals(field.getAccessibleContext().getAccessibleName()));
            JTextField replacement = findComponent(panel, JTextField.class,
                    field -> "Replacement text".equals(field.getAccessibleContext().getAccessibleName()));
            assertFalse(onEdt(replacement::isVisible));
            int findHeight = onEdt(() -> { frame.validate(); return editor.searchBar().getHeight(); });
            setText(find, "counter");
            triggerButton(findComponent(panel, JToggleButton.class,
                    button -> "Show replacement controls".equals(button.getToolTipText())));
            assertTrue(onEdt(replacement::isVisible));
            assertEquals("counter", onEdt(find::getText));
            assertTrue(onEdt(() -> { frame.validate(); return editor.searchBar().getHeight(); }) > findHeight);
            runOnEdt(() -> panel.getFindAction().actionPerformed(new ActionEvent(panel, 0, "Find")));
            assertFalse(onEdt(replacement::isVisible));
            runOnEdt(() -> panel.getReplaceAction().actionPerformed(new ActionEvent(panel, 0, "Replace")));
            assertTrue(onEdt(replacement::isVisible));
            assertEquals("counter", onEdt(find::getText));
        } finally { runOnEdt(panel::dispose); }
    }

    @Test
    public void fontZoomComboTracksShortcutsWheelAndActiveTabs() {
        Dialogs dialogs = mock(Dialogs.class);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {}));
        EditorPanel panel = createPanel(dialogs, editor);
        try {
            JComboBox<?> zoom = findComponent(panel, JComboBox.class,
                    component -> "Font zoom".equals(component.getAccessibleContext().getAccessibleName()));
            TextEditorPane text = findComponent(panel, TextEditorPane.class, component -> true);
            int baseSize = FONT_CODE.getSize();

            assertTrue(onEdt(zoom::isEditable));
            assertEquals("100%", onEdt(zoom::getSelectedItem));
            runOnEdt(() -> zoom.setSelectedItem("200%"));
            assertEquals(baseSize * 2, onEdt(editor::getFontSize).intValue());
            assertEquals("200%", onEdt(zoom::getSelectedItem));

            for (KeyStroke key : List.of(
                    KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK),
                    KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                    KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK))) {
                invokeKeyBinding(text, key);
            }
            assertEquals(baseSize * 2 + 4, onEdt(editor::getFontSize), 0.001f);
            invokeKeyBinding(text, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
            invokeKeyBinding(text, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK));
            assertEquals(baseSize * 2 + 2, onEdt(editor::getFontSize), 0.001f);
            assertEquals(Math.round((baseSize * 2 + 2) * 100.0f / baseSize) + "%", onEdt(zoom::getSelectedItem));

            JScrollPane scroll = (JScrollPane) ((JTabbedPane) editor.getView()).getSelectedComponent();
            runOnEdt(() -> {
                zoom.setSelectedItem("350%");
                MouseWheelEvent wheel = new MouseWheelEvent(scroll, MouseWheelEvent.MOUSE_WHEEL,
                        System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK, 0, 0, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1);
                for (java.awt.event.MouseWheelListener listener : scroll.getMouseWheelListeners()) {
                    listener.mouseWheelMoved(wheel);
                }
            });
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);
            assertEquals("350%", onEdt(zoom::getSelectedItem));
            invokeKeyBinding(text, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK));
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);

            runOnEdt(editor::newFile);
            assertEquals("100%", onEdt(zoom::getSelectedItem));
            runOnEdt(() -> ((JTabbedPane) editor.getView()).setSelectedIndex(0));
            assertEquals("350%", onEdt(zoom::getSelectedItem));
            runOnEdt(editor::closeCurrent);
            assertEquals("100%", onEdt(zoom::getSelectedItem));
            runOnEdt(() -> zoom.setSelectedItem("200%"));
            runOnEdt(editor::closeCurrent);
            assertEquals("100%", onEdt(zoom::getSelectedItem));
            runOnEdt(() -> editor.setFontSize(0));
            assertEquals(baseSize * 0.3f, onEdt(editor::getFontSize), 0.001f);
            assertEquals("30%", onEdt(zoom::getSelectedItem));
            TextEditorPane currentText = findComponent(panel, TextEditorPane.class, component -> true);
            invokeKeyBinding(currentText, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
            assertEquals(baseSize * 0.3f, onEdt(editor::getFontSize), 0.001f);
            runOnEdt(() -> editor.setFontSize(1000));
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);
            assertEquals("350%", onEdt(zoom::getSelectedItem));
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    public void customFontZoomAcceptsPercentagesAndRestoresInvalidInput() {
        Dialogs dialogs = mock(Dialogs.class);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {}));
        EditorPanel panel = createPanel(dialogs, editor);
        try {
            JComboBox<?> zoom = findComponent(panel, JComboBox.class,
                    component -> "Font zoom".equals(component.getAccessibleContext().getAccessibleName()));
            JTextField input = onEdt(() -> (JTextField) zoom.getEditor().getEditorComponent());
            for (String value : List.of("113", " 113 % ", "", "abc", "NaN", "100%%", "99999999999999999")) {
                runOnEdt(() -> {
                    input.setText(value);
                    input.postActionEvent();
                });
                assertEquals(FONT_CODE.getSize2D() * 1.13f, onEdt(editor::getFontSize), 0.001f);
                assertEquals("113%", onEdt(zoom::getSelectedItem));
                assertEquals("113%", onEdt(input::getText));
            }
            runOnEdt(() -> {
                input.setText("10");
                input.postActionEvent();
            });
            assertEquals("30%", onEdt(input::getText));
            runOnEdt(() -> {
                input.setText("500%");
                input.postActionEvent();
            });
            assertEquals("350%", onEdt(input::getText));
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    private void invokeKeyBinding(TextEditorPane text, KeyStroke key) {
        runOnEdt(() -> text.getActionMap().get(text.getInputMap().get(key))
                .actionPerformed(new ActionEvent(text, ActionEvent.ACTION_PERFORMED, "")));
    }

    @Test
    public void confirmSaveSavesDirtyEditorWhenUserApproves() {
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.ask("File is not saved yet. Do you want to save it?", "Confirmation"))
                .thenReturn(Dialogs.Answer.YES);

        Editor editor = createEditor(true, true);
        EditorPanel panel = createPanel(dialogs, editor);

        try {
            assertTrue(panel.confirmSave());
            verify(editor).saveFile();
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    public void confirmSaveReturnsFalseWhenUserCancels() {
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.ask("File is not saved yet. Do you want to save it?", "Confirmation"))
                .thenReturn(Dialogs.Answer.CANCEL);

        Editor editor = createEditor(true, true);
        EditorPanel panel = createPanel(dialogs, editor);

        try {
            assertFalse(panel.confirmSave());
            verify(editor, never()).saveFile();
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    public void draggingCompilerDividerResizesEditorOnlyOnRelease() {
        EditorPanel panel = createPanel(mock(Dialogs.class), createEditor(false, true));
        JFrame frame = showInFrame(panel);
        JSplitPane split = findComponent(panel, JSplitPane.class, component -> true);
        try {
            runOnEdt(() -> {
                frame.setSize(1000, 650);
                frame.validate();
                split.setDividerLocation(350);
                frame.validate();
                BasicSplitPaneDivider divider = ((BasicSplitPaneUI) split.getUI()).getDivider();
                int height = split.getTopComponent().getHeight();
                int location = split.getDividerLocation();
                int x = divider.getWidth() / 2, y = divider.getHeight() / 2;
                long time = System.currentTimeMillis();
                divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_PRESSED,
                        time, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                for (int distance : new int[]{20, 40, 60}) {
                    int dragY = location - distance - divider.getY() + y;
                    divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_DRAGGED,
                            time + distance, MouseEvent.BUTTON1_DOWN_MASK, x, dragY, 0, false, MouseEvent.NOBUTTON));
                    frame.validate();
                    assertEquals("Dragging must not repeatedly resize the source editor",
                            height, split.getTopComponent().getHeight());
                }
                divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_RELEASED,
                        time + 61, 0, x, location - 60 - divider.getY() + y, 1, false, MouseEvent.BUTTON1));
                frame.validate();
                assertEquals(location - 60, split.getDividerLocation());
                assertEquals(height - 60, split.getTopComponent().getHeight());
            });
        } finally { runOnEdt(panel::dispose); }
    }

    @Test
    public void resizingKeepsUserChosenCompilerOutputHeight() {
        EditorPanel panel = createPanel(mock(Dialogs.class), createEditor(false, true));

        try {
            showInFrame(panel);
            JSplitPane splitPane = findComponent(panel, JSplitPane.class, split -> true);

            runOnEdt(() -> {
                panel.resizeComponents(650);
                splitPane.setDividerLocation(250);
                panel.resizeComponents(900);
            });

            assertEquals(250, onEdt(splitPane::getDividerLocation).intValue());
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    private EditorPanel createPanel(Dialogs dialogs, Editor editor) {
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCompiler()).thenReturn(Optional.empty());

        onEdt(RTextArea::new);

        return onEdt(() -> new EditorPanel(
                new JFrame(),
                dialogs,
                editor,
                computer,
                () -> {
                },
                () -> CPU.RunState.STATE_STOPPED_BREAK,
                new EmuStudioGui()
        ));
    }

    private Editor createEditor(boolean dirty, boolean saveFileResult) {
        Editor editor = mock(Editor.class);
        when(editor.getView()).thenReturn(new JPanel());
        when(editor.getFontSize()).thenReturn(FONT_CODE.getSize2D());
        when(editor.getSelectedText()).thenReturn(null);
        when(editor.isDirty()).thenReturn(dirty);
        when(editor.saveFile()).thenReturn(saveFileResult);
        return editor;
    }
}
