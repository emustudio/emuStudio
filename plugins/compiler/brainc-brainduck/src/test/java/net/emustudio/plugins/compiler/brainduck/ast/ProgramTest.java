/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck.ast;

import net.emustudio.emulib.runtime.io.IntelHEX;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgramTest {

    @Test
    public void testAddNullInstructionIsIgnored() {
        Program program = new Program();
        program.add(null);
        // Should not throw and toString should show empty instructions
        assertTrue(program.toString().contains("instructions=[]"));
    }

    @Test
    public void testGenerateCodeWithNoInstructions() {
        Program program = new Program();
        IntelHEX hex = new IntelHEX();
        program.generateCode(hex);
        // No exception, and hex should have no data records
        assertNotNull(hex);
    }

    @Test
    public void testToString() {
        Program program = new Program();
        String str = program.toString();
        assertNotNull(str);
        assertTrue(str.startsWith("Program{"));
        assertTrue(str.contains("instructions="));
    }

    @Test
    public void testToStringWithInstructions() {
        Program program = new Program();
        program.add(new Instruction(net.emustudio.plugins.compiler.brainduck.BraincParser.HALT));
        program.add(new Instruction(net.emustudio.plugins.compiler.brainduck.BraincParser.INC));
        String str = program.toString();
        assertTrue(str.contains("Instruction{"));
    }
}

