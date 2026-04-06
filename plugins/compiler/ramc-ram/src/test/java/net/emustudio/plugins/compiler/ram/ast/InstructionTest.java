/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstructionTest {

    @Test
    public void testGetOpcode() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertEquals(RamInstruction.Opcode.READ, instruction.getOpcode());
    }

    @Test
    public void testGetDirection() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.INDIRECT, 0, new Value(5));
        assertEquals(RamInstruction.Direction.INDIRECT, instruction.getDirection());
    }

    @Test
    public void testGetAddress() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 42, new Value(5));
        assertEquals(42, instruction.getAddress());
    }

    @Test
    public void testGetOperand() {
        Value value = new Value(5);
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, value);
        assertNotNull(instruction.getOperand());
        assertEquals(value, instruction.getOperand());
    }

    @Test
    public void testGetOperandEmpty() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT, 0, null);
        assertNull(instruction.getOperand());
    }

    @Test
    public void testGetLabelEmpty() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertNull(instruction.getLabel());
    }

    @Test
    public void testSetLabel() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.JMP, RamInstruction.Direction.DIRECT, 0, new Value("here", true));
        Label label = new Label(1, 0, "here", 0);
        instruction.setLabel(label);
        assertNotNull(instruction.getLabel());
        assertEquals(label, instruction.getLabel());
    }

    @Test
    public void testConstructorWithLabel() {
        Label label = new Label(1, 0, "here", 0);
        Instruction instruction = new Instruction(RamInstruction.Opcode.JMP, RamInstruction.Direction.DIRECT, 0, new Value("here", true), label);
        assertNotNull(instruction.getLabel());
        assertEquals(label, instruction.getLabel());
    }

    @Test
    public void testEqualsAndHashCode() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        Instruction i2 = new Instruction(2, 1, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.WRITE, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentDirection() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.INDIRECT, 0, new Value(5));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 1, new Value(5));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOperand() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(6));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testEqualsWithNull() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertNotEquals(null, instruction);
    }

    @Test
    public void testEqualsWithSameObject() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertEquals(instruction, instruction);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        assertNotEquals("READ", instruction);
    }

    @Test
    public void testToString() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, new Value(5));
        String str = instruction.toString();
        assertTrue(str.contains("READ"));
        assertTrue(str.contains("DIRECT"));
        assertTrue(str.contains("0"));
    }
}
