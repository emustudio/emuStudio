/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import net.emustudio.plugins.memory.ssem.gui.table.MemoryTableModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EraseMemoryActionTest {

    @Test
    public void testEraseMemoryAction() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        memory.write(0, (byte) 0xFF);
        memory.write(10, (byte) 0xAB);

        MemoryTableModel tableModel = new MemoryTableModel(memory);
        EraseMemoryAction action = new EraseMemoryAction(tableModel, memory);
        action.actionPerformed(null);

        assertEquals((byte) 0, (byte) memory.read(0));
        assertEquals((byte) 0, (byte) memory.read(10));
    }

    @Test(expected = NullPointerException.class)
    public void testNullTableModelThrows() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        new EraseMemoryAction(null, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContextThrows() {
        MemoryTableModel tableModel = new MemoryTableModel(new MemoryContextImpl(new Annotations()));
        new EraseMemoryAction(tableModel, null);
    }
}
