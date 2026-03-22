/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import org.junit.Test;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.event.MouseEvent;

import static org.junit.Assert.assertNotNull;

public class BooleanCellEditorTest {

    @Test
    public void nullCellValueDoesNotThrowWhenEditorStarts() {
        JTable table = new JTable(1, 1);
        BooleanCellEditor editor = new BooleanCellEditor();

        editor.isCellEditable(new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1, false));
        Component component = editor.getTableCellEditorComponent(table, null, false, 0, 0);

        assertNotNull(component);
    }
}
