/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import java.util.Optional;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class ExprInfixTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    private ExprInfix makeInfix(int op, int left, int right) {
        ExprInfix infix = new ExprInfix(POS, op);
        infix.addChild(new ExprNumber(POS, left));
        infix.addChild(new ExprNumber(POS, right));
        return infix;
    }

    @Test
    public void testEvalAdd() {
        ExprInfix infix = makeInfix(OP_ADD, 3, 4);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(7, result.get().value);
    }

    @Test
    public void testEvalSubtract() {
        ExprInfix infix = makeInfix(OP_SUBTRACT, 10, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(7, result.get().value);
    }

    @Test
    public void testEvalMultiply() {
        ExprInfix infix = makeInfix(OP_MULTIPLY, 3, 4);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(12, result.get().value);
    }

    @Test
    public void testEvalDivide() {
        ExprInfix infix = makeInfix(OP_DIVIDE, 10, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().value);
    }

    @Test
    public void testEvalMod() {
        ExprInfix infix = makeInfix(OP_MOD, 10, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalMod2() {
        ExprInfix infix = makeInfix(OP_MOD_2, 10, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalAnd() {
        ExprInfix infix = makeInfix(OP_AND, 0xFF, 0x0F);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0x0F, result.get().value);
    }

    @Test
    public void testEvalOr() {
        ExprInfix infix = makeInfix(OP_OR, 0xF0, 0x0F);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0xFF, result.get().value);
    }

    @Test
    public void testEvalXor() {
        ExprInfix infix = makeInfix(OP_XOR, 0xFF, 0x0F);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0xF0, result.get().value);
    }

    @Test
    public void testEvalShl() {
        ExprInfix infix = makeInfix(OP_SHL, 1, 4);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(16, result.get().value);
    }

    @Test
    public void testEvalShl2() {
        ExprInfix infix = makeInfix(OP_SHL_2, 1, 4);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(16, result.get().value);
    }

    @Test
    public void testEvalShr() {
        ExprInfix infix = makeInfix(OP_SHR, 16, 2);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(4, result.get().value);
    }

    @Test
    public void testEvalShr2() {
        ExprInfix infix = makeInfix(OP_SHR_2, 16, 2);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(4, result.get().value);
    }

    @Test
    public void testEvalEqual() {
        ExprInfix infix = makeInfix(OP_EQUAL, 5, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalEqualFalse() {
        ExprInfix infix = makeInfix(OP_EQUAL, 5, 6);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalLt() {
        ExprInfix infix = makeInfix(OP_LT, 3, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalLtFalse() {
        ExprInfix infix = makeInfix(OP_LT, 5, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalLte() {
        ExprInfix infix = makeInfix(OP_LTE, 5, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalLteFalse() {
        ExprInfix infix = makeInfix(OP_LTE, 6, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalGt() {
        ExprInfix infix = makeInfix(OP_GT, 5, 3);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalGtFalse() {
        ExprInfix infix = makeInfix(OP_GT, 3, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalGte() {
        ExprInfix infix = makeInfix(OP_GTE, 5, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().value);
    }

    @Test
    public void testEvalGteFalse() {
        ExprInfix infix = makeInfix(OP_GTE, 3, 5);
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
    }

    @Test
    public void testEvalWithUnresolvableChild() {
        ExprInfix infix = new ExprInfix(POS, OP_ADD);
        infix.addChild(new ExprNumber(POS, 1));
        infix.addChild(new ExprId(POS, "undefined"));
        Optional<Evaluated> result = infix.eval(Optional.empty(), new NameSpace());
        assertFalse(result.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void testUnknownOperationThrows() {
        new ExprInfix(POS, 99999);
    }

    @Test
    public void testEqualsSame() {
        ExprInfix e1 = new ExprInfix(POS, OP_ADD);
        ExprInfix e2 = new ExprInfix(POS, OP_ADD);
        assertEquals(e1, e2);
    }

    @Test
    public void testNotEqualsDifferentOp() {
        ExprInfix e1 = new ExprInfix(POS, OP_ADD);
        ExprInfix e2 = new ExprInfix(POS, OP_SUBTRACT);
        assertNotEquals(e1, e2);
    }

    @Test
    public void testNotEqualsNull() {
        ExprInfix e = new ExprInfix(POS, OP_ADD);
        assertNotEquals(e, null);
    }

    @Test
    public void testToStringShallow() {
        ExprInfix e = new ExprInfix(POS, OP_ADD);
        assertTrue(e.toString().contains("ExprInfix"));
    }

    @Test
    public void testMkCopy() {
        ExprInfix original = new ExprInfix(POS, OP_ADD);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprInfix);
        assertEquals(original, copy);
    }
}

