/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class ExprUnaryTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalUnaryAdd() {
        ExprUnary unary = new ExprUnary(POS, OP_ADD);
        unary.addChild(new ExprNumber(POS, 42));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    public void testEvalUnarySubtract() {
        ExprUnary unary = new ExprUnary(POS, OP_SUBTRACT);
        unary.addChild(new ExprNumber(POS, 42));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(-42, result.value);
    }

    @Test
    public void testEvalUnaryNot() {
        ExprUnary unary = new ExprUnary(POS, OP_NOT);
        unary.addChild(new ExprNumber(POS, 0));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(~0, result.value);
    }

    @Test
    public void testEvalUnaryNot2() {
        ExprUnary unary = new ExprUnary(POS, OP_NOT_2);
        unary.addChild(new ExprNumber(POS, 0xFF));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(~0xFF, result.value);
    }

    @Test
    public void testEvalWithUnresolvableChild() {
        ExprUnary unary = new ExprUnary(POS, OP_ADD);
        unary.addChild(new ExprId(POS, "undefined"));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testUnknownOperationThrows() {
        new ExprUnary(POS, 99999);
    }

    @Test
    public void testEqualsSame() {
        ExprUnary e1 = new ExprUnary(POS, OP_ADD);
        ExprUnary e2 = new ExprUnary(POS, OP_ADD);
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferentOp() {
        ExprUnary e1 = new ExprUnary(POS, OP_ADD);
        ExprUnary e2 = new ExprUnary(POS, OP_SUBTRACT);
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprUnary e = new ExprUnary(POS, OP_ADD);
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprUnary e = new ExprUnary(POS, OP_ADD);
        assertTrue(e.toString().contains("ExprUnary"));
    }

    @Test
    public void testMkCopy() {
        ExprUnary original = new ExprUnary(POS, OP_SUBTRACT);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprUnary);
        assertEquals(original, copy);
    }
}
