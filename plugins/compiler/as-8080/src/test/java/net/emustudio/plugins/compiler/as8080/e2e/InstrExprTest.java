/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class InstrExprTest extends AbstractCompilerTest {

    @Test
    public void testRST() {
        compile(
                "JMP EXAMPLE\n" +
                        "RST 00H\n" +
                        "EXAMPLE:\n" +
                        "MVI A,01H"
        );

        assertProgram(
                0xC3, 0x04, 0x00, 0xC7, 0x3E, 0x01
        );
    }

    @Test
    public void testCPI() {
        compile("cpi '9' + 1");
        assertProgram(0xFE, '9' + 1);
    }

    @Test
    public void testForwardCall() {
        compile("call sample\n" +
                "label: db 'hello'\n" +
                "sample: hlt");
        assertProgram(0xCD, 0x08, 0x00, 'h', 'e', 'l', 'l', 'o', 0x76);
    }
}
