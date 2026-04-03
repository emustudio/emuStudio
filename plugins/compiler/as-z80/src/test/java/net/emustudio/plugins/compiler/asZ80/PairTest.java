/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80;

import org.junit.Test;

import static org.junit.Assert.*;

public class PairTest {

    @Test
    public void testConstructor() {
        Pair<String, Integer> pair = new Pair<>("hello", 42);
        assertEquals("hello", pair.l);
        assertEquals(42, (int) pair.r);
    }

    @Test
    public void testOf() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        assertEquals("hello", pair.l);
        assertEquals(42, (int) pair.r);
    }

    @Test
    public void testNullValues() {
        Pair<String, String> pair = Pair.of(null, null);
        assertNull(pair.l);
        assertNull(pair.r);
    }

    @Test
    public void testDifferentTypes() {
        Pair<Integer, Boolean> pair = Pair.of(1, true);
        assertEquals(1, (int) pair.l);
        assertTrue(pair.r);
    }
}

