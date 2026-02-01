/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SchemaPointTest {

    @Test
    public void testParse() {
        assertEquals(SchemaPoint.parse("10,56"), SchemaPoint.of(10, 56));
    }

    @Test
    public void testParseWithSpaces() {
        assertEquals(SchemaPoint.parse("   30   , 5   "), SchemaPoint.of(30, 5));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testParseMissingY() {
        SchemaPoint.parse("10,");
    }

    @Test(expected = NumberFormatException.class)
    public void testMissingX() {
        SchemaPoint.parse(",30");
    }

    @Test(expected = NumberFormatException.class)
    public void parseEmpty() {
        SchemaPoint.parse("");
    }

    @Test(expected = NumberFormatException.class)
    public void parseNonsense() {
        SchemaPoint.parse("non,sense");
    }
}
