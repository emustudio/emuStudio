/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class InstrXDCBTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalRLC() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        byte[] result = instr.eval();
        assertEquals(3, result.length);
        assertEquals((byte) 0xDD, result[0]);
        assertEquals((byte) 0xCB, result[1]);
        // x=0, y=0, z=6 => 0x06
        assertEquals((byte) 0x06, result[2]);
    }

    @Test
    public void testEvalBIT() {
        // BIT: x=1
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 3, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xDD, result[0]);
        assertEquals((byte) 0xCB, result[1]);
        // (1<<6 | 3<<3 | 6) = 0x5E
        assertEquals((byte) 0x5E, result[2]);
    }

    @Test
    public void testEvalRES() {
        // RES: x=2
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RES, 0xFD, 5, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xFD, result[0]);
        assertEquals((byte) 0xCB, result[1]);
        // (2<<6 | 5<<3 | 6) = 0xAE
        assertEquals((byte) 0xAE, result[2]);
    }

    @Test
    public void testEvalSET() {
        // SET: x=3
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_SET, 0xDD, 7, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xDD, result[0]);
        assertEquals((byte) 0xCB, result[1]);
        // (3<<6 | 7<<3 | 6) = 0xFE
        assertEquals((byte) 0xFE, result[2]);
    }

    @Test
    public void testSetY() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 0, 6);
        instr.setY(4);
        byte[] result = instr.eval();
        // (1<<6 | 4<<3 | 6) = 0x66
        assertEquals((byte) 0x66, result[2]);
    }

    @Test
    public void testEvalWithFDPrefix() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RRC, 0xFD, 1, 6);
        byte[] result = instr.eval();
        assertEquals((byte) 0xFD, result[0]);
        assertEquals((byte) 0xCB, result[1]);
    }

    @Test
    public void testEvalSLA() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_SLA, 0xDD, 0, 6);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSRA() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_SRA, 0xDD, 0, 6);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSLL() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_SLL, 0xDD, 0, 6);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEvalSRL() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_SRL, 0xDD, 0, 6);
        assertEquals(0, instr.x);
    }

    @Test
    public void testEqualsSame() {
        InstrXDCB i1 = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        InstrXDCB i2 = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        assertEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentPrefix() {
        InstrXDCB i1 = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        InstrXDCB i2 = new InstrXDCB(POS, OPCODE_RLC, 0xFD, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        InstrXDCB i1 = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        InstrXDCB i2 = new InstrXDCB(POS, OPCODE_RRC, 0xDD, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsNull() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        assertNotEquals(instr, null);
    }

    @Test
    public void testEqualsReflexive() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        assertEquals(instr, instr);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        assertNotEquals(instr, "string");
    }

    @Test
    public void testNotEqualsDifferentY() {
        InstrXDCB i1 = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 0, 6);
        InstrXDCB i2 = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 3, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentZ() {
        InstrXDCB i1 = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 3, 0);
        InstrXDCB i2 = new InstrXDCB(POS, OPCODE_BIT, 0xDD, 3, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testToStringShallow() {
        InstrXDCB instr = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        String result = instr.toString();
        assertTrue(result.contains("InstrXDCB"));
        assertTrue(result.contains("prefix="));
    }

    @Test
    public void testMkCopy() {
        InstrXDCB original = new InstrXDCB(POS, OPCODE_RLC, 0xDD, 0, 6);
        Node copy = original.copy();
        assertTrue(copy instanceof InstrXDCB);
        assertEquals(original, copy);
    }
}

