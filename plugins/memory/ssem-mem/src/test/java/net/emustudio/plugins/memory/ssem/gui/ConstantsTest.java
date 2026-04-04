/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConstantsTest {

    @Test
    public void testCharWidth() {
        assertEquals(17, Constants.CHAR_WIDTH);
    }

    @Test
    public void testTwoCharsIsTwiceCharWidth() {
        assertEquals(2 * Constants.CHAR_WIDTH, Constants.TWO_CHARS);
    }

    @Test
    public void testColumnWidthHas35Elements() {
        // 32 bit columns + Hex + Dec + Char
        assertEquals(35, Constants.COLUMN_WIDTH.length);
    }

    @Test
    public void testBitColumnWidths() {
        for (int i = 0; i < 32; i++) {
            assertEquals(Constants.TWO_CHARS, Constants.COLUMN_WIDTH[i]);
        }
    }

    @Test
    public void testHexColumnWidth() {
        assertEquals(10 * Constants.CHAR_WIDTH, Constants.COLUMN_WIDTH[32]);
    }

    @Test
    public void testDecColumnWidth() {
        assertEquals(10 * Constants.CHAR_WIDTH, Constants.COLUMN_WIDTH[33]);
    }

    @Test
    public void testCharColumnWidth() {
        assertEquals(5 * Constants.CHAR_WIDTH, Constants.COLUMN_WIDTH[34]);
    }
}

