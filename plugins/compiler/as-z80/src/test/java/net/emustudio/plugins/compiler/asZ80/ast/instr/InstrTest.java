/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class InstrTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEval() {
        // NOP: x=0, y=0, z=0 => (0<<6 | 0<<3 | 0) = 0x00
        Instr instr = new Instr(POS, OPCODE_NOP, 0, 0, 0);
        assertEquals(0x00, instr.eval() & 0xFF);
    }

    @Test
    public void testEvalLD() {
        // LD B, n: x=0, y=0, z=6 => (0<<6 | 0<<3 | 6) = 0x06
        Instr instr = new Instr(POS, OPCODE_LD, 0, 0, 6);
        assertEquals(0x06, instr.eval() & 0xFF);
    }

    @Test
    public void testEvalHalt() {
        // HALT: x=1, y=6, z=6 => (1<<6 | 6<<3 | 6) = 0x76
        Instr instr = new Instr(POS, OPCODE_HALT, 1, 6, 6);
        assertEquals(0x76, instr.eval() & 0xFF);
    }

    @Test
    public void testEvalRet() {
        // RET: x=3, y=1, z=1 => (3<<6 | 1<<3 | 1) = 0xC9
        Instr instr = new Instr(POS, OPCODE_RET, 3, 1, 1);
        assertEquals(0xC9, instr.eval() & 0xFF);
    }

    @Test
    public void testHasRelativeAddressDJNZ() {
        // DJNZ: x=0, z=0, y=2
        Instr instr = new Instr(POS, OPCODE_DJNZ, 0, 2, 0);
        assertTrue(instr.hasRelativeAddress());
    }

    @Test
    public void testHasRelativeAddressJR() {
        // JR: x=0, z=0, y=3
        Instr instr = new Instr(POS, OPCODE_JR, 0, 3, 0);
        assertTrue(instr.hasRelativeAddress());
    }

    @Test
    public void testHasRelativeAddressJRcc() {
        // JR cc: x=0, z=0, y=4..7
        for (int y = 4; y <= 7; y++) {
            Instr instr = new Instr(POS, OPCODE_JR, 0, y, 0);
            assertTrue("y=" + y, instr.hasRelativeAddress());
        }
    }

    @Test
    public void testHasNoRelativeAddress() {
        // y < 2
        Instr instr = new Instr(POS, OPCODE_NOP, 0, 0, 0);
        assertFalse(instr.hasRelativeAddress());
    }

    @Test
    public void testHasNoRelativeAddressNonZeroX() {
        Instr instr = new Instr(POS, OPCODE_LD, 1, 3, 0);
        assertFalse(instr.hasRelativeAddress());
    }

    @Test
    public void testHasNoRelativeAddressNonZeroZ() {
        Instr instr = new Instr(POS, OPCODE_LD, 0, 3, 1);
        assertFalse(instr.hasRelativeAddress());
    }

    @Test
    public void testSetY() {
        Instr instr = new Instr(POS, OPCODE_RST, 3, 0, 7);
        instr.setY(5);
        // x=3, y=5, z=7 => (3<<6 | 5<<3 | 7) = 0xEF
        assertEquals(0xEF, instr.eval() & 0xFF);
    }

    @Test
    public void testEqualsSame() {
        Instr i1 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Instr i2 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        assertEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentOpcode() {
        Instr i1 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Instr i2 = new Instr(POS, OPCODE_NOP, 0, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentX() {
        Instr i1 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Instr i2 = new Instr(POS, OPCODE_LD, 1, 0, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentY() {
        Instr i1 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Instr i2 = new Instr(POS, OPCODE_LD, 0, 1, 6);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentZ() {
        Instr i1 = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Instr i2 = new Instr(POS, OPCODE_LD, 0, 0, 7);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsNull() {
        Instr instr = new Instr(POS, OPCODE_LD, 0, 0, 6);
        assertNotEquals(instr, null);
    }

    @Test
    public void testEqualsReflexive() {
        Instr instr = new Instr(POS, OPCODE_LD, 0, 0, 6);
        assertEquals(instr, instr);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        Instr instr = new Instr(POS, OPCODE_LD, 0, 0, 6);
        assertNotEquals(instr, "string");
    }

    @Test
    public void testHasRelativeAddressY1() {
        // y=1 should NOT be relative (y must be >= 2)
        Instr instr = new Instr(POS, OPCODE_LD, 0, 1, 0);
        assertFalse(instr.hasRelativeAddress());
    }

    @Test
    public void testToStringShallow() {
        Instr instr = new Instr(POS, OPCODE_LD, 0, 0, 6);
        String result = instr.toString();
        assertTrue(result.contains("Instr"));
        assertTrue(result.contains("x=0"));
        assertTrue(result.contains("z=6"));
    }

    @Test
    public void testMkCopy() {
        Instr original = new Instr(POS, OPCODE_LD, 0, 0, 6);
        Node copy = original.copy();
        assertTrue(copy instanceof Instr);
        assertEquals(original, copy);
    }
}

