/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem;

import org.junit.Test;

import static net.emustudio.plugins.compiler.ssem.SSEMLexer.*;
import static net.emustudio.plugins.compiler.ssem.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.ssem.Utils.assertTokenTypesForCaseVariations;

public class LexerTest {

    @Test
    public void testParseError() {
        assertTokenTypes("B I", ERROR, WS, ERROR, EOF);
        assertTokenTypes("BINS ha", BNUM, BWS, BERROR, BERROR, EOF);
    }

    @Test
    public void testParseReservedWords() {
        assertTokenTypesForCaseVariations("jmp", JMP, EOF);
        assertTokenTypesForCaseVariations("jrp", JPR, EOF);
        assertTokenTypesForCaseVariations("jpr", JPR, EOF);
        assertTokenTypesForCaseVariations("jmr", JPR, EOF);
        assertTokenTypesForCaseVariations("ldn", LDN, EOF);
        assertTokenTypesForCaseVariations("sto", STO, EOF);
        assertTokenTypesForCaseVariations("sub", SUB, EOF);
        assertTokenTypesForCaseVariations("cmp", CMP, EOF);
        assertTokenTypesForCaseVariations("skn", CMP, EOF);
        assertTokenTypesForCaseVariations("stp", STP, EOF);
        assertTokenTypesForCaseVariations("hlt", STP, EOF);
    }

    @Test
    public void testParsePreprocessor() {
        assertTokenTypesForCaseVariations("start", START, EOF);
        assertTokenTypesForCaseVariations("num", NUM, EOF);
        assertTokenTypesForCaseVariations("bnum", BNUM, EOF);
        assertTokenTypesForCaseVariations("bins", BNUM, EOF);
    }

    @Test
    public void testParseWhitespaces() {
        assertTokenTypes(" ", WS, EOF);
        assertTokenTypes("\t", WS, EOF);
        assertTokenTypes("\n", EOL, EOF);
        assertTokenTypes("", EOF);
    }

    @Test
    public void testParseComments() {
        assertTokenTypes("-- comment baybe", COMMENT, EOF);
        assertTokenTypes("# comment baybe", COMMENT, EOF);
        assertTokenTypes("// comment baybe", COMMENT, EOF);
        assertTokenTypes("; comment baybe", COMMENT, EOF);
    }

    @Test
    public void testLiterals() {
        assertTokenTypes("10", NUMBER, EOF);
        assertTokenTypes("0xAF", HEXNUMBER, EOF);
        assertTokenTypes("BINS 1010", BNUM, BWS, BinaryNumber, EOF);
    }
}
