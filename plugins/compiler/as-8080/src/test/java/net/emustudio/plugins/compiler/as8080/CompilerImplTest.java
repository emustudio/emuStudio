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
package net.emustudio.plugins.compiler.as8080;

import org.junit.Test;

public class CompilerImplTest extends AbstractCompilerTest {

    @Test
    public void testForwardAbsoluteJump() throws Exception {
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
    public void testBackwardAbsoluteJump() throws Exception {
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
    public void testCallBackward() throws Exception {
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
    public void testCallForward() throws Exception {
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
    public void testRSTtooBigArgument() throws Exception {
        compile("rst 10");
    }

    @Test
    public void testDCXwithLXI() throws Exception {
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
    public void testINthenJMP() throws Exception {
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
    public void testGetChar() throws Exception {
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
