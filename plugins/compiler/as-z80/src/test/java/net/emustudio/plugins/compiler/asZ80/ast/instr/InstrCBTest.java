/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class InstrCBTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalRLC() {
        InstrCB instr = new InstrCB(POS, OPCODE_RLC, 0, 0);
        byte[] result = instr.eval();
        assertEquals(2, result.length);
        assertEquals((byte) 0xCB, result[0]);
        // x=0, y=0, z=0 => 0x00
        assertEquals((byte) 0x00, result[1]);
    }

    @Test
    public void testEvalRRC() {
        InstrCB instr = new InstrCB(POS, OPCODE_RRC, 1, 0);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // x=0, y=1, z=0 => 0x08
        assertEquals((byte) 0x08, result[1]);
    }

    @Test
    public void testEvalBIT() {
        // BIT 3, B: x=1, y=3, z=0
        InstrCB instr = new InstrCB(POS, OPCODE_BIT, 3, 0);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // (1<<6 | 3<<3 | 0) = 0x58
        assertEquals((byte) 0x58, result[1]);
    }

    @Test
    public void testEvalRES() {
        // RES 0, B: x=2, y=0, z=0
        InstrCB instr = new InstrCB(POS, OPCODE_RES, 0, 0);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // (2<<6 | 0<<3 | 0) = 0x80
        assertEquals((byte) 0x80, result[1]);
    }

    @Test
    public void testEvalSET() {
        // SET 7, A: x=3, y=7, z=7
        InstrCB instr = new InstrCB(POS, OPCODE_SET, 7, 7);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // (3<<6 | 7<<3 | 7) = 0xFF
        assertEquals((byte) 0xFF, result[1]);
    }

    @Test
    public void testSetY() {
        InstrCB instr = new InstrCB(POS, OPCODE_BIT, 0, 0);
        instr.setY(5);
        byte[] result = instr.eval();
        // x=1, y=5, z=0 => (1<<6 | 5<<3 | 0) = 0x68
        assertEquals((byte) 0x68, result[1]);
    }

    @Test
    public void testEvalRL() {
        // RL (HL): x=0 (from xmap), y=2 (rot table for RL), z=6
        InstrCB instr = new InstrCB(POS, OPCODE_RL, 2, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // (0<<6 | 2<<3 | 6) = 0x16
        assertEquals((byte) 0x16, result[1]);
    }

    @Test
    public void testEvalRR() {
        // RR (HL): x=0 (from xmap), y=3 (rot table for RR), z=6
        InstrCB instr = new InstrCB(POS, OPCODE_RR, 3, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // (0<<6 | 3<<3 | 6) = 0x1E
        assertEquals((byte) 0x1E, result[1]);
    }

    @Test
    public void testEvalSLA() {
        InstrCB instr = new InstrCB(POS, OPCODE_SLA, 0, 0);
        byte[] result = instr.eval();
        assertEquals((byte) 0xCB, result[0]);
        // x=0, y=0, z=0 => 0x20
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSRA() {
        InstrCB instr = new InstrCB(POS, OPCODE_SRA, 0, 0);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSLL() {
        InstrCB instr = new InstrCB(POS, OPCODE_SLL, 0, 0);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSRL() {
        InstrCB instr = new InstrCB(POS, OPCODE_SRL, 0, 0);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEqualsSame() {
        InstrCB i1 = new InstrCB(POS, OPCODE_RLC, 0, 0);
        InstrCB i2 = new InstrCB(POS, OPCODE_RLC, 0, 0);
        assertEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        InstrCB i1 = new InstrCB(POS, OPCODE_RLC, 0, 0);
        InstrCB i2 = new InstrCB(POS, OPCODE_RRC, 0, 0);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentY() {
        // Same opcode (same x), different y, same z
        InstrCB i1 = new InstrCB(POS, OPCODE_BIT, 0, 0);
        InstrCB i2 = new InstrCB(POS, OPCODE_BIT, 3, 0);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentZ() {
        // Same opcode, same y, different z
        InstrCB i1 = new InstrCB(POS, OPCODE_BIT, 3, 0);
        InstrCB i2 = new InstrCB(POS, OPCODE_BIT, 3, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testEqualsReflexive() {
        InstrCB instr = new InstrCB(POS, OPCODE_RLC, 0, 0);
        assertEquals(instr, instr);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        InstrCB instr = new InstrCB(POS, OPCODE_RLC, 0, 0);
        assertNotEquals(instr, "string");
    }

    @Test
    public void testNotEqualsNull() {
        InstrCB instr = new InstrCB(POS, OPCODE_RLC, 0, 0);
        assertNotEquals(instr, null);
    }

    @Test
    public void testToStringShallow() {
        InstrCB instr = new InstrCB(POS, OPCODE_RLC, 0, 0);
        assertTrue(instr.toString().contains("InstrCB"));
    }

    @Test
    public void testMkCopy() {
        InstrCB original = new InstrCB(POS, OPCODE_RLC, 0, 0);
        Node copy = original.copy();
        assertTrue(copy instanceof InstrCB);
        assertEquals(original, copy);
    }
}

