/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
