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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class LexerTest {

    @Test
    public void testParseError() {
        assertTokenTypes("B I", SSEMLexer.ERROR, SSEMLexer.WS, SSEMLexer.ERROR, SSEMLexer.EOF);
        assertTokenTypes("BINS ha", SSEMLexer.BNUM, SSEMLexer.BWS, SSEMLexer.BERROR, SSEMLexer.BERROR, SSEMLexer.EOF);
    }

    @Test
    public void testParseReservedWords() {
        assertTokenTypesForCaseVariations("jmp", SSEMLexer.JMP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jrp", SSEMLexer.JRP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jpr", SSEMLexer.JPR, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("jmr", SSEMLexer.JMR, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("ldn", SSEMLexer.LDN, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("sto", SSEMLexer.STO, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("sub", SSEMLexer.SUB, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("cmp", SSEMLexer.CMP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("skn", SSEMLexer.SKN, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("stp", SSEMLexer.STP, SSEMLexer.EOF);
        assertTokenTypesForCaseVariations("hlt", SSEMLexer.HLT, SSEMLexer.EOF);
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

    private List<Token> getTokens(String variation) {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(variation));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        stream.fill();
        return stream.getTokens();
    }

    private void assertTokenTypes(String variation, int... expectedTypes) {
        List<Token> tokens = getTokens(variation);
        assertTokenTypes(tokens, expectedTypes);
    }

    private void assertTokenTypes(List<Token> tokens, int... expectedTypes) {
        assertEquals(expectedTypes.length, tokens.size());
        for (int i = 0; i < expectedTypes.length; i++) {
            Token token = tokens.get(i);
            assertEquals(expectedTypes[i], token.getType());
        }
    }

    private void assertTokenTypesForCaseVariations(String base, int... expectedTypes) {
        Random r = new Random();
        List<String> variations = new ArrayList<>();
        variations.add(base);
        variations.add(base.toLowerCase());
        variations.add(base.toUpperCase());
        for (int i = 0; i < 5; i++) {
            byte[] chars = base.getBytes();
            for (int j = 0; j < base.length(); j++) {
                if (r.nextBoolean()) {
                    chars[j] = Character.valueOf((char)chars[j]).toString().toUpperCase().getBytes()[0];
                } else {
                    chars[j] = Character.valueOf((char)chars[j]).toString().toLowerCase().getBytes()[0];
                }
            }
            variations.add(new String(chars));
        }
        for (String variation : variations) {
            assertTokenTypes(variation, expectedTypes);
        }
    }
}
