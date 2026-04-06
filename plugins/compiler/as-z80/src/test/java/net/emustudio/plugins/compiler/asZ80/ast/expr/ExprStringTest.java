/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprStringTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalSingleChar() {
        ExprString expr = new ExprString(POS, "A");
        Evaluated result = expr.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals('A' & 0xFF, result.value);
    }

    @Test
    public void testEvalMultiCharReturnsEmpty() {
        ExprString expr = new ExprString(POS, "AB");
        Evaluated result = expr.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test
    public void testEvalEmptyStringReturnsEmpty() {
        ExprString expr = new ExprString(POS, "");
        Evaluated result = expr.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test
    public void testEqualsSame() {
        ExprString e1 = new ExprString(POS, "hello");
        ExprString e2 = new ExprString(POS, "hello");
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferent() {
        ExprString e1 = new ExprString(POS, "hello");
        ExprString e2 = new ExprString(POS, "world");
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprString e = new ExprString(POS, "hello");
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprString e = new ExprString(POS, "hello");
        assertTrue(e.toString().contains("ExprString(hello)"));
    }

    @Test
    public void testMkCopy() {
        ExprString original = new ExprString(POS, "test");
        Node copy = original.copy();
        assertTrue(copy instanceof ExprString);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStringThrows() {
        new ExprString(POS, null);
    }
}
