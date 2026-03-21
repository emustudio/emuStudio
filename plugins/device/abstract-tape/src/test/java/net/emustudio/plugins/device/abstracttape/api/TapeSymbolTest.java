/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class TapeSymbolTest {

    @Test
    public void testNumberSymbol() {
        TapeSymbol symbol = new TapeSymbol(42);
        assertEquals(42, symbol.number);
        assertNull(symbol.string);
        assertEquals(TapeSymbol.Type.NUMBER, symbol.type);
    }

    @Test
    public void testStringSymbol() {
        TapeSymbol symbol = new TapeSymbol("hello");
        assertEquals("hello", symbol.string);
        assertEquals(0, symbol.number);
        assertEquals(TapeSymbol.Type.STRING, symbol.type);
    }

    @Test
    public void testNullStringDefaultsToEmpty() {
        TapeSymbol symbol = new TapeSymbol((String) null);
        assertEquals("", symbol.string);
        assertEquals(TapeSymbol.Type.STRING, symbol.type);
    }

    @Test
    public void testEmptyConstant() {
        assertEquals("", TapeSymbol.EMPTY.string);
        assertEquals(0, TapeSymbol.EMPTY.number);
        assertEquals(TapeSymbol.Type.STRING, TapeSymbol.EMPTY.type);
    }

    @Test
    public void testGuessNumber() {
        TapeSymbol symbol = TapeSymbol.guess("123");
        assertEquals(123, symbol.number);
        assertEquals(TapeSymbol.Type.NUMBER, symbol.type);
    }

    @Test
    public void testGuessNegativeNumber() {
        TapeSymbol symbol = TapeSymbol.guess("-5");
        assertEquals(-5, symbol.number);
        assertEquals(TapeSymbol.Type.NUMBER, symbol.type);
    }

    @Test
    public void testGuessHexNumber() {
        TapeSymbol symbol = TapeSymbol.guess("0xFF");
        assertEquals(255, symbol.number);
        assertEquals(TapeSymbol.Type.NUMBER, symbol.type);
    }

    @Test
    public void testGuessString() {
        TapeSymbol symbol = TapeSymbol.guess("hello");
        assertEquals("hello", symbol.string);
        assertEquals(TapeSymbol.Type.STRING, symbol.type);
    }

    @Test
    public void testGuessEmptyString() {
        TapeSymbol symbol = TapeSymbol.guess("");
        assertEquals("", symbol.string);
        assertEquals(TapeSymbol.Type.STRING, symbol.type);
    }

    @Test
    public void testFromInt() {
        TapeSymbol symbol = TapeSymbol.fromInt(99);
        assertEquals(99, symbol.number);
        assertEquals(TapeSymbol.Type.NUMBER, symbol.type);
    }

    @Test
    public void testFromString() {
        TapeSymbol symbol = TapeSymbol.fromString("world");
        assertEquals("world", symbol.string);
        assertEquals(TapeSymbol.Type.STRING, symbol.type);
    }

    @Test
    public void testToStringNumber() {
        assertEquals("42", new TapeSymbol(42).toString());
    }

    @Test
    public void testToStringForString() {
        assertEquals("hello", new TapeSymbol("hello").toString());
    }

    @Test
    public void testToStringForZero() {
        assertEquals("0", new TapeSymbol(0).toString());
    }

    @Test
    public void testEqualsAndHashCodeSameNumber() {
        TapeSymbol a = new TapeSymbol(10);
        TapeSymbol b = new TapeSymbol(10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSameString() {
        TapeSymbol a = new TapeSymbol("abc");
        TapeSymbol b = new TapeSymbol("abc");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualsDifferentNumbers() {
        assertNotEquals(new TapeSymbol(1), new TapeSymbol(2));
    }

    @Test
    public void testNotEqualsDifferentStrings() {
        assertNotEquals(new TapeSymbol("a"), new TapeSymbol("b"));
    }

    @Test
    public void testNotEqualsDifferentTypes() {
        assertNotEquals(new TapeSymbol(0), new TapeSymbol("0"));
    }

    @Test
    public void testNotEqualsNull() {
        assertNotEquals(new TapeSymbol(1), null);
    }

    @Test
    public void testEqualsSameReference() {
        TapeSymbol s = new TapeSymbol("x");
        assertEquals(s, s);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        assertNotEquals(new TapeSymbol("x"), "x");
    }
}

