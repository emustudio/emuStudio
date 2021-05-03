/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.compiler.ssem.tree.ASTvisitor;
import net.emustudio.plugins.compiler.ssem.tree.Constant;
import net.emustudio.plugins.compiler.ssem.tree.Instruction;
import net.emustudio.plugins.compiler.ssem.tree.Program;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class ParserTest {

    // PASS:
    // -- COMMENT
    // <EOL>
    // 01 STP -- COMMENT

    // FAIL:
    // STP

    // PASS:
    // 01

    // PASS:
    // --

    // PASS:
    // <EOL>

    // FAIL:
    // 01 STP 01 STP



    private ParserImpl program(String program) {
        return new ParserImpl(
            new LexerImpl(new StringReader(program)),
            new CompilerImpl(0, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE)
        );
    }

    @Test
    public void testInstructions() throws Exception {
        ParserImpl parser = program(
            "0 cmp // comment\n" +
                "1 stp\n" +
                "2 jmp 22\n" +
                "3 jrp 0\n" +
                "4 ldn 31\n" +
                "5 sto 10\n" +
                "6 sub 15\n"
        );

        Program program = (Program) parser.parse().value;
        assertFalse(parser.hasSyntaxErrors());

        Deque<Instruction> expectedInstructions = new LinkedList<>(Arrays.asList(
            Instruction.cmp(),
            Instruction.stp(),
            Instruction.jmp((byte) 22),
            Instruction.jrp((byte) 0),
            Instruction.ldn((byte) 31),
            Instruction.sto((byte) 10),
            Instruction.sub((byte) 15)
        ));
        program.accept(new ASTvisitor() {

            @Override
            public void setCurrentLine(int line) {

            }

            @Override
            public void visit(Instruction instruction) {
                assertEquals(expectedInstructions.removeFirst(), instruction);
            }

            @Override
            public void visit(Constant constant) {
                fail("Didn't expect a constant");
            }
        });
    }


    @Test(expected = Exception.class)
    public void testInstructionWithoutEOL() throws Exception {
        ParserImpl parser = program("0 jmp 1");

        parser.parse();
    }

    @Test
    public void testInstructionWithoutProperArgument() throws Exception {
        ParserImpl parser = program("0 jmp ffff\n");

        parser.parse();
        assertTrue(parser.hasSyntaxErrors());
    }

    @Test
    public void testConstantIsTranslatedCorrectly() throws Exception {
        ParserImpl parser = program(
            "0 NUM 5\n"
        );

        Program program = (Program) parser.parse().value;

        assertFalse(parser.hasSyntaxErrors());
        assertConstant(program, 5);
    }

    @Test
    public void testHexadecimalConstant() throws Exception {
        ParserImpl parser = program(
            "0 NUM -0x20\n"
        );

        Program program = (Program) parser.parse().value;
        assertFalse(parser.hasSyntaxErrors());

        assertConstant(program, -32);
    }

    @Test
    public void testStartingPointIsAccepted() throws Exception {
        ParserImpl parser = program("0 jmp 1\nstart:\n3 cmp\n");

        Program program = (Program) parser.parse().value;
        assertFalse(parser.hasSyntaxErrors());
        assertEquals(3, program.getStartLine());
    }

    @Test
    public void testIndexOfLineThenCommentWorks() throws Exception {
        ParserImpl parser = program("0 --comment\n");

        Program program = (Program) parser.parse().value;
        assertFalse(parser.hasSyntaxErrors());
    }

    private void assertConstant(Program program, int value) throws Exception {
        program.accept(new ASTvisitor() {

            @Override
            public void setCurrentLine(int line) {

            }

            @Override
            public void visit(Instruction instruction) {
                fail("Didn't expect an instruction");
            }

            @Override
            public void visit(Constant constant) {
                assertEquals(new Constant(value), constant);
            }
        });
    }
}
