/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

public class IfNodeTest extends AbstractCompilerTest {

    @Test
    public void testIfNodeIsProcessed() {
        compile(
                "if 1\n"
                        + "  rrca\n"
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
                        + "  rrca\n"
                        + "endif"
        );

        assertProgram();
    }

    @Test
    public void testIfNoteIsProcessedForNegativeExpression() {
        compile(
                "if -1\n"
                        + "  rrca\n"
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
                        + "  rrca\n"
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
