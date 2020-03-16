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
package net.emustudio.plugins.compiler.asZ80;

import org.junit.Test;

import java.io.File;

public class IncludeTest extends AbstractCompilerTest {

    @Test
    public void testIncludeAndForwardCall() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
            "call sample\n"
                + "include \"" + includeFile.getAbsolutePath() + "\"\n"
        );

        assertProgram(
            0xCD, 03, 00, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testDoubleIncludeAndForwardCall() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "call sample2\n"
                + "include \"" + first.getAbsolutePath() + "\"\n"
                + "include \"" + second.getAbsolutePath() + "\"\n"
        );

        assertProgram(
            0xCD, 06, 00, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testIncludeAndBackwardCall() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
            "include \"" + includeFile.getAbsolutePath() + "\"\n"
                + "call sample\n"
        );

        assertProgram(
            0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testDoubleIncludeAndBackwardCall() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "include \"" + first.getAbsolutePath() + "\"\n"
                + "include \"" + second.getAbsolutePath() + "\"\n"
                + "call sample\n"
        );

        assertProgram(
            0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testIncludeAndJMPafter() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
            "jp next\n"
                + "include \"" + includeFile.getAbsolutePath() + "\"\n"
                + "next:\n"
                + "ld a, b\n"
        );

        assertProgram(
            0xC3, 0x06, 0, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testDoubleIncludeAndJMPafter() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "jp next\n"
                + "include \"" + first.getAbsolutePath() + "\"\n"
                + "include \"" + second.getAbsolutePath() + "\"\n"
                + "next:\n"
                + "ld a, b\n"
        );

        assertProgram(
            0xC3, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }

}
