/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class InstrEDTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalIN() {
        // IN r, (C): x=1, y=0, z=0
        InstrED instr = new InstrED(POS, OPCODE_IN, 0, 0);
        byte[] result = instr.eval();
        assertEquals(2, result.length);
        assertEquals((byte) 0xED, result[0]);
        // (1<<6 | 0<<3 | 0) = 0x40
        assertEquals((byte) 0x40, result[1]);
    }

    @Test
    public void testEvalOUT() {
        InstrED instr = new InstrED(POS, OPCODE_OUT, 0, 1);
        byte[] result = instr.eval();
        assertEquals((byte) 0xED, result[0]);
        // (1<<6 | 0<<3 | 1) = 0x41
        assertEquals((byte) 0x41, result[1]);
    }

    @Test
    public void testEvalNEG() {
        InstrED instr = new InstrED(POS, OPCODE_NEG, 0, 4);
        byte[] result = instr.eval();
        assertEquals((byte) 0xED, result[0]);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalRETN() {
        InstrED instr = new InstrED(POS, OPCODE_RETN, 0, 5);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalRETI() {
        InstrED instr = new InstrED(POS, OPCODE_RETI, 1, 5);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalLDI() {
        InstrED instr = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertEquals(2, instr.x);
        byte[] result = instr.eval();
        assertEquals((byte) 0xED, result[0]);
        // (2<<6 | 4<<3 | 0) = 0xA0
        assertEquals((byte) 0xA0, result[1]);
    }

    @Test
    public void testEvalLDD() {
        InstrED instr = new InstrED(POS, OPCODE_LDD, 5, 0);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalLDIR() {
        InstrED instr = new InstrED(POS, OPCODE_LDIR, 6, 0);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalLDDR() {
        InstrED instr = new InstrED(POS, OPCODE_LDDR, 7, 0);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalCPI() {
        InstrED instr = new InstrED(POS, OPCODE_CPI, 4, 1);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalCPD() {
        InstrED instr = new InstrED(POS, OPCODE_CPD, 5, 1);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalCPIR() {
        InstrED instr = new InstrED(POS, OPCODE_CPIR, 6, 1);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalCPDR() {
        InstrED instr = new InstrED(POS, OPCODE_CPDR, 7, 1);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalINI() {
        InstrED instr = new InstrED(POS, OPCODE_INI, 4, 2);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalIND() {
        InstrED instr = new InstrED(POS, OPCODE_IND, 5, 2);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalINIR() {
        InstrED instr = new InstrED(POS, OPCODE_INIR, 6, 2);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalINDR() {
        InstrED instr = new InstrED(POS, OPCODE_INDR, 7, 2);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalOUTI() {
        InstrED instr = new InstrED(POS, OPCODE_OUTI, 4, 3);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalOUTD() {
        InstrED instr = new InstrED(POS, OPCODE_OUTD, 5, 3);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalOTIR() {
        InstrED instr = new InstrED(POS, OPCODE_OTIR, 6, 3);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalOTDR() {
        InstrED instr = new InstrED(POS, OPCODE_OTDR, 7, 3);
        assertEquals(2, instr.x);
    }

    @Test
    public void testEvalRRD() {
        InstrED instr = new InstrED(POS, OPCODE_RRD, 4, 7);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalRLD() {
        InstrED instr = new InstrED(POS, OPCODE_RLD, 5, 7);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalSBC() {
        InstrED instr = new InstrED(POS, OPCODE_SBC, 0, 2);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalADC() {
        InstrED instr = new InstrED(POS, OPCODE_ADC, 1, 2);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEvalIM() {
        InstrED instr = new InstrED(POS, OPCODE_IM, 0, 6);
        assertEquals(1, instr.x);
    }

    @Test
    public void testEqualsSame() {
        InstrED i1 = new InstrED(POS, OPCODE_LDI, 4, 0);
        InstrED i2 = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferent() {
        InstrED i1 = new InstrED(POS, OPCODE_LDI, 4, 0);
        InstrED i2 = new InstrED(POS, OPCODE_LDD, 5, 0);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsNull() {
        InstrED instr = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertNotEquals(instr, null);
    }

    @Test
    public void testEqualsReflexive() {
        InstrED instr = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertEquals(instr, instr);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        InstrED instr = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertNotEquals(instr, "string");
    }

    @Test
    public void testNotEqualsDifferentY() {
        InstrED i1 = new InstrED(POS, OPCODE_IN, 0, 0);
        InstrED i2 = new InstrED(POS, OPCODE_IN, 1, 0);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testNotEqualsDifferentZ() {
        InstrED i1 = new InstrED(POS, OPCODE_IN, 0, 0);
        InstrED i2 = new InstrED(POS, OPCODE_IN, 0, 1);
        assertNotEquals(i1, i2);
    }

    @Test
    public void testToStringShallow() {
        InstrED instr = new InstrED(POS, OPCODE_LDI, 4, 0);
        assertTrue(instr.toString().contains("InstrED"));
    }

    @Test
    public void testMkCopy() {
        InstrED original = new InstrED(POS, OPCODE_LDI, 4, 0);
        Node copy = original.copy();
        assertTrue(copy instanceof InstrED);
        assertEquals(original, copy);
    }
}

