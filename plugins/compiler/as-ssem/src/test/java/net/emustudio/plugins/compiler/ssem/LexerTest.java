/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
