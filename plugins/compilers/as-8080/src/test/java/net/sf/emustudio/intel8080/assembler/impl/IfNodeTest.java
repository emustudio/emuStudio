/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

public class IfNodeTest extends AbstractCompilerTest {

    @Test
    public void testIfNodeIsProcessed() throws Exception {
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
    public void testIfNodeIsNotProcessed() throws Exception {
        compile(
            "if 0\n"
                + "  rrc\n"
                + "endif"
        );

        assertProgram();
    }

    @Test
    public void testIfNoteIsProcessedForNegativeExpression() throws Exception {
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
    public void testIfCanEvaluateBackwardReferenceInExpression() throws Exception {
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

    @Test
    public void testIfCannotEvaluateForwardReferenceInExpression() throws Exception {
        compile(
            "if present\n"
                + "  rrc\n"
                + "endif\n"
                + "present equ 1\n"
        );

        assertError();
    }

    @Test
    public void testIfCannotRedefineIdentifierInside() throws Exception {
        compile(
            "text: db 6\n"
                + "if 554\n"
                + "  text: db 5\n"
                + "endif"
        );

        assertError();
    }
}
