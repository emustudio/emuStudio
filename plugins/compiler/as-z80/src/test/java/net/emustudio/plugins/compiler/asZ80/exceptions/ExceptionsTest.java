/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.exceptions;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.CompileError;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionsTest {
    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.asm");

    // CompileException tests

    @Test
    public void testCompileExceptionMessage() {
        CompileException e = new CompileException(POS, "test error");
        assertTrue(e.getMessage().contains("test error"));
        assertSame(POS, e.position);
    }

    @Test
    public void testCompileExceptionWithCause() {
        RuntimeException cause = new RuntimeException("cause");
        CompileException e = new CompileException(POS, "test error", cause);
        assertTrue(e.getMessage().contains("test error"));
        assertSame(cause, e.getCause());
        assertSame(POS, e.position);
    }

    @Test(expected = NullPointerException.class)
    public void testCompileExceptionNullPosition() {
        new CompileException(null, "msg");
    }

    // FatalError tests

    @Test
    public void testFatalErrorMessage() {
        FatalError e = new FatalError(POS, "something bad");
        assertTrue(e.getMessage().contains("Fatal error"));
        assertTrue(e.getMessage().contains("something bad"));
    }

    @Test(expected = FatalError.class)
    public void testFatalErrorNowWithPositionAndWhy() {
        FatalError.now(POS, "reason");
    }

    @Test(expected = FatalError.class)
    public void testFatalErrorNowWithCompileError() {
        CompileError error = CompileError.notDefined(new DataDB(POS), "x");
        FatalError.now(error);
    }

    // SyntaxErrorException tests

    @Test
    public void testSyntaxErrorExceptionMessage() {
        SyntaxErrorException e = new SyntaxErrorException(POS, "unexpected token");
        assertTrue(e.getMessage().contains("unexpected token"));
        assertSame(POS, e.position);
    }

    @Test
    public void testSyntaxErrorExceptionWithCause() {
        RuntimeException cause = new RuntimeException("cause");
        SyntaxErrorException e = new SyntaxErrorException(POS, "unexpected token", cause);
        assertTrue(e.getMessage().contains("unexpected token"));
        assertSame(cause, e.getCause());
    }
}

