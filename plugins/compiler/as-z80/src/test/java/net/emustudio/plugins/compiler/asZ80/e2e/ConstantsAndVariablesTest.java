/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.e2e;

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
                "here equ 0\nhere var 1\n"
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
        compile("ld SP,STACK\n" +
                "TEMPP: DW TEMP0\n" +
                "TEMP0: DS 1\n" +
                "STACK EQU TEMPP+256");

        assertProgram(
                0x31, 0x03, 0x01, 5, 0, 0
        );
    }

    @Test(expected = Exception.class)
    public void testUnknownIdentifier() throws Exception {
        compile("ld SP,STACK");
    }
}
