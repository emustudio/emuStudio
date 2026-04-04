/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;
import java.awt.event.ActionEvent;
import static org.junit.Assert.*;
public class EraseMemoryActionTest {
    private MemoryContextImpl context;
    private MemoryTableModel tableModel;
    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(256, 1, 0);
        tableModel = new MemoryTableModel(context);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new EraseMemoryAction(null, context);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        new EraseMemoryAction(tableModel, null);
    }
    @Test
    public void testActionPerformedClearsMemory() {
        context.write(10, (byte) 42);
        context.write(20, (byte) 99);
        EraseMemoryAction action = new EraseMemoryAction(tableModel, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "erase"));
        assertEquals(Byte.valueOf((byte) 0), context.read(10));
        assertEquals(Byte.valueOf((byte) 0), context.read(20));
    }
    @Test
    public void testTableModelReflectsErase() {
        context.write(0, (byte) 0xFF);
        assertEquals(0xFF, tableModel.getRawValueAt(0, 0));
        EraseMemoryAction action = new EraseMemoryAction(tableModel, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "erase"));
        assertEquals(0, tableModel.getRawValueAt(0, 0));
    }
}
