/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.plugins.compiler.ssem.ast.Program;
import org.junit.Test;

import static net.emustudio.plugins.compiler.ssem.Utils.assertInstructions;
import static net.emustudio.plugins.compiler.ssem.Utils.parseProgram;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void testParseEmptyLine() {
        Program program = parseProgram("");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseLineNumberOnly() {
        Program program = parseProgram("10");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseHexLineNumber() {
        Program program = parseProgram("0x10");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testLineThenCommentWorks() {
        Program program = parseProgram("0x10 -- comment");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testCommentOnly() {
        Program program = parseProgram("-- comment");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testEolOnly() {
        Program program = parseProgram("\n");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseInstructions() {
        Program program = parseProgram(
                "01 LDN 0x1F\n" +
                        "-- 01 ldn 30\n" +
                        "; 01 LDN 30\n" +
                        "# 01 ldn 30\n" +
                        "04 SUB 30 --comment1\n" +
                        "05 STO 31 # comment2\n" +
                        "06 STP ; comment3\n" +
                        "07 CMP\n" +
                        "08 NUM 4\n" +
                        "09 BNUM 100\n\n\n"
        );
        assertInstructions(
                program,
                new Utils.ParsedInstruction(1, SSEMParser.LDN, 0x1F),
                new Utils.ParsedInstruction(4, SSEMParser.SUB, 30),
                new Utils.ParsedInstruction(5, SSEMParser.STO, 31),
                new Utils.ParsedInstruction(6, SSEMParser.STP, 0),
                new Utils.ParsedInstruction(7, SSEMParser.CMP, 0),
                new Utils.ParsedInstruction(8, SSEMParser.NUM, 4),
                new Utils.ParsedInstruction(9, SSEMParser.BNUM, 4)
        );
    }

    @Test
    public void testParseAllInstructions() {
        Program program = parseProgram(
                "01 jmp 0x1F\n" +
                        "02 jrp 0x1F\n" +
                        "03 jpr 0x1F\n" +
                        "04 jmr 0x1F\n" +
                        "05 ldn 0x1F\n" +
                        "06 sto 0x1F\n" +
                        "07 sub 0x1F\n" +
                        "08 cmp\n" +
                        "09 skn\n" +
                        "10 stp\n" +
                        "11 hlt\n"
        );
        assertInstructions(
                program,
                new Utils.ParsedInstruction(1, SSEMParser.JMP, 0x1F),
                new Utils.ParsedInstruction(2, SSEMParser.JPR, 0x1F),
                new Utils.ParsedInstruction(3, SSEMParser.JPR, 0x1F),
                new Utils.ParsedInstruction(4, SSEMParser.JPR, 0x1F),
                new Utils.ParsedInstruction(5, SSEMParser.LDN, 0x1F),
                new Utils.ParsedInstruction(6, SSEMParser.STO, 0x1F),
                new Utils.ParsedInstruction(7, SSEMParser.SUB, 0x1F),
                new Utils.ParsedInstruction(8, SSEMParser.CMP, 0),
                new Utils.ParsedInstruction(9, SSEMParser.CMP, 0),
                new Utils.ParsedInstruction(10, SSEMParser.STP, 0),
                new Utils.ParsedInstruction(11, SSEMParser.STP, 0)
        );
    }

    @Test
    public void testParseNegativeNumber() {
        Program program = parseProgram(
                "01 NUM -3"
        );
        assertInstructions(program, new Utils.ParsedInstruction(1, SSEMLexer.NUM, -3));
    }

    @Test
    public void testStartingPointIsAccepted() {
        Program program = parseProgram(
                "02 start\n" +
                        "01 LDN 21\n" +
                        "02 STP"
        );
        assertEquals(2, program.getStartLine());
    }

    @Test
    public void testParseLongBinaryNumber() {
        Program program = parseProgram("0000 BINS 11001000000000000000000000000000");
        assertEquals(3355443200L, program.getInstructions().get(0).operand);
    }

    @Test(expected = CompileException.class)
    public void testOperandBounds() {
        parseProgram("01 ldn 99\n");
    }

    @Test(expected = CompileException.class)
    public void testLineBounds() {
        parseProgram("99 ldn 1\n");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithoutOperand() {
        parseProgram("01 ldn");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithWrongArgument() {
        parseProgram("01 ldn fff");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithoutLine() {
        parseProgram("stp");
    }

    @Test(expected = CompileException.class)
    public void testParseTwoInstructionsWithoutEol() {
        parseProgram("01 stp 02 stp");
    }
}
