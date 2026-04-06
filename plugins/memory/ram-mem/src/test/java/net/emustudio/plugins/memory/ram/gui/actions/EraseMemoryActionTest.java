/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui.actions;

import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.TestRamMemoryContextFactory;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.gui.RamTableModel;
import net.emustudio.plugins.memory.ram.gui.TestRamTableModelFactory;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class EraseMemoryActionTest {

    private MemoryContextImpl context;
    private RamTableModel tableModel;
    private EraseMemoryAction action;

    @Before
    public void setUp() {
        context = TestRamMemoryContextFactory.create();
        tableModel = TestRamTableModelFactory.create(context);
        action = new EraseMemoryAction(tableModel, context);
    }

    @Test(expected = NullPointerException.class)
    public void testNullTableModelThrows() {
        new EraseMemoryAction(null, context);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContextThrows() {
        new EraseMemoryAction(tableModel, null);
    }

    @Test
    public void testEraseMemory() {
        RamInstruction instr = createNiceMock(RamInstruction.class);
        expect(instr.getOpcode()).andReturn(RamInstruction.Opcode.HALT).anyTimes();
        expect(instr.getOperand()).andReturn(null).anyTimes();
        replay(instr);

        context.write(0, instr);
        assertEquals(1, context.getSize());

        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(0, context.getSize());
    }
}
