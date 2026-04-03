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
    public void findActionHidesReplaceDialogAndShowsFindDialog() {
        FindDialog findDialog = mock(FindDialog.class);
        ReplaceDialog replaceDialog = mock(ReplaceDialog.class);
        when(replaceDialog.isVisible()).thenReturn(true);

        new FindAction(findDialog, replaceDialog).actionPerformed(null);

        verify(replaceDialog).setVisible(false);
        verify(findDialog).setVisible(true);
    }

    @Test
    public void replaceActionHidesFindDialogAndShowsReplaceDialog() {
        FindDialog findDialog = mock(FindDialog.class);
        ReplaceDialog replaceDialog = mock(ReplaceDialog.class);
        when(findDialog.isVisible()).thenReturn(true);

        new ReplaceAction(findDialog, replaceDialog).actionPerformed(null);

        verify(findDialog).setVisible(false);
        verify(replaceDialog).setVisible(true);
    }

    @Test
    public void findNextDoesNothingWhenTextIsFound() {
        Dialogs dialogs = mock(Dialogs.class);
        Action fallbackAction = mock(Action.class);
        Editor editor = mock(Editor.class);
        when(editor.findNext()).thenReturn(Optional.of(true));

        new FindNextAction(editor, dialogs, fallbackAction).actionPerformed(null);

        verifyNoInteractions(dialogs, fallbackAction);
    }

    @Test
    public void findNextShowsInfoWhenTextNotFound() {
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        when(editor.findNext()).thenReturn(Optional.of(false));

        new FindNextAction(editor, dialogs, mock(Action.class)).actionPerformed(null);

        verify(dialogs).showInfo("Text was not found", "Find next");
    }

    @Test
    public void findNextFallsBackToFindDialogWhenNoSearch() {
        Action fallbackAction = mock(Action.class);
        Editor editor = mock(Editor.class);
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "find");
        when(editor.findNext()).thenReturn(Optional.empty());

        new FindNextAction(editor, mock(Dialogs.class), fallbackAction).actionPerformed(event);

        verify(fallbackAction).actionPerformed(event);
    }

    @Test
    public void findPreviousDoesNothingWhenTextIsFound() {
        Dialogs dialogs = mock(Dialogs.class);
        Action fallbackAction = mock(Action.class);
        Editor editor = mock(Editor.class);
        when(editor.findPrevious()).thenReturn(Optional.of(true));

        new FindPreviousAction(editor, dialogs, fallbackAction).actionPerformed(null);

        verifyNoInteractions(dialogs, fallbackAction);
    }

    @Test
    public void findPreviousShowsInfoWhenTextNotFound() {
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        when(editor.findPrevious()).thenReturn(Optional.of(false));

        new FindPreviousAction(editor, dialogs, mock(Action.class)).actionPerformed(null);

        verify(dialogs).showInfo("Text was not found", "Find previous");
    }

    @Test
    public void findPreviousFallsBackToFindDialogWhenNoSearch() {
        Action fallbackAction = mock(Action.class);
        Editor editor = mock(Editor.class);
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "find");
        when(editor.findPrevious()).thenReturn(Optional.empty());

        new FindPreviousAction(editor, mock(Dialogs.class), fallbackAction).actionPerformed(event);

        verify(fallbackAction).actionPerformed(event);
    }

    @Test
    public void newFileActionDoesNothingWhenNotConfirmed() {
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea("old output");
        AtomicInteger titleUpdates = new AtomicInteger();

        new NewFileAction(() -> false, editor, compilerOutput, titleUpdates::incrementAndGet).actionPerformed(null);

        verify(editor, never()).newFile();
        assertEquals("old output", compilerOutput.getText());
        assertEquals(0, titleUpdates.get());
    }

    @Test
    public void newFileActionCreatesNewFileAndClearsOutput() {
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea("old output");
        AtomicInteger titleUpdates = new AtomicInteger();

        new NewFileAction(() -> true, editor, compilerOutput, titleUpdates::incrementAndGet).actionPerformed(null);

        verify(editor).newFile();
        assertEquals("", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());
    }

    @Test
    public void openFileActionDoesNotClearOutputWhenOpenFails() {
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea("compile me");
        AtomicInteger titleUpdates = new AtomicInteger();
        when(editor.openFile()).thenReturn(false);

        new OpenFileAction(() -> true, editor, compilerOutput, titleUpdates::incrementAndGet).actionPerformed(null);

        assertEquals("compile me", compilerOutput.getText());
        assertEquals(0, titleUpdates.get());
    }

    @Test
    public void openFileActionClearsOutputWhenOpenSucceeds() {
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea("compile me");
        AtomicInteger titleUpdates = new AtomicInteger();
        when(editor.openFile()).thenReturn(true);

        new OpenFileAction(() -> true, editor, compilerOutput, titleUpdates::incrementAndGet).actionPerformed(null);

        assertEquals("", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());
    }

    @Test
    public void saveFileActionUpdatesTitleOnlyOnSuccess() {
        Editor editor = mock(Editor.class);
        AtomicInteger titleUpdates = new AtomicInteger();
        when(editor.saveFile()).thenReturn(false, true);

        SaveFileAction action = new SaveFileAction(editor, titleUpdates::incrementAndGet);
        action.actionPerformed(null); // returns false
        action.actionPerformed(null); // returns true

        assertEquals(1, titleUpdates.get());
    }

    @Test
    public void saveFileAsActionUpdatesTitleOnlyOnSuccess() {
        Editor editor = mock(Editor.class);
        AtomicInteger titleUpdates = new AtomicInteger();
        when(editor.saveFileAs()).thenReturn(false, true);

        SaveFileAsAction action = new SaveFileAsAction(editor, titleUpdates::incrementAndGet);
        action.actionPerformed(null); // returns false
        action.actionPerformed(null); // returns true

        assertEquals(1, titleUpdates.get());
    }
}
