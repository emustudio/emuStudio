/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class IfNodeTest extends AbstractCompilerTest {

    @Test
    public void testIfNodeIsProcessed() {
        compile(
                "if 1\n"
                        + "  rrc\n"
                        + "endif"
        );

        assertProgram(
                0x0F
        );
    }

    @Test
    public void testIfNodeIsNotProcessed() {
        compile(
                "if 0\n"
                        + "  rrc\n"
                        + "endif"
        );

        assertProgram();
    }

    @Test
    public void testIfNoteIsProcessedForNegativeExpression() {
        compile(
                "if -1\n"
                        + "  rrc\n"
                        + "endif"
        );

        assertProgram(
                0x0F
        );
    }

    @Test
    public void testIfCanEvaluateBackwardReferenceInExpression() {
        compile(
                "present equ 1\n"
                        + "if present\n"
                        + "  rrc\n"
                        + "endif\n"
        );

        assertProgram(
                0x0F
        );
    }

    @Test(expected = Exception.class)
    public void testIfCannotRedefineIdentifierInside() {
        compile(
                "text: db 6\n"
                        + "if 554\n"
                        + "  text: db 5\n"
                        + "endif"
        );
    }
}
