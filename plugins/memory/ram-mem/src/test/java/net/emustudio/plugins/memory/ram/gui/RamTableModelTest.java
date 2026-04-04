/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.TestRamMemoryContextFactory;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamValue;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RamTableModelTest {
    private MemoryContextImpl context;
    private RamTableModel model;

    @Before
    public void setUp() {
        context = TestRamMemoryContextFactory.create();
        model = new RamTableModel(context);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMemoryThrows() {
        new RamTableModel(null);
    }

    @Test
    public void testColumnCount() {
        assertEquals(3, model.getColumnCount());
    }

    @Test
    public void testRowCountEmptyMemory() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    public void testRowCountAfterWrite() {
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));
        assertEquals(1, model.getRowCount());
    }

    @Test
    public void testColumnNames() {
        assertEquals("Address", model.getColumnName(0));
        assertEquals("Label", model.getColumnName(1));
        assertEquals("Instruction", model.getColumnName(2));
    }

    @Test
    public void testColumnNameInvalid() {
        assertEquals("", model.getColumnName(3));
        assertEquals("", model.getColumnName(-1));
    }

    @Test
    public void testColumnClass() {
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals(String.class, model.getColumnClass(2));
    }

    @Test
    public void testGetValueAtAddressColumn() {
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));
        assertEquals("0", model.getValueAt(0, 0));
    }

    @Test
    public void testGetValueAtLabelColumnNoLabel() {
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));
        assertEquals("", model.getValueAt(0, 1));
    }

    @Test
    public void testGetValueAtLabelColumnWithLabel() {
        RamLabel label = createNiceMock(RamLabel.class);
        expect(label.getAddress()).andReturn(0).anyTimes();
        expect(label.getLabel()).andReturn("start").anyTimes();
        replay(label);

        context.setLabels(List.of(label));
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));

        assertEquals("start", model.getValueAt(0, 1));
    }

    @Test
    public void testGetValueAtInstructionColumnNoOperand() {
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));
        assertEquals("halt ", model.getValueAt(0, 2));
    }

    @Test
    public void testGetValueAtInstructionColumnWithOperand() {
        RamValue operand = createNiceMock(RamValue.class);
        expect(operand.getStringRepresentation()).andReturn("42").anyTimes();
        replay(operand);

        RamInstruction instr = createNiceMock(RamInstruction.class);
        expect(instr.getOpcode()).andReturn(RamInstruction.Opcode.LOAD).anyTimes();
        expect(instr.getDirection()).andReturn(RamInstruction.Direction.CONSTANT).anyTimes();
        expect(instr.getOperand()).andReturn(Optional.of(operand)).anyTimes();
        expect(instr.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instr);

        context.write(0, instr);
        assertEquals("load 42", model.getValueAt(0, 2));
    }

    @Test
    public void testGetValueAtInvalidColumn() {
        context.write(0, createInstruction(RamInstruction.Opcode.HALT));
        assertEquals("", model.getValueAt(0, 3));
    }

    @Test
    public void testMultipleInstructions() {
        context.write(0, createInstruction(RamInstruction.Opcode.READ));
        context.write(1, createInstruction(RamInstruction.Opcode.WRITE));
        context.write(2, createInstruction(RamInstruction.Opcode.HALT));

        assertEquals(3, model.getRowCount());
        assertEquals("0", model.getValueAt(0, 0));
        assertEquals("1", model.getValueAt(1, 0));
        assertEquals("2", model.getValueAt(2, 0));
    }

    private RamInstruction createInstruction(RamInstruction.Opcode opcode) {
        RamInstruction instr = createNiceMock(RamInstruction.class);
        expect(instr.getOpcode()).andReturn(opcode).anyTimes();
        expect(instr.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(instr.getOperand()).andReturn(Optional.empty()).anyTimes();
        expect(instr.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instr);
        return instr;
    }
}
