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
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class Assembler8080Test extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testForwardAbsoluteJump() {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testCallBackward() {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x01, 0x00, 0x77
        );
    }

    @Test
    public void testCallForward() {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x07, 0x00, 0x77
        );
    }

    @Test(expected = Exception.class)
    public void testRSTtooBigArgument() {
        compile("rst 10");
    }

    @Test
    public void testDCXwithLXI() {
        compile(
                "dcx sp\n"
                        + "lxi h, text\n"
                        + "text:\n"
                        + "db 'ahoj'"
        );

        assertProgram(
                0x3B, 0x21, 0x04, 0, 'a', 'h', 'o', 'j'
        );
    }

    @Test
    public void testINthenJMP() {
        compile(
                "jmp sample\n"
                        + "in 10h\n"
                        + "sample:\n"
                        + "mov a, b\n"
        );

        assertProgram(
                0xC3, 0x5, 0, 0xDB, 0x10, 0x78
        );
    }

    @Test
    public void testGetChar() {
        compile(
                "jmp sample\n"
                        + "getchar:\n"
                        + "in 10h\n"
                        + "ani 1\n"
                        + "jz getchar\n"
                        + "in 11h\n"
                        + "out 11h\n"
                        + "ret\n"
                        + "sample:\n"
                        + "mov a, b"
        );

        assertProgram(
                0xC3, 0x0F, 0, 0xDB, 0x10, 0xE6, 1, 0xCA, 0x03, 0, 0xDB, 0x11, 0xD3, 0x11, 0xC9, 0x78
        );
    }
}
