/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.ssem.ast.Instruction;
import net.emustudio.plugins.compiler.ssem.ast.Program;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ProgramTest {

    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.ssem");

    @Test
    public void testToStringEmpty() {
        Program program = new Program();
        String result = program.toString();
        assertNotNull(result);
        assertTrue(result.contains("0 start"));
    }

    @Test
    public void testToStringWithInstructions() {
        Program program = new Program();
        program.setStartLine(5, POS);
        program.add(0, new Instruction(SSEMParser.LDN, 10, POS, Optional.of(POS)), POS);
        program.add(1, new Instruction(SSEMParser.STP, 0, POS, Optional.empty()), POS);

        String result = program.toString();
        assertNotNull(result);
        assertTrue(result.contains("5 start"));
    }

    @Test(expected = CompileException.class)
    public void testSetStartLineThrowsOnDuplicate() {
        Program program = new Program();
        program.setStartLine(5, POS);
        program.setStartLine(10, POS);
    }

    @Test(expected = CompileException.class)
    public void testSetStartLineThrowsOnOutOfBounds() {
        Program program = new Program();
        program.setStartLine(32, POS);
    }

    @Test(expected = CompileException.class)
    public void testAddThrowsOnDuplicateLine() {
        Program program = new Program();
        Instruction instr = new Instruction(SSEMParser.STP, 0, POS, Optional.empty());
        program.add(0, instr, POS);
        program.add(0, instr, POS);
    }

    @Test(expected = CompileException.class)
    public void testAddThrowsOnLineOutOfBounds() {
        Program program = new Program();
        Instruction instr = new Instruction(SSEMParser.STP, 0, POS, Optional.empty());
        program.add(32, instr, POS);
    }

    @Test
    public void testForEach() {
        Program program = new Program();
        program.add(0, new Instruction(SSEMParser.STP, 0, POS, Optional.empty()), POS);
        program.add(1, new Instruction(SSEMParser.LDN, 5, POS, Optional.of(POS)), POS);

        int[] count = {0};
        program.forEach((line, instr) -> count[0]++);
        assertEquals(2, count[0]);
    }
}

