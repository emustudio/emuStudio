/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;

import static org.junit.Assert.*;

public class LexicalAnalyzerImplTest {

    @Test
    public void testAllTokensArePresent() {
        for (int i = 1; i < LexicalAnalyzerImpl.tokenMap.length; i++) {
            int token = LexicalAnalyzerImpl.tokenMap[i];
            assertTrue("Token " + i + " is missing", token != 0);
        }
    }

    @Test
    public void testNextReturnsToken() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString("01 LDN 5"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        assertTrue(analyzer.hasNext());
        Token token = analyzer.next();
        assertNotNull(token);
    }

    @Test
    public void testHasNextReturnsFalseAfterAllTokensConsumed() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Consume the EOF token
        while (analyzer.hasNext()) {
            analyzer.next();
        }
        assertFalse(analyzer.hasNext());
    }

    @Test
    public void testNextReturnsAllTokens() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString("01 STP"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        int count = 0;
        while (analyzer.hasNext()) {
            Token token = analyzer.next();
            assertNotNull(token);
            count++;
        }
        assertTrue(count > 0);
    }

    @Test
    public void testReset() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString("01 STP"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Consume all tokens
        while (analyzer.hasNext()) {
            analyzer.next();
        }
        assertFalse(analyzer.hasNext());

        // Reset with new input
        String newInput = "02 LDN 5";
        analyzer.reset(newInput.toCharArray(), 0, newInput.length());

        assertTrue(analyzer.hasNext());
        Token token = analyzer.next();
        assertNotNull(token);
    }

    @Test
    public void testResetWithOffset() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Reset with offset into char array
        String fullInput = "XXX01 STP";
        analyzer.reset(fullInput.toCharArray(), 3, fullInput.length() - 3);

        assertTrue(analyzer.hasNext());
    }

    @Test
    public void testTokenTypes() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString("01 LDN 5 -- comment"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Collect all tokens
        Token numberToken = analyzer.next(); // 01
        Token wsToken = analyzer.next();     // space
        Token instrToken = analyzer.next();  // LDN
        analyzer.next();                     // space
        Token operandToken = analyzer.next(); // 5
        analyzer.next();                      // space
        Token commentToken = analyzer.next(); // -- comment

        assertEquals(Token.LITERAL, numberToken.getType());
        assertEquals(Token.WHITESPACE, wsToken.getType());
        assertEquals(Token.RESERVED, instrToken.getType());
        assertEquals(Token.LITERAL, operandToken.getType());
        assertEquals(Token.COMMENT, commentToken.getType());
    }

    @Test
    public void testEmuTokenGetOffset() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString("01 LDN 5"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        Token token = analyzer.next(); // "01"
        assertEquals(0, token.getOffset());
    }

    @Test
    public void testEofToken() {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // The only token should be EOF
        Token token = analyzer.next();
        assertEquals(Token.EOF, token.getType());
    }
}
