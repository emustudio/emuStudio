/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp.ast;

import org.junit.Test;

import static org.junit.Assert.*;

public class InstructionTest {

    @Test
    public void testConstructorWithOperand() {
        Instruction instruction = new Instruction(1, 0, 5, 20, 10);
        assertEquals(5, instruction.opcode);
        assertEquals(20, instruction.address);
        assertNotNull(instruction.operand);
        assertEquals(Integer.valueOf(10), instruction.operand);
        assertNull(instruction.id);
    }

    @Test
    public void testConstructorWithId() {
        Instruction instruction = new Instruction(1, 0, 15, 20, "myLabel");
        assertEquals(15, instruction.opcode);
        assertEquals(20, instruction.address);
        assertNull(instruction.operand);
        assertNotNull(instruction.id);
        assertEquals("myLabel", instruction.id);
    }

    @Test
    public void testShortConstructorWithOperand() {
        Instruction instruction = new Instruction(5, 20, 10);
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
        Instruction instruction = new Instruction(1, 0, 18, 20, (Integer) null);
        assertNull(instruction.operand);
        assertNull(instruction.id);
    }

    @Test
    public void testEqualsAndHashCode() {
        Instruction i1 = new Instruction(1, 0, 5, 20, 10);
        Instruction i2 = new Instruction(2, 1, 5, 20, 10);
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        Instruction i1 = new Instruction(1, 0, 5, 20, 10);
        Instruction i2 = new Instruction(1, 0, 6, 20, 10);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        Instruction i1 = new Instruction(1, 0, 5, 20, 10);
        Instruction i2 = new Instruction(1, 0, 5, 21, 10);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOperand() {
        Instruction i1 = new Instruction(1, 0, 5, 20, 10);
        Instruction i2 = new Instruction(1, 0, 5, 20, 11);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testEqualsWithNull() {
        Instruction instruction = new Instruction(1, 0, 5, 20, 10);
        assertNotNull(instruction);
    }

    @Test
    public void testEqualsWithSameObject() {
        Instruction instruction = new Instruction(1, 0, 5, 20, 10);
        assertEquals(instruction, instruction);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Instruction instruction = new Instruction(1, 0, 5, 20, 10);
        assertNotEquals("instruction", instruction);
    }

    @Test
    public void testToString() {
        Instruction instruction = new Instruction(1, 0, 5, 20, 10);
        String str = instruction.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("10"));
    }

    @Test
    public void testLineAndColumn() {
        Instruction instruction = new Instruction(3, 7, 5, 20, 10);
        assertEquals(3, instruction.line);
        assertEquals(7, instruction.column);
    }
}

