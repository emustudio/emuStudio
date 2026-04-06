/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.ui.rtextarea.RTextArea;
import org.junit.Test;

import javax.swing.*;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EditorPanelTest extends AbstractSwingTest {

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
                new EmuStudioGui()
        ));
    }

    private Editor createEditor(boolean dirty, boolean saveFileResult) {
        Editor editor = mock(Editor.class);
        when(editor.getView()).thenReturn(new JPanel());
        when(editor.getSelectedText()).thenReturn(null);
        when(editor.isDirty()).thenReturn(dirty);
        when(editor.saveFile()).thenReturn(saveFileResult);
        return editor;
    }
}
