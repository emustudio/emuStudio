/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * A zero-copy {@link CharStream} backed directly by a {@code char[]} slice.
 * <p>
 * Unlike {@link org.antlr.v4.runtime.CharStreams#fromString(String)}, this
 * implementation avoids:
 * <ul>
 *   <li>Allocating an intermediate {@code String}</li>
 *   <li>Allocating a {@code CharBuffer}</li>
 *   <li>Allocating a {@code CodePointBuffer}</li>
 *   <li>Copying character data (3× in the standard path)</li>
 * </ul>
 * <p>
 * This makes it suitable for per-line tokenization in RSyntaxTextArea where
 * {@code getTokenList()} is called for every visible line on every keystroke.
 * <p>
 * <b>Limitation:</b> Assumes all characters are in the BMP (U+0000–U+FFFF),
 * which is true for assembly language source code.
 */
final class CharArrayCharStream implements CharStream {
    private final char[] data;
    private final int start;
    private final int size;
    private int index;

    /**
     * Wrap a slice of a char array as a CharStream with zero copies.
     *
     * @param data   the backing array (NOT copied)
     * @param offset first character index in the array
     * @param length number of characters to expose
     */
    CharArrayCharStream(char[] data, int offset, int length) {
        this.data = data;
        this.start = offset;
        this.size = length;
        this.index = 0;
    }

    @Override
    public String getText(Interval interval) {
        int a = interval.a;
        int b = Math.min(interval.b, size - 1);
        if (a > b) {
            return "";
        }
        return new String(data, start + a, b - a + 1);
    }

    @Override
    public void consume() {
        if (index >= size) {
            throw new IllegalStateException("cannot consume EOF");
        }
        index++;
    }

    @Override
    public int LA(int i) {
        if (i == 0) {
            return 0; // undefined
        }
        if (i < 0) {
            i++; // e.g., LA(-1) looks at data[index-1]
            if (index + i - 1 < 0) {
                return IntStream.EOF;
            }
        }
        int pos = index + i - 1;
        if (pos < 0 || pos >= size) {
            return IntStream.EOF;
        }
        return data[start + pos];
    }

    @Override
    public int mark() {
        return -1; // no need for mark/release in per-line scanning
    }

    @Override
    public void release(int marker) {
        // no-op
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public void seek(int index) {
        this.index = index;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String getSourceName() {
        return IntStream.UNKNOWN_SOURCE_NAME;
    }
}

