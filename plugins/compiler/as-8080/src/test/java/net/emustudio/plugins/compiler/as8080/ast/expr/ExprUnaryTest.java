/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprUnaryTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalUnaryAdd() {
        ExprUnary unary = new ExprUnary(POS, UnaryOperator.ADD);
        unary.addChild(new ExprNumber(POS, 42));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    public void testEvalUnarySubtract() {
        ExprUnary unary = new ExprUnary(POS, UnaryOperator.SUBTRACT);
        unary.addChild(new ExprNumber(POS, 42));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(-42, result.value);
    }

    @Test
    public void testEvalUnaryNot() {
        ExprUnary unary = new ExprUnary(POS, UnaryOperator.NOT);
        unary.addChild(new ExprNumber(POS, 0));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(~0, result.value);
    }

    @Test
    public void testEvalUnaryNot2() {
        ExprUnary unary = new ExprUnary(POS, UnaryOperator.NOT);
        unary.addChild(new ExprNumber(POS, 0xFF));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(~0xFF, result.value);
    }

    @Test
    public void testEvalWithUnresolvableChild() {
        ExprUnary unary = new ExprUnary(POS, UnaryOperator.ADD);
        unary.addChild(new ExprId(POS, "undefined"));
        Evaluated result = unary.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testUnknownOperationThrows() {
        new ExprUnary(POS, null);
    }

    @Test
    public void testEqualsSame() {
        ExprUnary e1 = new ExprUnary(POS, UnaryOperator.ADD);
        ExprUnary e2 = new ExprUnary(POS, UnaryOperator.ADD);
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferentOp() {
        ExprUnary e1 = new ExprUnary(POS, UnaryOperator.ADD);
        ExprUnary e2 = new ExprUnary(POS, UnaryOperator.SUBTRACT);
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprUnary e = new ExprUnary(POS, UnaryOperator.ADD);
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprUnary e = new ExprUnary(POS, UnaryOperator.ADD);
        assertTrue(e.toString().contains("ExprUnary"));
    }

    @Test
    public void testMkCopy() {
        ExprUnary original = new ExprUnary(POS, UnaryOperator.SUBTRACT);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprUnary);
        assertEquals(original, copy);
    }
}
