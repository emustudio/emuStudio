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
package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

public class IncludeTest extends AbstractCompilerTest {
    private String sampleFile;
    private String sample2File;

    @Before
    public void setup() {
        sampleFile = Objects.requireNonNull(getClass().getResource("/sample.asm")).getFile();
        sample2File = Objects.requireNonNull(getClass().getResource("/sample2.asm")).getFile();
    }

    @Test
    public void testIncludeAndForwardCall() {
        compile(
                "call sample\n"
                        + "include '" + sampleFile + "'\n"
        );

        assertProgram(
                0xCD, 0x03, 0x00, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testCallDataInclude() {
        compile(
                "call sample\n" +
                        "label: db 'hello'\n" +
                        "include '" + sampleFile + "'\n"
        );
        assertProgram(
                0xCD, 0x08, 0x00, 'h', 'e', 'l', 'l', 'o', 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testDoubleIncludeAndForwardCall() {
        compile(
                "call sample2\n"
                        + "include '" + sampleFile + "'\n"
                        + "include '" + sample2File + "'\n"
        );

        assertProgram(
                0xCD, 0x06, 0x00, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testIncludeAndBackwardCall() {
        compile(
                "include '" + sampleFile + "'\n"
                        + "call sample\n"
        );

        assertProgram(
                0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testDoubleIncludeAndBackwardCall() {
        compile(
                "include '" + sampleFile + "'\n"
                        + "include '" + sample2File + "'\n"
                        + "call sample\n"
        );

        assertProgram(
                0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testIncludeAndJMPafter() {
        compile(
                "jp next\n"
                        + "include '" + sampleFile + "'\n"
                        + "next:\n"
                        + "ld a, b\n"
        );

        assertProgram(
                0xC3, 0x06, 0, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testDoubleIncludeAndJMPafter() {
        compile(
                "jp next\n"
                        + "include '" + sampleFile + "'\n"
                        + "include '" + sample2File + "'\n"
                        + "next:\n"
                        + "ld a, b\n"
        );

        assertProgram(
                0xC3, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }
}
