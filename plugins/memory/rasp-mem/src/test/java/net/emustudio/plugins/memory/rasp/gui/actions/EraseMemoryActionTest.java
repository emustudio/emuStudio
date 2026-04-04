/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui.actions;

import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.MemoryContextImplFactory;
import net.emustudio.plugins.memory.rasp.gui.RaspTableModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;

public class EraseMemoryActionTest {
    private MemoryContextImpl context;
    private RaspTableModel tableModel;

    @Before
    public void setUp() {
        context = MemoryContextImplFactory.create();
        tableModel = new RaspTableModel(context);
    }

    @After
    public void tearDown() {
        context.destroy();
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
        context.write(0, 100);
        context.write(1, 200);
        context.write(5, 500);

        EraseMemoryAction action = new EraseMemoryAction(tableModel, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "erase"));

        assertEquals(Integer.valueOf(0), context.read(0));
        assertEquals(Integer.valueOf(0), context.read(1));
        assertEquals(Integer.valueOf(0), context.read(5));
        assertEquals(0, context.getSize());
    }

    @Test
    public void testActionPerformedOnEmptyMemory() {
        EraseMemoryAction action = new EraseMemoryAction(tableModel, context);
        // Should not throw
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "erase"));

        assertEquals(0, context.getSize());
    }

    @Test
    public void testActionPerformedUpdatesTableModel() {
        context.write(0, 100);

        EraseMemoryAction action = new EraseMemoryAction(tableModel, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "erase"));

        // After erase, table model should report 0 rows
        assertEquals(0, tableModel.getRowCount());
    }
}

