/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.CompileError;
import org.junit.Test;

import static org.junit.Assert.*;

public class NameSpaceTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testPutAndGet() {
        NameSpace ns = new NameSpace();
        Evaluated eval = new Evaluated(POS, 42);
        ns.put("x", eval);
        Evaluated result = ns.get("x");
        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    public void testGetUndefined() {
        NameSpace ns = new NameSpace();
        Evaluated result = ns.get("undefined");
        assertNull(result);
    }

    @Test
    public void testPutNull() {
        NameSpace ns = new NameSpace();
        ns.put("x", null);
        Evaluated result = ns.get("x");
        assertNull(result);
    }

    @Test
    public void testRemove() {
        NameSpace ns = new NameSpace();
        ns.put("x", new Evaluated(POS, 42));
        ns.remove("x");
        assertNull(ns.get("x"));
    }

    @Test
    public void testRemoveNonExistent() {
        NameSpace ns = new NameSpace();
        // Should not throw
        ns.remove("nonexistent");
    }

    @Test
    public void testErrorAndHasError() {
        NameSpace ns = new NameSpace();
        assertTrue(ns.hasNoErrors());

        ExprNumberForError node = new ExprNumberForError(POS);
        CompileError error = CompileError.notDefined(node, "something");
        ns.error(error);

        assertFalse(ns.hasNoErrors());
        assertTrue(ns.hasError(CompileError.ERROR_NOT_DEFINED));
        assertFalse(ns.hasError(CompileError.ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testGetErrors() {
        NameSpace ns = new NameSpace();
        ExprNumberForError node = new ExprNumberForError(POS);
        CompileError error = CompileError.notDefined(node, "something");
        ns.error(error);

        assertEquals(1, ns.getErrors().size());
        assertEquals(error, ns.getErrors().get(0));
    }

    @Test
    public void testGetErrorsUnmodifiable() {
        NameSpace ns = new NameSpace();
        try {
            ns.getErrors().add(CompileError.notDefined(new ExprNumberForError(POS), "x"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void testErrorNull() {
        NameSpace ns = new NameSpace();
        ns.error(null);
    }

    @Test
    public void testToString() {
        NameSpace ns = new NameSpace();
        String result = ns.toString();
        assertNotNull(result);
        assertTrue(result.contains("NameSpace"));
        assertTrue(result.contains("errors"));
    }

    @Test
    public void testOverwriteDefinition() {
        NameSpace ns = new NameSpace();
        ns.put("x", new Evaluated(POS, 1));
        ns.put("x", new Evaluated(POS, 2));
        assertEquals(2, ns.get("x").value);
    }

    // Simple concrete Node subclass for testing
    private static class ExprNumberForError extends Node {
        ExprNumberForError(SourceCodePosition position) {
            super(position);
        }

        @Override
        protected Node mkCopy() {
            return new ExprNumberForError(position);
        }
    }
}
