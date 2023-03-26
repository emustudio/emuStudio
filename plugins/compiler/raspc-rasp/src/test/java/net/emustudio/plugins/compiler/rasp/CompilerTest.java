/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.rasp;

import org.junit.Test;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.junit.Assert.assertNotEquals;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testJmpInstruction() throws Exception {
        compile(
                "org 22\n" +
                        "START: jmp HERE\n" +
                        "jmp START\n" +
                        "HERE: halt"
        );

        assertProgram(
                null,
                null,
                15,
                26,
                15,
                22,
                18
        );
    }

    @Test(expected = Exception.class)
    public void testNonExistingLabel() throws Exception {
        compile("jmp hahaha");
    }

    @Test(expected = Exception.class)
    public void testAlreadyDefinedLabel() throws Exception {
        compile("label:\nlabel:");
    }


    @Test
    public void testREAD() throws Exception {
        compile("READ 5");
        assertProgram(READ, 5);
    }

    @Test
    public void testWRITE() throws Exception {
        compile("WRITE =3\nWRITE 4");
        assertProgram(2, 3, 3, 4);
    }

    @Test
    public void testLOAD() throws Exception {
        compile("LOAD =6\nLOAD 7");
        assertProgram(4, 6, 5, 7);
    }

    @Test
    public void testSTORE() throws Exception {
        compile("STORE 111111112");
        assertProgram(6, 111111112);
    }

    @Test
    public void testADD() throws Exception {
        compile("ADD =2\nADD 99");
        assertProgram(7, 2, 8, 99);
    }

    @Test
    public void testSUB() throws Exception {
        compile("SUB =3\nSUB 229");
        assertProgram(9, 3, 10, 229);
    }

    @Test
    public void testMUL() throws Exception {
        compile("MUL =-5\nMUL 229");
        assertProgram(11, -5, 12, 229);
    }

    @Test
    public void testDIV() throws Exception {
        compile("DIV =0\nDIV 229");
        assertProgram(13, 0, 14, 229);
    }

    @Test
    public void testJMP() throws Exception {
        compile("here: JMP here");
        assertProgram(JMP, 20);
    }

    @Test
    public void testJMP_ForwardReference() throws Exception {
        compile("JMP here\nhere:HALT");
        assertProgram(JMP, 22, HALT);
    }

    @Test
    public void testJZ() throws Exception {
        compile("JZ here\nhere:HALT");
        assertProgram(JZ, 22, HALT);
    }

    @Test
    public void testJGTZ() throws Exception {
        compile("JGTZ here\nhere:HALT");
        assertProgram(JGTZ, 22, HALT);
    }

    @Test
    public void testHALT() throws Exception {
        compile("halt");
        assertProgram(HALT);
    }

    @Test(expected = Exception.class)
    public void testNegativeRegistersAreNotSupported() throws Exception {
        compile("STORE -2");
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }
}
