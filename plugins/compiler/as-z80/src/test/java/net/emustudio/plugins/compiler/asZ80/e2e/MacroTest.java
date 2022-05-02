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
package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class MacroTest extends AbstractCompilerTest {

    @Test
    public void testMacroWithoutCallDoesNotGenerateCode() throws Exception {
        compile(
            "shrt macro\n"
                + "  rrca\n"
                + "  and 7Fh\n"
                + "endm\n\n"
        );
        assertProgram();
    }

    @Test
    public void testMacroWithoutParams() throws Exception {
        compile(
            "shrt macro\n"
                + "  rrca\n"
                + "  and 7Fh\n"
                + "endm\n\n"
                + "shrt\n"
        );

        assertProgram(
            0x0F, 0xE6, 0x7F
        );
    }

    @Test
    public void testMacroWithParams() throws Exception {
        compile(
            "shrt macro amount, dbsize\n"
                + "  rrca\n"
                + "  and amount\n"
                + "  db dbsize\n"
                + "endm\n\n"
                + "shrt 7Fh, 0\n"
        );

        assertProgram(
            0x0F, 0xE6, 0x7F
        );
    }

    @Test
    public void testDBinMacroIsVisibleFromOutside() throws Exception {
        compile(
            "shrt macro\n"
                + "  text: db 0Fh\n"
                + "  and 7Fh\n"
                + "endm\n\n"
                + "shrt\n"
                + "ld HL, text\n"
        );
        assertProgram(
            0x0F, 0xE6, 0x7F, 0x21, 0, 0
        );
    }

    @Test(expected = Exception.class)
    public void testCannotRedefineIdentifierInMacro() throws Exception {
        compile(
            "hello: db 0\n"
                + "shrt macro\n"
                + "  hello equ 0Fh\n"
                + "endm\n"
                + "shrt\n"
        );
    }

    @Test(expected = Exception.class)
    public void testMacroAlreadyDefined() throws Exception {
        compile(
            "shrt macro\nendm\n"
                + "shrt macro\nendm\n"
        );
    }

    @Test
    public void testMacroCanGetForwardLabelReferences() throws Exception {
        compile(
            "shrt macro param\n"
                + "  ld HL, param\n"
                + "endm\n"
                + "shrt text\n"
                + "text: db 1\n"
        );
        assertProgram(
            0x21, 3, 0, 1
        );
    }

    @Test(expected = Exception.class)
    public void testLessMacroParametersThanExpected() throws Exception {
        compile(
            "shrt macro param\n"
                + "  lD HL, param\n"
                + "endm\n"
                + "shrt\n"
        );
    }

    @Test(expected = Exception.class)
    public void testMoreMacroParametersThanExpected() throws Exception {
        compile(
            "shrt macro param\n"
                + "  ld HL, param\n"
                + "endm\n"
                + "shrt 1, 2\n"
        );
    }

    @Test(expected = Exception.class)
    public void testCallUndefinedMacro() throws Exception {
        compile(
            "shrt 1,2\n"
        );
    }
}
