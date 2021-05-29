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
package net.emustudio.plugins.compiler.ssem;

import org.junit.Test;

import static net.emustudio.plugins.compiler.ssem.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.ssem.Utils.assertTokenTypesForCaseVariations;

public class LexerTest {

    @Test
    public void testParseError() {
        assertTokenTypes("B I", SSEMLexer.ERROR, SSEMLexer.WS, SSEMLexer.ERROR, SSEMLexer.EOF);
        assertTokenTypes("BINS ha", SSEMLexer.BNUM, SSEMLexer.BWS, SSEMLexer.BERROR, SSEMLexer.BERROR, SSEMLexer.EOF);
    }

    @Test
    public void testParseReservedWords() {
        assertTokenTypesForCaseVariations("jmp", SSEMLexer.JMP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jrp", SSEMLexer.JPR, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jpr", SSEMLexer.JPR, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jmr", SSEMLexer.JPR, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("ldn", SSEMLexer.LDN, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("sto", SSEMLexer.STO, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("sub", SSEMLexer.SUB, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("cmp", SSEMLexer.CMP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("skn", SSEMLexer.CMP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("stp", SSEMLexer.STP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("hlt", SSEMLexer.STP, SSEMLexer.EOF);
    }

    @Test
    public void testParsePreprocessor() {
        assertTokenTypesForCaseVariations("start", SSEMLexer.START, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("num", SSEMLexer.NUM, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("bnum", SSEMLexer.BNUM, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("bins", SSEMLexer.BNUM, SSEMLexer.EOF);
    }

    @Test
    public void testParseWhitespaces() {
        assertTokenTypes(" ", SSEMLexer.WS, SSEMLexer.EOF);
        assertTokenTypes("\t", SSEMLexer.WS, SSEMLexer.EOF);
        assertTokenTypes("\n", SSEMLexer.EOL, SSEMLexer.EOF);
        assertTokenTypes("", SSEMLexer.EOF);
    }

    @Test
    public void testParseComments() {
        assertTokenTypes("-- comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
        assertTokenTypes("# comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
        assertTokenTypes("// comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
        assertTokenTypes("; comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
    }

    @Test
    public void testLiterals() {
        assertTokenTypes("10", SSEMLexer.NUMBER, SSEMLexer.EOF);
        assertTokenTypes("0xAF", SSEMLexer.HEXNUMBER, SSEMLexer.EOF);
        assertTokenTypes("BINS 1010", SSEMLexer.BNUM, SSEMLexer.BWS, SSEMLexer.BinaryNumber, SSEMLexer.EOF);
    }
}
