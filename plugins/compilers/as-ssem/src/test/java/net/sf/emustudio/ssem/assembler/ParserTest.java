/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.assembler;

import java_cup.runtime.ComplexSymbolFactory;
import net.sf.emustudio.ssem.assembler.tree.Instruction;
import net.sf.emustudio.ssem.assembler.tree.Program;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    private ParserImpl program(String program) {
        return new ParserImpl(new LexerImpl(new StringReader(program)), new ComplexSymbolFactory());
    }

    @Test
    public void testInstructions() throws Exception {
        ParserImpl parser = program(
            "cmp // comment\n" +
            "stp\n" +
            "jmp 22\n" +
            "jrp 0\n" +
            "ldn 31\n" +
            "sto 10\n" +
            "sub 15\n"
        );

        Program program = (Program) parser.parse().value;
        assertFalse(parser.hasSyntaxErrors());

        Deque<Instruction> expectedInstructions = new LinkedList<>(Arrays.asList(
            Instruction.cmp(),
            Instruction.stp(),
            Instruction.jmp((byte)22),
            Instruction.jrp((byte)0),
            Instruction.ldn((byte)31),
            Instruction.sto((byte)10),
            Instruction.sub((byte)15)
        ));
        program.accept(instruction -> assertEquals(expectedInstructions.removeLast(), instruction));
    }


    @Test(expected = Exception.class)
    public void testInstructionWithoutEOL() throws Exception {
        ParserImpl parser = program("jmp 1");

        parser.parse();
    }

    @Test
    public void testInstructionWithoutProperArgument() throws Exception {
        ParserImpl parser = program("jmp ffff\n");

        parser.parse();
        assertTrue(parser.hasSyntaxErrors());
    }
}
