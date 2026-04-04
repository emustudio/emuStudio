/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.junit.Test;

import static org.junit.Assert.*;

public class EvaluatedTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testConstructorWithAddress() {
        Evaluated eval = new Evaluated(POS, 42, true);
        assertEquals(42, eval.value);
        assertTrue(eval.isAddress);
    }

    @Test
    public void testConstructorWithoutAddress() {
        Evaluated eval = new Evaluated(POS, 42);
        assertEquals(42, eval.value);
        assertFalse(eval.isAddress);
    }

    @Test
    public void testMkCopy() {
        Evaluated original = new Evaluated(POS, 42, true);
        Node copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertTrue(copy instanceof Evaluated);
        Evaluated evalCopy = (Evaluated) copy;
        assertEquals(42, evalCopy.value);
        assertTrue(evalCopy.isAddress);
    }

    @Test
    public void testEqualsReflexive() {
        Evaluated eval = new Evaluated(POS, 42);
        assertEquals(eval, eval);
    }

    @Test
    public void testEqualsSameValues() {
        Evaluated eval1 = new Evaluated(POS, 42);
        Evaluated eval2 = new Evaluated(POS, 42);
        assertEquals(eval1, eval2);
    }

    @Test
    public void testNotEqualsDifferentValue() {
        Evaluated eval1 = new Evaluated(POS, 42);
        Evaluated eval2 = new Evaluated(POS, 43);
        assertNotEquals(eval1, eval2);
    }

    @Test
    public void testNotEqualsNull() {
        Evaluated eval = new Evaluated(POS, 42);
        assertNotEquals(eval, null);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        Evaluated eval = new Evaluated(POS, 42);
        assertNotEquals(eval, "string");
    }

    @Test
    public void testToStringShallow() {
        Evaluated eval = new Evaluated(POS, 42);
        String result = eval.toString();
        assertTrue(result.contains("Evaluated(42)"));
    }
}

