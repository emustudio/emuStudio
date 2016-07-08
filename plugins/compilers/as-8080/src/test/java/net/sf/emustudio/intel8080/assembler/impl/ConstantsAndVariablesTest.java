/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.intel8080.assembler.impl;

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

    @Test
    public void testRecursiveConstantDefinitionsDoesNotWork() throws Exception {
        compile(
                "here equ there\n"
                        + "there equ here\n"
                        + "jz here"
        );

        assertError();
    }

    @Test
    public void testTwoSameLabelsDoNotWork() throws Exception {
        compile(
                "here:\nhere:\njz here"
        );

        assertError();
    }

    @Test
    public void testTwoSameConstantsDoNotWork() throws Exception {
        compile(
                "here equ 0\nhere equ 1"
        );

        assertError();
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

    @Test
    public void testCannotSetVariableBecauseIdentifierIsAlreadyDefined() throws Exception {
        compile(
            "here equ 0\nhere set 1\n"
        );

        assertError();
    }

    @Test
    public void testCannotDefineConstantBecauseIdentifierIsAlreadyDefined() throws Exception {
        compile(
            "here: db 4\nhere equ 1\n"
        );

        assertError();
    }
}
