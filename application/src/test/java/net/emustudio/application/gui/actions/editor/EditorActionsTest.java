/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.editor;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class EditorActionsTest {

    @Test
    public void findAndReplaceActionsToggleDialogVisibility() {
        FindDialog findDialog = mock(FindDialog.class);
        ReplaceDialog replaceDialog = mock(ReplaceDialog.class);

        when(replaceDialog.isVisible()).thenReturn(true);
        new FindAction(findDialog, replaceDialog).actionPerformed(null);

        verify(replaceDialog).setVisible(false);
        verify(findDialog).setVisible(true);

        reset(findDialog, replaceDialog);
        when(findDialog.isVisible()).thenReturn(true);

        new ReplaceAction(findDialog, replaceDialog).actionPerformed(null);

        verify(findDialog).setVisible(false);
        verify(replaceDialog).setVisible(true);
    }

    @Test
    public void findNextAndPreviousEitherContinueReportMissOrFallbackToFindDialog() {
        Dialogs dialogs = mock(Dialogs.class);
        Action fallbackAction = mock(Action.class);
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "find");

        Editor nextFoundEditor = mock(Editor.class);
        when(nextFoundEditor.findNext()).thenReturn(Optional.of(true));
        new FindNextAction(nextFoundEditor, dialogs, fallbackAction).actionPerformed(event);
        verifyNoInteractions(dialogs, fallbackAction);

        Editor nextMissingEditor = mock(Editor.class);
        when(nextMissingEditor.findNext()).thenReturn(Optional.of(false));
        new FindNextAction(nextMissingEditor, dialogs, fallbackAction).actionPerformed(event);
        verify(dialogs).showInfo("Text was not found", "Find next");

        reset(dialogs, fallbackAction);
        Editor nextFallbackEditor = mock(Editor.class);
        when(nextFallbackEditor.findNext()).thenReturn(Optional.empty());
        new FindNextAction(nextFallbackEditor, dialogs, fallbackAction).actionPerformed(event);
        verify(fallbackAction).actionPerformed(event);

        reset(dialogs, fallbackAction);
        Editor previousFoundEditor = mock(Editor.class);
        when(previousFoundEditor.findPrevious()).thenReturn(Optional.of(true));
        new FindPreviousAction(previousFoundEditor, dialogs, fallbackAction).actionPerformed(event);
        verifyNoInteractions(dialogs, fallbackAction);

        Editor previousMissingEditor = mock(Editor.class);
        when(previousMissingEditor.findPrevious()).thenReturn(Optional.of(false));
        new FindPreviousAction(previousMissingEditor, dialogs, fallbackAction).actionPerformed(event);
        verify(dialogs).showInfo("Text was not found", "Find previous");

        reset(dialogs, fallbackAction);
        Editor previousFallbackEditor = mock(Editor.class);
        when(previousFallbackEditor.findPrevious()).thenReturn(Optional.empty());
        new FindPreviousAction(previousFallbackEditor, dialogs, fallbackAction).actionPerformed(event);
        verify(fallbackAction).actionPerformed(event);
    }

    @Test
    public void newAndOpenActionsRequireConfirmationAndSuccessfulEditorOperation() {
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea("old output");
        AtomicInteger titleUpdates = new AtomicInteger();

        NewFileAction newAction = new NewFileAction(() -> false, editor, compilerOutput, titleUpdates::incrementAndGet);
        newAction.actionPerformed(null);
        verify(editor, never()).newFile();
        assertEquals("old output", compilerOutput.getText());
        assertEquals(0, titleUpdates.get());

        NewFileAction confirmedNewAction = new NewFileAction(() -> true, editor, compilerOutput, titleUpdates::incrementAndGet);
        confirmedNewAction.actionPerformed(null);
        verify(editor).newFile();
        assertEquals("", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());

        compilerOutput.setText("compile me");
        when(editor.openFile()).thenReturn(false, true);

        OpenFileAction openAction = new OpenFileAction(() -> true, editor, compilerOutput, titleUpdates::incrementAndGet);
        openAction.actionPerformed(null);
        assertEquals("compile me", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());

        openAction.actionPerformed(null);
        assertEquals("", compilerOutput.getText());
        assertEquals(2, titleUpdates.get());
        verify(editor, times(2)).openFile();
    }

    @Test
    public void saveActionsUpdateWindowTitleOnlyAfterSuccessfulSave() {
        Editor editor = mock(Editor.class);
        AtomicInteger titleUpdates = new AtomicInteger();

        when(editor.saveFile()).thenReturn(false, true);
        SaveFileAction saveAction = new SaveFileAction(editor, titleUpdates::incrementAndGet);
        saveAction.actionPerformed(null);
        saveAction.actionPerformed(null);

        when(editor.saveFileAs()).thenReturn(false, true);
        SaveFileAsAction saveAsAction = new SaveFileAsAction(editor, titleUpdates::incrementAndGet);
        saveAsAction.actionPerformed(null);
        saveAsAction.actionPerformed(null);

        assertEquals(2, titleUpdates.get());
        verify(editor, times(2)).saveFile();
        verify(editor, times(2)).saveFileAs();
    }
}
