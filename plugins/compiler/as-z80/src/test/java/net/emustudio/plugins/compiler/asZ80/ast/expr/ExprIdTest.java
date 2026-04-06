/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprIdTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalDefined() {
        NameSpace ns = new NameSpace();
        ns.put("myvar", new Evaluated(POS, 42));
        ExprId expr = new ExprId(POS, "myvar");
        Evaluated result = expr.eval(null, ns);
        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    public void testEvalUndefined() {
        ExprId expr = new ExprId(POS, "undefined");
        Evaluated result = expr.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test
    public void testEvalCaseInsensitive() {
        NameSpace ns = new NameSpace();
        ns.put("myvar", new Evaluated(POS, 10));
        ExprId expr = new ExprId(POS, "MYVAR");
        Evaluated result = expr.eval(null, ns);
        assertNotNull(result);
        assertEquals(10, result.value);
    }

    @Test
    public void testEqualsSame() {
        ExprId e1 = new ExprId(POS, "x");
        ExprId e2 = new ExprId(POS, "x");
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferent() {
        ExprId e1 = new ExprId(POS, "x");
        ExprId e2 = new ExprId(POS, "y");
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprId e = new ExprId(POS, "x");
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprId e = new ExprId(POS, "myvar");
        assertTrue(e.toString().contains("ExprId(myvar)"));
    }

    @Test
    public void testMkCopy() {
        ExprId original = new ExprId(POS, "x");
        Node copy = original.copy();
        assertTrue(copy instanceof ExprId);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdThrows() {
        new ExprId(POS, null);
    }
}
