/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprCurrentAddressTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalWithAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Evaluated result = expr.eval(100, new NameSpace());
        assertNotNull(result);
        assertEquals(100, result.value);
        assertTrue(result.isAddress);
    }

    @Test
    public void testEvalWithoutAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Evaluated result = expr.eval(null, new NameSpace());
        assertNull(result);
    }

    @Test
    public void testEvalWithZeroAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Evaluated result = expr.eval(0, new NameSpace());
        assertNotNull(result);
        assertEquals(0, result.value);
        assertTrue(result.isAddress);
    }

    @Test
    public void testMkCopy() {
        ExprCurrentAddress original = new ExprCurrentAddress(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprCurrentAddress);
    }
}
