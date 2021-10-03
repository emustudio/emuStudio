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
package net.emustudio.plugins.compiler.as8080;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
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

    @Test(expected = Exception.class)
    public void testIfCannotEvaluateForwardReferenceInExpression() throws Exception {
        compile(
            "if present\n"
                + "  rrc\n"
                + "endif\n"
                + "present equ 1\n"
        );
    }

    @Test(expected = Exception.class)
    public void testIfCannotRedefineIdentifierInside() throws Exception {
        compile(
            "text: db 6\n"
                + "if 554\n"
                + "  text: db 5\n"
                + "endif"
        );
    }
}
