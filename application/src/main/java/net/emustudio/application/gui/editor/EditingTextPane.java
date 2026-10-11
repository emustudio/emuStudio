/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;
import java.awt.event.MouseEvent;

/** Route selection gestures before Swing's single-caret mouse handler. */
final class EditingTextPane extends TextEditorPane {
    MultipleSelection selections;
    EditingTextPane() { super(RTextArea.INSERT_MODE); }
    @Override protected void processMouseEvent(MouseEvent e) {
        if (selections == null || !selections.mouse(e)) super.processMouseEvent(e);
    }
    @Override protected void processMouseMotionEvent(MouseEvent e) {
        if (selections == null || !selections.mouse(e)) super.processMouseMotionEvent(e);
    }
}
