/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class ConstantsAndVariablesTest extends AbstractCompilerTest {

    @Test
    public void testLabelAsConstantWorks() {
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
    public void testConstantAsLabelWorks() {
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
    public void testRecursiveConstantDefinitionsDoesNotWork() {
        compile(
                "here equ there\n"
                        + "there equ here\n"
                        + "jz here"
        );
    }

    @Test(expected = Exception.class)
    public void testTwoSameLabelsDoNotWork() {
        compile(
                "here:\nhere:\njz here"
        );
    }

    @Test(expected = Exception.class)
    public void testTwoSameConstantsDoNotWork() {
        compile(
                "here equ 0\nhere equ 1"
        );
    }

    @Test
    public void testVariableCanBeOverwritten() {
        compile(
                "here set 0\nhere set 1\ncpi here"
        );
        assertProgram(
                0xFE, 1
        );
    }

    @Test(expected = Exception.class)
    public void testCannotSetVariableBecauseIdentifierIsAlreadyDefined() {
        compile(
                "here equ 0\nhere set 1\n"
        );
    }

    @Test(expected = Exception.class)
    public void testCannotDefineConstantBecauseIdentifierIsAlreadyDefined() {
        compile(
                "here: db 4\nhere equ 1\n"
        );
    }

    @Test
    public void testForwardReferenceOfConstantShouldWork() {
        compile("LXI SP,STACK\n" +
                "TEMPP: DW TEMP0\n" +
                "TEMP0: DS 1\n" +
                "STACK EQU TEMPP+256");

        assertProgram(
                0x31, 0x03, 0x01, 5, 0, 0
        );
    }

    @Test(expected = Exception.class)
    public void testUnknownIdentifier() {
        compile("LXI SP,STACK");
    }
}
