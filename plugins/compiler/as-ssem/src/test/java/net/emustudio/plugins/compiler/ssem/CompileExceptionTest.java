/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompileExceptionTest {

    @Test
    public void testToString() {
        SourceCodePosition position = new SourceCodePosition(1, 5, "test.ssem");
        CompileException exception = new CompileException(position, "Some error");
        String result = exception.toString();
        assertNotNull(result);
        assertTrue(result.contains("Some error"));
    }

    @Test
    public void testGetMessage() {
        SourceCodePosition position = new SourceCodePosition(3, 10, "file.ssem");
        CompileException exception = new CompileException(position, "Bad instruction");
        assertEquals("Bad instruction", exception.getMessage());
    }

    @Test
    public void testPositionIsStored() {
        SourceCodePosition position = new SourceCodePosition(2, 0, "x.ssem");
        CompileException exception = new CompileException(position, "err");
        assertSame(position, exception.position);
    }

    @Test(expected = NullPointerException.class)
    public void testNullPositionThrows() {
        new CompileException(null, "msg");
    }
}

