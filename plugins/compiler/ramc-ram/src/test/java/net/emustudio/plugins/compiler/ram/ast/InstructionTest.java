/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class InstructionTest {

    @Test
    public void testGetOpcode() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertEquals(RamInstruction.Opcode.READ, instruction.getOpcode());
    }

    @Test
    public void testGetDirection() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.INDIRECT, 0, Optional.of(new Value(5)));
        assertEquals(RamInstruction.Direction.INDIRECT, instruction.getDirection());
    }

    @Test
    public void testGetAddress() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 42, Optional.of(new Value(5)));
        assertEquals(42, instruction.getAddress());
    }

    @Test
    public void testGetOperand() {
        Value value = new Value(5);
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(value));
        assertTrue(instruction.getOperand().isPresent());
        assertEquals(value, instruction.getOperand().get());
    }

    @Test
    public void testGetOperandEmpty() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT, 0, Optional.empty());
        assertFalse(instruction.getOperand().isPresent());
    }

    @Test
    public void testGetLabelEmpty() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertFalse(instruction.getLabel().isPresent());
    }

    @Test
    public void testSetLabel() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.JMP, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value("here", true)));
        Label label = new Label(1, 0, "here", 0);
        instruction.setLabel(label);
        assertTrue(instruction.getLabel().isPresent());
        assertEquals(label, instruction.getLabel().get());
    }

    @Test
    public void testConstructorWithLabel() {
        Label label = new Label(1, 0, "here", 0);
        Instruction instruction = new Instruction(RamInstruction.Opcode.JMP, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value("here", true)), label);
        assertTrue(instruction.getLabel().isPresent());
        assertEquals(label, instruction.getLabel().get());
    }

    @Test
    public void testEqualsAndHashCode() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        Instruction i2 = new Instruction(2, 1, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.WRITE, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentDirection() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.INDIRECT, 0, Optional.of(new Value(5)));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(5)));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOperand() {
        Instruction i1 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        Instruction i2 = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(6)));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testEqualsWithNull() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertNotEquals(instruction, null);
    }

    @Test
    public void testEqualsWithSameObject() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertEquals(instruction, instruction);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        assertNotEquals(instruction, "READ");
    }

    @Test
    public void testToString() {
        Instruction instruction = new Instruction(1, 0, RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5)));
        String str = instruction.toString();
        assertTrue(str.contains("READ"));
        assertTrue(str.contains("DIRECT"));
        assertTrue(str.contains("0"));
    }
}

