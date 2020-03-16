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

public class ConstantsAndVariablesTest extends AbstractCompilerTest {

    @Test
    public void testLabelAsConstantWorks() throws Exception {
        compile(
            "here equ 0\n"
                + "ld a,b\n"
                + "jp z, here"
        );

        assertProgram(
            0x78, 0xCA, 0, 0
        );
    }

    @Test
    public void testConstantAsLabelWorks() throws Exception {
        compile(
            "here equ there\n"
                + "there: ld a,b\n"
                + "jp z, here"
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
                + "jp z, here"
        );
    }

    @Test(expected = Exception.class)
    public void testTwoSameLabelsDoNotWork() throws Exception {
        compile(
            "here:\nhere:\njp z, here"
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
            "here var 0\nhere var 1\ncp here"
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
}
