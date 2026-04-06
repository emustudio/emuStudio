/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.ssem.ast.Instruction;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstructionTest {

    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.ssem");

    @Test
    public void testToString() {
        Instruction instr = new Instruction(SSEMParser.LDN, 10, POS, POS);
        String result = instr.toString();
        assertNotNull(result);
        assertTrue(result.contains("02")); // opcode for LDN
        assertTrue(result.contains("10")); // operand
    }

    @Test
    public void testHashCode() {
        Instruction instr1 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        Instruction instr2 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        assertEquals(instr1.hashCode(), instr2.hashCode());
    }

    @Test
    public void testEqualsReflexive() {
        Instruction instr = new Instruction(SSEMParser.LDN, 10, POS, POS);
        assertEquals(instr, instr);
    }

    @Test
    public void testEqualsSymmetric() {
        Instruction instr1 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        Instruction instr2 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        assertEquals(instr1, instr2);
        assertEquals(instr2, instr1);
    }

    @Test
    public void testNotEqualsDifferentTokenType() {
        Instruction instr1 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        Instruction instr2 = new Instruction(SSEMParser.STO, 10, POS, POS);
        assertNotEquals(instr1, instr2);
    }

    @Test
    public void testNotEqualsDifferentOperand() {
        Instruction instr1 = new Instruction(SSEMParser.LDN, 10, POS, POS);
        Instruction instr2 = new Instruction(SSEMParser.LDN, 20, POS, POS);
        assertNotEquals(instr1, instr2);
    }

    @Test
    public void testGetOpcodeJMP() {
        Instruction instr = new Instruction(SSEMParser.JMP, 0, POS, POS);
        assertEquals(0, instr.getOpcode());
    }

    @Test
    public void testGetOpcodeJPR() {
        Instruction instr = new Instruction(SSEMParser.JPR, 0, POS, POS);
        assertEquals(4, instr.getOpcode());
    }

    @Test
    public void testGetOpcodeSUB() {
        Instruction instr = new Instruction(SSEMParser.SUB, 0, POS, POS);
        assertEquals(1, instr.getOpcode());
    }

    @Test
    public void testGetOpcodeCMP() {
        Instruction instr = new Instruction(SSEMParser.CMP, 0, POS, null);
        assertEquals(3, instr.getOpcode());
    }

    @Test
    public void testGetOpcodeSTP() {
        Instruction instr = new Instruction(SSEMParser.STP, 0, POS, null);
        assertEquals(7, instr.getOpcode());
    }

    @Test(expected = CompileException.class)
    public void testUnknownInstructionThrows() {
        // Use a token type that's not in the OPCODES map
        new Instruction(9999, 0, POS, null);
    }

    @Test(expected = CompileException.class)
    public void testOperandOutOfBoundsThrows() {
        new Instruction(SSEMParser.LDN, 32, POS, POS);
    }

    @Test
    public void testInstructionWithoutOperandPosition() {
        // STP doesn't need an operand, so operand position is empty
        Instruction instr = new Instruction(SSEMParser.STP, 0, POS, null);
        assertEquals(7, instr.getOpcode());
        assertEquals(0, instr.operand);
    }
}

