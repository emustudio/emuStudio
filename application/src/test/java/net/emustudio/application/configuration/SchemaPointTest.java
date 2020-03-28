/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.emustudio.application.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SchemaPointTest {

    @Test
    public void testParse() {
        assertEquals(SchemaPoint.parse("10,56"), SchemaPoint.of(10,56));
    }

    @Test
    public void testParseWithSpaces() {
        assertEquals(SchemaPoint.parse("   30   , 5   "), SchemaPoint.of(30,5));
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
