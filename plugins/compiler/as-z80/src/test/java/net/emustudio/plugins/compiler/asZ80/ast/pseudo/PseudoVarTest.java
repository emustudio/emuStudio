/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import org.junit.Test;

import static org.junit.Assert.*;

public class PseudoVarTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testEqualsSame() {
        PseudoVar v1 = new PseudoVar(POS, "counter");
        PseudoVar v2 = new PseudoVar(POS, "counter");
        assertEquals(v1, v2);
    }

    @Test
    public void testNotEqualsDifferent() {
        PseudoVar v1 = new PseudoVar(POS, "a");
        PseudoVar v2 = new PseudoVar(POS, "b");
        assertNotEquals(v1, v2);
    }

    @Test
    public void testNotEqualsNull() {
        assertNotEquals(new PseudoVar(POS, "a"), null);
    }

    @Test
    public void testToString() {
        assertTrue(new PseudoVar(POS, "counter").toString().contains("PseudoVar(counter)"));
    }

    @Test
    public void testCopy() {
        PseudoVar original = new PseudoVar(POS, "x");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoVar);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIdThrows() {
        new PseudoVar(POS, null);
    }
}
