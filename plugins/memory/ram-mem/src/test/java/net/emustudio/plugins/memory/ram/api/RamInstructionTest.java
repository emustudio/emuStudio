/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class RamInstructionTest {

    @Test
    public void testOpcodeValues() {
        assertEquals(12, RamInstruction.Opcode.values().length);
        assertNotNull(RamInstruction.Opcode.valueOf("READ"));
        assertNotNull(RamInstruction.Opcode.valueOf("WRITE"));
        assertNotNull(RamInstruction.Opcode.valueOf("LOAD"));
        assertNotNull(RamInstruction.Opcode.valueOf("STORE"));
        assertNotNull(RamInstruction.Opcode.valueOf("ADD"));
        assertNotNull(RamInstruction.Opcode.valueOf("SUB"));
        assertNotNull(RamInstruction.Opcode.valueOf("MUL"));
        assertNotNull(RamInstruction.Opcode.valueOf("DIV"));
        assertNotNull(RamInstruction.Opcode.valueOf("JMP"));
        assertNotNull(RamInstruction.Opcode.valueOf("JZ"));
        assertNotNull(RamInstruction.Opcode.valueOf("JGTZ"));
        assertNotNull(RamInstruction.Opcode.valueOf("HALT"));
    }

    @Test
    public void testDirectionValues() {
        assertEquals(3, RamInstruction.Direction.values().length);
        assertEquals("=", RamInstruction.Direction.CONSTANT.value());
        assertEquals("", RamInstruction.Direction.DIRECT.value());
        assertEquals("*", RamInstruction.Direction.INDIRECT.value());
    }

    @Test
    public void testDirectionValueOf() {
        assertEquals(RamInstruction.Direction.CONSTANT, RamInstruction.Direction.valueOf("CONSTANT"));
        assertEquals(RamInstruction.Direction.DIRECT, RamInstruction.Direction.valueOf("DIRECT"));
        assertEquals(RamInstruction.Direction.INDIRECT, RamInstruction.Direction.valueOf("INDIRECT"));
    }
}

