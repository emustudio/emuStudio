/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprInfixTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    private ExprInfix makeInfix(InfixOperator op, int left, int right) {
        ExprInfix infix = new ExprInfix(POS, op);
        infix.addChild(new ExprNumber(POS, left));
        infix.addChild(new ExprNumber(POS, right));
        return infix;
    }

    @Test
    public void testEvalAdd() {
        ExprInfix infix = makeInfix(InfixOperator.ADD, 3, 4);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(7, result.value);
    }

    @Test
    public void testEvalSubtract() {
        ExprInfix infix = makeInfix(InfixOperator.SUBTRACT, 10, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(7, result.value);
    }

    @Test
    public void testEvalMultiply() {
        ExprInfix infix = makeInfix(InfixOperator.MULTIPLY, 3, 4);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(12, result.value);
    }

    @Test
    public void testEvalDivide() {
        ExprInfix infix = makeInfix(InfixOperator.DIVIDE, 10, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(3, result.value);
    }

    @Test
    public void testEvalMod() {
        ExprInfix infix = makeInfix(InfixOperator.MOD, 10, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalMod2() {
        ExprInfix infix = makeInfix(InfixOperator.MOD, 10, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalAnd() {
        ExprInfix infix = makeInfix(InfixOperator.AND, 0xFF, 0x0F);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0x0F, result.value);
    }

    @Test
    public void testEvalOr() {
        ExprInfix infix = makeInfix(InfixOperator.OR, 0xF0, 0x0F);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0xFF, result.value);
    }

    @Test
    public void testEvalXor() {
        ExprInfix infix = makeInfix(InfixOperator.XOR, 0xFF, 0x0F);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0xF0, result.value);
    }

    @Test
    public void testEvalShl() {
        ExprInfix infix = makeInfix(InfixOperator.SHL, 1, 4);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(16, result.value);
    }

    @Test
    public void testEvalShl2() {
        ExprInfix infix = makeInfix(InfixOperator.SHL, 1, 4);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(16, result.value);
    }

    @Test
    public void testEvalShr() {
        ExprInfix infix = makeInfix(InfixOperator.SHR, 16, 2);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(4, result.value);
    }

    @Test
    public void testEvalShr2() {
        ExprInfix infix = makeInfix(InfixOperator.SHR, 16, 2);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(4, result.value);
    }

    @Test
    public void testEvalEqual() {
        ExprInfix infix = makeInfix(InfixOperator.EQUAL, 5, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalEqualFalse() {
        ExprInfix infix = makeInfix(InfixOperator.EQUAL, 5, 6);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
    }

    @Test
    public void testEvalLt() {
        ExprInfix infix = makeInfix(InfixOperator.LT, 3, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalLtFalse() {
        ExprInfix infix = makeInfix(InfixOperator.LT, 5, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
    }

    @Test
    public void testEvalLte() {
        ExprInfix infix = makeInfix(InfixOperator.LTE, 5, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalLteFalse() {
        ExprInfix infix = makeInfix(InfixOperator.LTE, 6, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
    }

    @Test
    public void testEvalGt() {
        ExprInfix infix = makeInfix(InfixOperator.GT, 5, 3);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalGtFalse() {
        ExprInfix infix = makeInfix(InfixOperator.GT, 3, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
    }

    @Test
    public void testEvalGte() {
        ExprInfix infix = makeInfix(InfixOperator.GTE, 5, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(1, result.value);
    }

    @Test
    public void testEvalGteFalse() {
        ExprInfix infix = makeInfix(InfixOperator.GTE, 3, 5);
        Evaluated result = infix.eval(null, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
    }

    @Test
    public void testEvalWithUnresolvableChild() {
        ExprInfix infix = new ExprInfix(POS, InfixOperator.ADD);
        infix.addChild(new ExprNumber(POS, 1));
        infix.addChild(new ExprId(POS, "undefined"));
        Evaluated result = infix.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testUnknownOperationThrows() {
        new ExprInfix(POS, null);
    }

    @Test
    public void testEqualsSame() {
        ExprInfix e1 = new ExprInfix(POS, InfixOperator.ADD);
        ExprInfix e2 = new ExprInfix(POS, InfixOperator.ADD);
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferentOp() {
        ExprInfix e1 = new ExprInfix(POS, InfixOperator.ADD);
        ExprInfix e2 = new ExprInfix(POS, InfixOperator.SUBTRACT);
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprInfix e = new ExprInfix(POS, InfixOperator.ADD);
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprInfix e = new ExprInfix(POS, InfixOperator.ADD);
        assertTrue(e.toString().contains("ExprInfix"));
    }

    @Test
    public void testMkCopy() {
        ExprInfix original = new ExprInfix(POS, InfixOperator.ADD);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprInfix);
        assertEquals(original, copy);
    }
}
