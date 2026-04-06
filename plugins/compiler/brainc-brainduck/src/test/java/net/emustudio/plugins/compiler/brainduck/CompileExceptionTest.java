/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import org.junit.Test;

import static org.junit.Assert.*;

public class CompileExceptionTest {

    @Test
    public void testConstructorAndGetMessage() {
        CompileException exception = new CompileException(5, 10, "unexpected token");
        assertEquals("unexpected token", exception.getMessage());
        assertEquals(5, exception.line);
        assertEquals(10, exception.column);
    }

    @Test
    public void testToString() {
        CompileException exception = new CompileException(3, 7, "syntax error");
        assertEquals("line 3:7 syntax error", exception.toString());
    }

    @Test
    public void testToStringWithZeroLineAndColumn() {
        CompileException exception = new CompileException(0, 0, "error at start");
        assertEquals("line 0:0 error at start", exception.toString());
    }
}

