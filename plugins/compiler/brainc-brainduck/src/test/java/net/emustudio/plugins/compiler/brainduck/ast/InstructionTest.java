/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck.ast;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.brainduck.BraincParser;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstructionTest {

    @Test
    public void testHaltInstruction() {
        Instruction instr = new Instruction(BraincParser.HALT);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testIncInstruction() {
        Instruction instr = new Instruction(BraincParser.INC);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testDecInstruction() {
        Instruction instr = new Instruction(BraincParser.DEC);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testIncvInstruction() {
        Instruction instr = new Instruction(BraincParser.INCV);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testDecvInstruction() {
        Instruction instr = new Instruction(BraincParser.DECV);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testPrintInstruction() {
        Instruction instr = new Instruction(BraincParser.PRINT);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testLoadInstruction() {
        Instruction instr = new Instruction(BraincParser.LOAD);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testLoopInstruction() {
        Instruction instr = new Instruction(BraincParser.LOOP);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testEndlInstruction() {
        Instruction instr = new Instruction(BraincParser.ENDL);
        IntelHEX hex = new IntelHEX();
        instr.generateCode(hex);
        assertNotNull(hex);
    }

    @Test
    public void testToString() {
        Instruction instr = new Instruction(BraincParser.INC);
        String str = instr.toString();
        assertNotNull(str);
        assertTrue(str.startsWith("Instruction{"));
        assertTrue(str.contains("instructionCode=1"));
    }

    @Test
    public void testToStringHalt() {
        Instruction instr = new Instruction(BraincParser.HALT);
        assertTrue(instr.toString().contains("instructionCode=0"));
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidTokenTypeThrows() {
        new Instruction(999);
    }
}

