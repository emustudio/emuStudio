/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ExprNumberTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEval() {
        ExprNumber expr = new ExprNumber(POS, 42);
        Optional<Evaluated> result = expr.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(42, result.get().value);
    }

    @Test
    public void testEvalZero() {
        ExprNumber expr = new ExprNumber(POS, 0);
        Optional<Evaluated> result = expr.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalNegative() {
        ExprNumber expr = new ExprNumber(POS, -1);
        Optional<Evaluated> result = expr.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(-1, result.get().value);
    }

    @Test
    public void testEqualsSame() {
        ExprNumber e1 = new ExprNumber(POS, 42);
        ExprNumber e2 = new ExprNumber(POS, 42);
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferent() {
        ExprNumber e1 = new ExprNumber(POS, 42);
        ExprNumber e2 = new ExprNumber(POS, 43);
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprNumber e = new ExprNumber(POS, 42);
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprNumber e = new ExprNumber(POS, 42);
        assertTrue(e.toString().contains("ExprNumber(42)"));
    }

    @Test
    public void testMkCopy() {
        ExprNumber original = new ExprNumber(POS, 42);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprNumber);
        assertEquals(original, copy);
    }
}

