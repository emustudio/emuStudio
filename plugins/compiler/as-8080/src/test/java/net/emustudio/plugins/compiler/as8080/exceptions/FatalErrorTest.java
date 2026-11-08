/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.exceptions;

import net.emustudio.emulib.plugins.compiler.CompileException;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.CompileError;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import org.junit.Test;

import static org.junit.Assert.*;

public class FatalErrorTest {
    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.asm");

    @Test
    public void testFatalErrorMessage() {
        FatalError e = new FatalError(POS, "something bad");
        assertTrue(e.getMessage().contains("Fatal error"));
        assertTrue(e.getMessage().contains("something bad"));
        assertSame(POS, e.position);
    }

    @Test
    public void testFatalErrorIsCompileException() {
        assertTrue(new FatalError(POS, "x") instanceof CompileException);
    }

    @Test(expected = FatalError.class)
    public void testFatalErrorNowWithPositionAndWhy() {
        FatalError.now(POS, "reason");
    }

    @Test
    public void testFatalErrorNowWithCompileError() {
        CompileError error = CompileError.notDefined(new DataDB(POS), "x");
        FatalError e = assertThrows(FatalError.class, () -> FatalError.now(error));
        assertSame(POS, e.position);
        assertTrue(e.getMessage().contains(error.msg));
    }
}
