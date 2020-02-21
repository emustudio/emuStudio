/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.impl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class CompilerImplTest extends AbstractCompilerTest {

    @Test
    public void testForwardAbsoluteJump() throws Exception {
        compile(
            "now: ld a, b\n" +
                "cp \"C\"\n" +
                "jp z, ler\n" +
                "ler: ld (hl), a"
        );

        assertProgram(
            0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() throws Exception {
        compile(
            "now: ld a,b\n" +
                "cp \"C\"\n" +
                "jp z, now\n" +
                "ler: ld (hl), a"
        );

        assertProgram(
            0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testForwardRelativeJump() throws Exception {
        compile(
            "now: ld a, b\n" +
                "cp \"C\"\n" +
                "jp z, ler\n" +
                "ler: ld (hl), a"
        );

        assertProgram(
            0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testRSTtooBigArgument() throws Exception {
        compile("rst 40h");

        assertFalse(errorCode == 0);
    }

    @Test
    public void testRelativeJumpForward() throws Exception {
        compile(
            "loop: ld A, 0\n" +
                "cp 0\n" +
                "jr Z, end\n" +
                "jp loop\n" +
                "\n" +
                "end:\n" +
                "halt\n"
        );

        assertProgram(
            0x3E, 0, 0xFE, 0, 0x28, 3, 0xC3, 0, 0, 0x76
        );
    }

    @Test
    public void testRelativeJumpBackward() throws Exception {
        compile(
            "loop: ld A, 0\n" +
                "cp 0\n" +
                "jr Z, loop\n" +
                "jp loop\n" +
                "\n" +
                "end:\n" +
                "halt\n"
        );

        assertProgram(
            0x3E, 0, 0xFE, 0, 0x28, (-6) & 0xFF, 0xC3, 0, 0, 0x76
        );
    }

    @Test
    public void testRelativeJumpTooFarBackwards() throws Exception {
        compile(
            "loop: ld A,0\n" +
                "org 130h\n" +
                "cp 0\n" +
                "jr loop\n" +
                "halt"
        );

        assertFalse(errorCode == 0);
    }

    @Test
    public void testRelativeJumpTooFarForwards() throws Exception {
        compile(
            "loop: ld A,0\n" +
                "cp 0\n" +
                "jr end\n" +
                "org 130h\n" +
                "end:\n" +
                "halt"
        );

        assertFalse(errorCode == 0);
    }

    @Test
    public void testDJNZ() throws Exception {
        compile(
            "loop: ld B,1\n" +
                "djnz loop\n" +
                "halt"
        );

        assertProgram(
            0x06, 1, 0x10, (-4) & 0xFF, 0x76
        );
    }

    @Test
    public void testDJNZtooFarBackwards() throws Exception {
        compile(
            "loop: ld B,1\n" +
                "org 130h\n" +
                "djnz loop\n" +
                "halt"
        );

        assertFalse(errorCode == 0);
    }

    @Test
    public void testDJNZtooFarForwards() throws Exception {
        compile(
            "djnz end\n" +
                "org 130h\n" +
                "end:\n" +
                "halt"
        );

        assertFalse(errorCode == 0);
    }

}
