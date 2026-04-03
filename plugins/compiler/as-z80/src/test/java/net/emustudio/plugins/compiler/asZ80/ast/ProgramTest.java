/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgramTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testConstructorWithFileName() {
        Program program = new Program("test.asm");
        assertNotNull(program.env());
    }

    @Test
    public void testConstructorWithPositionAndEnv() {
        NameSpace env = new NameSpace();
        Program program = new Program(POS, env);
        assertSame(env, program.env());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullEnv() {
        new Program(POS, null);
    }

    @Test
    public void testMkCopy() {
        NameSpace env = new NameSpace();
        Program original = new Program(POS, env);
        Node copy = original.copy();
        assertTrue(copy instanceof Program);
        assertSame(env, ((Program) copy).env());
    }

    @Test
    public void testProgramEquality() {
        Program p1 = new Program("test.asm");
        Program p2 = new Program("test.asm");
        assertEquals(p1, p2);
    }
}

