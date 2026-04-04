/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp.ast;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class InstructionTest {

    @Test
    public void testConstructorWithOperand() {
        Instruction instruction = new Instruction(1, 0, 5, 20, Optional.of(10));
        assertEquals(5, instruction.opcode);
        assertEquals(20, instruction.address);
        assertTrue(instruction.operand.isPresent());
        assertEquals(Integer.valueOf(10), instruction.operand.get());
        assertFalse(instruction.id.isPresent());
    }

    @Test
    public void testConstructorWithId() {
        Instruction instruction = new Instruction(1, 0, 15, 20, "myLabel");
        assertEquals(15, instruction.opcode);
        assertEquals(20, instruction.address);
        assertFalse(instruction.operand.isPresent());
        assertTrue(instruction.id.isPresent());
        assertEquals("myLabel", instruction.id.get());
    }

    @Test
    public void testShortConstructorWithOperand() {
        Instruction instruction = new Instruction(5, 20, Optional.of(10));
        assertEquals(5, instruction.opcode);
        assertEquals(20, instruction.address);
        assertEquals(0, instruction.line);
        assertEquals(0, instruction.column);
    }

    @Test
    public void testShortConstructorWithId() {
        Instruction instruction = new Instruction(15, 20, "myLabel");
        assertEquals(15, instruction.opcode);
        assertEquals(20, instruction.address);
        assertEquals(0, instruction.line);
        assertEquals(0, instruction.column);
    }

    @Test
    public void testConstructorWithEmptyOperand() {
        Instruction instruction = new Instruction(1, 0, 18, 20, Optional.empty());
        assertFalse(instruction.operand.isPresent());
        assertFalse(instruction.id.isPresent());
    }

    @Test
    public void testEqualsAndHashCode() {
        Instruction i1 = new Instruction(1, 0, 5, 20, Optional.of(10));
        Instruction i2 = new Instruction(2, 1, 5, 20, Optional.of(10));
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        Instruction i1 = new Instruction(1, 0, 5, 20, Optional.of(10));
        Instruction i2 = new Instruction(1, 0, 6, 20, Optional.of(10));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        Instruction i1 = new Instruction(1, 0, 5, 20, Optional.of(10));
        Instruction i2 = new Instruction(1, 0, 5, 21, Optional.of(10));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOperand() {
        Instruction i1 = new Instruction(1, 0, 5, 20, Optional.of(10));
        Instruction i2 = new Instruction(1, 0, 5, 20, Optional.of(11));
        assertNotEquals(i1, i2);
    }

    @Test
    public void testEqualsWithNull() {
        Instruction instruction = new Instruction(1, 0, 5, 20, Optional.of(10));
        assertNotEquals(instruction, null);
    }

    @Test
    public void testEqualsWithSameObject() {
        Instruction instruction = new Instruction(1, 0, 5, 20, Optional.of(10));
        assertEquals(instruction, instruction);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Instruction instruction = new Instruction(1, 0, 5, 20, Optional.of(10));
        assertNotEquals(instruction, "instruction");
    }

    @Test
    public void testToString() {
        Instruction instruction = new Instruction(1, 0, 5, 20, Optional.of(10));
        String str = instruction.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("10"));
    }

    @Test
    public void testLineAndColumn() {
        Instruction instruction = new Instruction(3, 7, 5, 20, Optional.of(10));
        assertEquals(3, instruction.line);
        assertEquals(7, instruction.column);
    }
}

