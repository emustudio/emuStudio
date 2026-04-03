/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class InstrXDTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalDD() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        byte[] result = instr.eval();
        assertEquals(2, result.length);
        assertEquals((byte) 0xDD, result[0]);
        // (0<<6 | 0<<3 | 6) = 0x06
        assertEquals((byte) 0x06, result[1]);
    }

    @Test
    public void testEvalFD() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xFD, 1, 6, 6);
        byte[] result = instr.eval();
        assertEquals(2, result.length);
        assertEquals((byte) 0xFD, result[0]);
        // (1<<6 | 6<<3 | 6) = 0x76
        assertEquals((byte) 0x76, result[1]);
    }

    @Test
    public void testEqualsSame() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        assertEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentPrefix() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_LD, 0xFD, 0, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_ADD, 0xDD, 0, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentX() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_LD, 0xDD, 1, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentY() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 1, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentZ() {
        InstrXD i1 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        InstrXD i2 = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 7);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsNull() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        assertNotEquals(instr, null);
    }

    @Test
    public void testEqualsReflexive() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        assertEquals(instr, instr);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        assertNotEquals(instr, "string");
    }

    @Test
    public void testToStringShallow() {
        InstrXD instr = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        String result = instr.toString();
        assertTrue(result.contains("InstrXD"));
        assertTrue(result.contains("prefix="));
    }

    @Test
    public void testMkCopy() {
        InstrXD original = new InstrXD(POS, OPCODE_LD, 0xDD, 0, 0, 6);
        Node copy = original.copy();
        assertTrue(copy instanceof InstrXD);
        assertEquals(original, copy);
    }
}
