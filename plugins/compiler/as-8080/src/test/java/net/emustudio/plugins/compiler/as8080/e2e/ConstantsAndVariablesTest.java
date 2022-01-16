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
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class ConstantsAndVariablesTest extends AbstractCompilerTest {

    @Test
    public void testLabelAsConstantWorks() throws Exception {
        compile(
            "here equ 0\n"
                + "mov a,b\n"
                + "jz here"
        );

        assertProgram(
            0x78, 0xCA, 0, 0
        );
    }

    @Test
    public void testConstantAsLabelWorks() throws Exception {
        compile(
            "here equ there\n"
                + "there: mov a,b\n"
                + "jz here"
        );

        assertProgram(
            0x78, 0xCA, 0, 0
        );
    }

    @Test(expected = Exception.class)
    public void testRecursiveConstantDefinitionsDoesNotWork() throws Exception {
        compile(
            "here equ there\n"
                + "there equ here\n"
                + "jz here"
        );
    }

    @Test(expected = Exception.class)
    public void testTwoSameLabelsDoNotWork() throws Exception {
        compile(
            "here:\nhere:\njz here"
        );
    }

    @Test(expected = Exception.class)
    public void testTwoSameConstantsDoNotWork() throws Exception {
        compile(
            "here equ 0\nhere equ 1"
        );
    }

    @Test
    public void testVariableCanBeOverwritten() throws Exception {
        compile(
            "here set 0\nhere set 1\ncpi here"
        );
        assertProgram(
            0xFE, 1
        );
    }

    @Test(expected = Exception.class)
    public void testCannotSetVariableBecauseIdentifierIsAlreadyDefined() throws Exception {
        compile(
            "here equ 0\nhere set 1\n"
        );
    }

    @Test(expected = Exception.class)
    public void testCannotDefineConstantBecauseIdentifierIsAlreadyDefined() throws Exception {
        compile(
            "here: db 4\nhere equ 1\n"
        );
    }

    @Test
    public void testForwardReferenceOfConstantShouldWork() throws Exception {
        compile("LXI SP,STACK\n" +
            "TEMPP: DW TEMP0\n" +
            "TEMP0: DS 1\n" +
            "STACK EQU TEMPP+256");

        assertProgram(
            0x31, 0x03, 0x01, 5, 0, 0
        );
    }

    @Test(expected = Exception.class)
    public void testUnknownIdentifier() throws Exception {
        compile("LXI SP,STACK");
    }
}
