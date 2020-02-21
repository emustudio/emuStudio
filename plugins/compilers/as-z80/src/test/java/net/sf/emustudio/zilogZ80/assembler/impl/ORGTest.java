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

import java.io.File;

public class ORGTest extends AbstractCompilerTest {

    @Test
    public void testORGwithInclude() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
            "org 3\n"
                + "call sample\n"
                + "include \"" + includeFile.getAbsolutePath() + "\"\n"
        );

        assertProgram(
            0, 0, 0, 0xCD, 06, 00, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleInclude() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "org 3\n"
                + "call sample\n"
                + "include \"" + second.getAbsolutePath() + "\"\n"
                + "include \"" + first.getAbsolutePath() + "\"\n"
        );

        assertProgram(
            0, 0, 0, 0xCD, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleIncludeAndJMPafter() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "org 3\n"
                + "jp next\n"
                + "include \"" + first.getAbsolutePath() + "\"\n"
                + "include \"" + second.getAbsolutePath() + "\"\n"
                + "next:\n"
                + "ld a, b\n"
        );

        assertProgram(
            0, 0, 0, 0xC3, 0x0C, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testORGwithDB() throws Exception {
        compile(
            "org 3\n"
                + "ld hl, text\n"
                + "text:\n"
                + "db \"ahoj\""
        );

        assertProgram(
            0, 0, 0, 0x21, 0x06, 0, 'a', 'h', 'o', 'j'
        );
    }

    @Test
    public void testORG() throws Exception {
        compile(
            "org 2\n" +
                "now: ld a,b\n" +
                "ds 2\n" +
                "cp \"C\"\n" +
                "jp z, now\n" +
                "ler: ld (hl), a"
        );

        assertProgram(
            0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testORGwithJumpBackwards() throws Exception {
        compile(
            "sample:\n"
                + "org 2\n"
                + "jp sample"
        );

        assertProgram(
            0, 0, 0xC3, 0, 0
        );
    }

    @Test
    public void testORGwithJumpForwards() throws Exception {
        compile(
            "jp sample\n"
                + "org 5\n"
                + "sample:\n"
                + "ld a, b"
        );

        assertProgram(
            0xC3, 0x05, 0, 0, 0, 0x78
        );
    }

    @Test
    public void testORGdoesNotBreakPreviousMemoryContent() throws Exception {
        memoryStub.write(0, (short) 0x10);
        memoryStub.write(1, (short) 0x11);

        compile(
            "org 2\n" + "now: ld a,b\n"
        );

        assertProgram(
            0x10, 0x11, 0x78
        );
    }

    @Test
    public void testORGthenDSdoNotOverlap() throws Exception {
        compile(
            "org 2\nds 2\nld a,b"
        );
        assertProgram(
            0, 0, 0, 0, 0x78
        );
    }

    @Test
    public void testORGisAmbiguous() throws Exception {
        compile(
            "org text\nld a, 4\ntext: db 4\n"
        );

        assertError();
    }

    @Test
    public void testTwoSuccessiveORG() throws Exception {
        compile(
            "org 2\norg 3\nhalt"
        );
        assertProgram(
            0, 0, 0, 0x76
        );
    }


}
