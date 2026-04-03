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

public class ExprCurrentAddressTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEvalWithAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Optional<Evaluated> result = expr.eval(Optional.of(100), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(100, result.get().value);
        assertTrue(result.get().isAddress);
    }

    @Test
    public void testEvalWithoutAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Optional<Evaluated> result = expr.eval(Optional.empty(), new NameSpace());
        assertFalse(result.isPresent());
    }

    @Test
    public void testEvalWithZeroAddress() {
        ExprCurrentAddress expr = new ExprCurrentAddress(POS);
        Optional<Evaluated> result = expr.eval(Optional.of(0), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(0, result.get().value);
        assertTrue(result.get().isAddress);
    }

    @Test
    public void testMkCopy() {
        ExprCurrentAddress original = new ExprCurrentAddress(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof ExprCurrentAddress);
    }
}

