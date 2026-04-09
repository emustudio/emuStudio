/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Test;

import javax.swing.*;

import static org.easymock.EasyMock.*;

public class KeyboardHandlerTest {
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableThrows() {
        SpinnerModel model = new SpinnerNumberModel(0, 0, 100, 1);
        MemoryGui gui = createNiceMock(MemoryGui.class);
        replay(gui);
        new KeyboardHandler(null, model, gui);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullSpinnerModelThrows() {
        MemoryTable table = createNiceMock(MemoryTable.class);
        MemoryTableModel tableModel = createNiceMock(MemoryTableModel.class);
        expect(table.getTableModel()).andReturn(tableModel).anyTimes();
        MemoryGui gui = createNiceMock(MemoryGui.class);
        replay(table, tableModel, gui);
        new KeyboardHandler(table, null, gui);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullGuiThrows() {
        MemoryTable table = createNiceMock(MemoryTable.class);
        MemoryTableModel tableModel = createNiceMock(MemoryTableModel.class);
        expect(table.getTableModel()).andReturn(tableModel).anyTimes();
        SpinnerModel model = new SpinnerNumberModel(0, 0, 100, 1);
        replay(table, tableModel);
        new KeyboardHandler(table, model, null);
    }

    @Test
    public void testConstructorValid() {
        MemoryTable table = createNiceMock(MemoryTable.class);
        MemoryTableModel tableModel = createNiceMock(MemoryTableModel.class);
        expect(table.getTableModel()).andReturn(tableModel).anyTimes();
        SpinnerModel model = new SpinnerNumberModel(0, 0, 100, 1);
        MemoryGui gui = createNiceMock(MemoryGui.class);
        replay(table, tableModel, gui);
        // Should not throw
        new KeyboardHandler(table, model, gui);
    }
}
