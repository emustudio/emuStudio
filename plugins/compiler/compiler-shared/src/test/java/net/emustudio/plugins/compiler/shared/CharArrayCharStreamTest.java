/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.shared;

import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;
import org.junit.Test;

import static org.junit.Assert.*;

public class CharArrayCharStreamTest {

    @Test
    public void testSizeReturnsLength() {
        char[] data = "hello".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(5, stream.size());
    }

    @Test
    public void testSizeWithOffset() {
        char[] data = "hello world".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 6, 5);
        assertEquals(5, stream.size());
    }

    @Test
    public void testIndexStartsAtZero() {
        char[] data = "hello".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(0, stream.index());
    }

    @Test
    public void testConsume() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.consume();
        assertEquals(1, stream.index());
        stream.consume();
        assertEquals(2, stream.index());
    }

    @Test(expected = IllegalStateException.class)
    public void testConsumeAtEOFThrows() {
        char[] data = "a".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.consume(); // index = 1 = size
        stream.consume(); // should throw
    }

    @Test
    public void testLAForward() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals('a', stream.LA(1));
        assertEquals('b', stream.LA(2));
        assertEquals('c', stream.LA(3));
    }

    @Test
    public void testLAReturnsEOFBeyondSize() {
        char[] data = "ab".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(IntStream.EOF, stream.LA(3));
    }

    @Test
    public void testLAZeroReturnsZero() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(0, stream.LA(0));
    }

    @Test
    public void testLANegative() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.consume(); // index = 1
        assertEquals('a', stream.LA(-1));
    }

    @Test
    public void testLANegativeBeyondStart() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(IntStream.EOF, stream.LA(-1));
    }

    @Test
    public void testSeek() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.seek(2);
        assertEquals(2, stream.index());
        assertEquals('c', stream.LA(1));
    }

    @Test
    public void testGetText() {
        char[] data = "hello world".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals("hello", stream.getText(Interval.of(0, 4)));
    }

    @Test
    public void testGetTextWithOffset() {
        char[] data = "hello world".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 6, 5);
        assertEquals("world", stream.getText(Interval.of(0, 4)));
    }

    @Test
    public void testGetTextBeyondSize() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals("abc", stream.getText(Interval.of(0, 10)));
    }

    @Test
    public void testGetTextEmptyInterval() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals("", stream.getText(Interval.of(5, 3)));
    }

    @Test
    public void testMark() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(-1, stream.mark());
    }

    @Test
    public void testRelease() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.release(0); // should not throw
    }

    @Test
    public void testGetSourceName() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        assertEquals(IntStream.UNKNOWN_SOURCE_NAME, stream.getSourceName());
    }

    @Test
    public void testLAWithOffset() {
        char[] data = "hello world".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 6, 5);
        assertEquals('w', stream.LA(1));
        assertEquals('o', stream.LA(2));
    }

    @Test
    public void testLANegativeAtPositionOneReturnsFirstChar() {
        char[] data = "abc".toCharArray();
        CharArrayCharStream stream = new CharArrayCharStream(data, 0, data.length);
        stream.consume(); // index = 1
        stream.consume(); // index = 2
        assertEquals('b', stream.LA(-1));
        assertEquals('a', stream.LA(-2));
    }
}

