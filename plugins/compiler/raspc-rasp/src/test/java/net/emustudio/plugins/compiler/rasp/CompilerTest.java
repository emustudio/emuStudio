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
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.plugins.compiler.rasp.ast.Program.RASPMemoryCellImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testJmpInstruction() throws Exception {
        compile(
                "org 2\n" +
                        "START: jmp HERE\n" +
                        "jmp START\n" +
                        "HERE: halt"
        );

        assertProgram(
                null,
                null,
                new RASPMemoryCellImpl(true, 15, 2),
                new RASPMemoryCellImpl(false, 6, 3),
                new RASPMemoryCellImpl(true, 15, 4),
                new RASPMemoryCellImpl(false, 2, 5),
                new RASPMemoryCellImpl(true, 18, 6)
        );
    }

    @Test(expected = Exception.class)
    public void testNonExistingLabel() throws Exception {
        compile("jmp hahaha");
    }

    @Test(expected = Exception.class)
    public void testAlreadyDefinedLabel() throws Exception {
        compile("label:\nlabel:");
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }
}
