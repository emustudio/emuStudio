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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
    public void fontZoomSliderTracksShortcutsWheelAndActiveTabs() {
        Dialogs dialogs = mock(Dialogs.class);
        TabbedEditor editor = onEdt(() -> new TabbedEditor(dialogs, null, List.of(), files -> {}));
        EditorPanel panel = createPanel(dialogs, editor);
        try {
            JSlider slider = findComponent(panel, JSlider.class, component -> true);
            JLabel percentage = findLabel(panel, "100%");
            TextEditorPane text = findComponent(panel, TextEditorPane.class, component -> true);
            int baseSize = FONT_CODE.getSize();

            assertEquals(30, onEdt(slider::getMinimum).intValue());
            assertEquals(350, onEdt(slider::getMaximum).intValue());
            runOnEdt(() -> slider.setValue(200));
            assertEquals(baseSize * 2, onEdt(editor::getFontSize).intValue());
            assertEquals("200%", onEdt(percentage::getText));

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
            assertEquals(Math.round((baseSize * 2 + 2) * 100.0f / baseSize) + "%", onEdt(percentage::getText));

            JScrollPane scroll = (JScrollPane) ((JTabbedPane) editor.getView()).getSelectedComponent();
            runOnEdt(() -> {
                slider.setValue(slider.getMaximum());
                MouseWheelEvent wheel = new MouseWheelEvent(scroll, MouseWheelEvent.MOUSE_WHEEL,
                        System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK, 0, 0, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1);
                for (java.awt.event.MouseWheelListener listener : scroll.getMouseWheelListeners()) {
                    listener.mouseWheelMoved(wheel);
                }
            });
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);
            assertEquals(350, onEdt(slider::getValue).intValue());
            assertEquals("350%", onEdt(percentage::getText));
            invokeKeyBinding(text, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK));
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);

            runOnEdt(editor::newFile);
            assertEquals(100, onEdt(slider::getValue).intValue());
            assertEquals("100%", onEdt(percentage::getText));
            runOnEdt(() -> ((JTabbedPane) editor.getView()).setSelectedIndex(0));
            assertEquals(350, onEdt(slider::getValue).intValue());
            runOnEdt(editor::closeCurrent);
            assertEquals(100, onEdt(slider::getValue).intValue());
            runOnEdt(() -> slider.setValue(200));
            runOnEdt(editor::closeCurrent);
            assertEquals(100, onEdt(slider::getValue).intValue());
            assertEquals("100%", onEdt(percentage::getText));
            runOnEdt(() -> editor.setFontSize(0));
            assertEquals(baseSize * 0.3f, onEdt(editor::getFontSize), 0.001f);
            assertEquals(30, onEdt(slider::getValue).intValue());
            assertEquals("30%", onEdt(percentage::getText));
            TextEditorPane currentText = findComponent(panel, TextEditorPane.class, component -> true);
            invokeKeyBinding(currentText, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
            assertEquals(baseSize * 0.3f, onEdt(editor::getFontSize), 0.001f);
            runOnEdt(() -> editor.setFontSize(1000));
            assertEquals(baseSize * 3.5f, onEdt(editor::getFontSize), 0.001f);
            assertEquals("350%", onEdt(percentage::getText));
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
        when(dialogs.ask("File is not saved yet. Do you want to save it?"))
                .thenReturn(Dialogs.DialogAnswer.ANSWER_YES);

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
        when(dialogs.ask("File is not saved yet. Do you want to save it?"))
                .thenReturn(Dialogs.DialogAnswer.ANSWER_CANCEL);

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
    public void resizeComponentsKeepsMinimumCompilerOutputHeight() {
        EditorPanel panel = createPanel(mock(Dialogs.class), createEditor(false, true));

        try {
            showInFrame(panel);
            JSplitPane splitPane = findComponent(panel, JSplitPane.class, split -> true);

            runOnEdt(() -> panel.resizeComponents(300));

            assertEquals(100, onEdt(splitPane::getDividerLocation).intValue());
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
                null,
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
