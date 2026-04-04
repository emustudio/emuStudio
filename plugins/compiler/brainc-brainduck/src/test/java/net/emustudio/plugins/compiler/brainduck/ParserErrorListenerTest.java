/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import org.antlr.v4.runtime.RecognitionException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParserErrorListenerTest {

    @Test(expected = CompileException.class)
    public void testSyntaxErrorThrowsCompileException() {
        ParserErrorListener listener = new ParserErrorListener();
        listener.syntaxError(null, null, 5, 10, "unexpected token", null);
    }

    @Test
    public void testSyntaxErrorContainsLineAndColumn() {
        ParserErrorListener listener = new ParserErrorListener();
        try {
            listener.syntaxError(null, null, 3, 7, "bad input", null);
            fail("Expected CompileException");
        } catch (CompileException e) {
            assertEquals(3, e.line);
            assertEquals(7, e.column);
            assertEquals("bad input", e.getMessage());
        }
    }
}

