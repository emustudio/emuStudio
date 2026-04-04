/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
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
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(">>"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        assertTrue(analyzer.hasNext());
        Token token = analyzer.next();
        assertNotNull(token);
    }

    @Test
    public void testHasNextReturnsFalseAfterAllTokensConsumed() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Consume the EOF token
        while (analyzer.hasNext()) {
            analyzer.next();
        }
        assertFalse(analyzer.hasNext());
    }

    @Test
    public void testNextReturnsAllTokens() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(";><+-.,[]"));
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
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(">>"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Consume all tokens
        while (analyzer.hasNext()) {
            analyzer.next();
        }
        assertFalse(analyzer.hasNext());

        // Reset with new input
        String newInput = "<<";
        analyzer.reset(newInput.toCharArray(), 0, newInput.length());

        assertTrue(analyzer.hasNext());
        Token token = analyzer.next();
        assertNotNull(token);
    }

    @Test
    public void testResetWithOffset() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // Reset with offset into char array
        String fullInput = "XXX>>";
        analyzer.reset(fullInput.toCharArray(), 3, fullInput.length() - 3);

        assertTrue(analyzer.hasNext());
        Token token = analyzer.next();
        assertNotNull(token);
        assertEquals(Token.RESERVED, token.getType());
    }

    @Test
    public void testTokenTypesReserved() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(";><+-.,[]"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // ; (HALT)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // > (INC)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // < (DEC)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // + (INCV)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // - (DECV)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // . (PRINT)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // , (LOAD)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // [ (LOOP)
        assertEquals(Token.RESERVED, analyzer.next().getType());
        // ] (ENDL)
        assertEquals(Token.RESERVED, analyzer.next().getType());
    }

    @Test
    public void testTokenTypeComment() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString("this is a comment\n"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        Token token = analyzer.next();
        assertEquals(Token.COMMENT, token.getType());
    }

    @Test
    public void testEofToken() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(""));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        Token token = analyzer.next();
        assertEquals(Token.EOF, token.getType());
    }

    @Test
    public void testEmuTokenGetOffset() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(">>"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        Token token = analyzer.next(); // first ">"
        assertEquals(0, token.getOffset());

        Token token2 = analyzer.next(); // second ">"
        assertEquals(1, token2.getOffset());
    }

    @Test
    public void testTokenTextIsSet() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(">"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        Token token = analyzer.next();
        assertNotNull(token);
        assertTrue(token instanceof org.antlr.v4.runtime.CommonToken);
        assertEquals(">", ((org.antlr.v4.runtime.CommonToken) token).getText());
    }

    @Test
    public void testMultipleResets() {
        BraincLexer lexer = new BraincLexer(CharStreams.fromString(">"));
        LexicalAnalyzerImpl analyzer = new LexicalAnalyzerImpl(lexer);

        // First pass
        while (analyzer.hasNext()) {
            analyzer.next();
        }

        // Reset 1
        String input1 = "<";
        analyzer.reset(input1.toCharArray(), 0, input1.length());
        assertTrue(analyzer.hasNext());
        Token t1 = analyzer.next();
        assertEquals(Token.RESERVED, t1.getType());

        // Consume rest
        while (analyzer.hasNext()) {
            analyzer.next();
        }

        // Reset 2
        String input2 = "+";
        analyzer.reset(input2.toCharArray(), 0, input2.length());
        assertTrue(analyzer.hasNext());
        Token t2 = analyzer.next();
        assertEquals(Token.RESERVED, t2.getType());
    }

    @Test
    public void testEmuTokenFactoryCreateSimple() {
        LexicalAnalyzerImpl.EmuTokenFactory factory = new LexicalAnalyzerImpl.EmuTokenFactory(LexicalAnalyzerImpl.tokenMap);
        LexicalAnalyzerImpl.EmuToken token = factory.create(BraincLexer.INC, ">");
        assertNotNull(token);
        assertEquals(Token.RESERVED, token.getType());
        assertEquals(">", token.getText());
    }

    @Test
    public void testEmuTokenFactoryCreateSimpleEof() {
        LexicalAnalyzerImpl.EmuTokenFactory factory = new LexicalAnalyzerImpl.EmuTokenFactory(LexicalAnalyzerImpl.tokenMap);
        LexicalAnalyzerImpl.EmuToken token = factory.create(org.antlr.v4.runtime.Token.EOF, "");
        assertNotNull(token);
        assertEquals(Token.EOF, token.getType());
    }

    @Test
    public void testEmuTokenFactoryCreateWithTextNotNull() {
        LexicalAnalyzerImpl.EmuTokenFactory factory = new LexicalAnalyzerImpl.EmuTokenFactory(LexicalAnalyzerImpl.tokenMap);
        CharStream stream = CharStreams.fromString(">");
        BraincLexer lexer = new BraincLexer(stream);
        Pair<TokenSource, CharStream> source = new Pair<>(lexer, stream);

        // Pass explicit text (not null)
        LexicalAnalyzerImpl.EmuToken token = factory.create(source, BraincLexer.INC, "explicit-text", 0, 0, 0, 1, 0);
        assertNotNull(token);
        assertEquals("explicit-text", token.getText());
    }

    @Test
    public void testEmuTokenFactoryCreateWithNullTextAndNullSource() {
        LexicalAnalyzerImpl.EmuTokenFactory factory = new LexicalAnalyzerImpl.EmuTokenFactory(LexicalAnalyzerImpl.tokenMap);
        Pair<TokenSource, CharStream> source = new Pair<>(null, null);

        // text is null AND source.b is null => text should remain null
        LexicalAnalyzerImpl.EmuToken token = factory.create(source, BraincLexer.INC, null, 0, 0, 0, 1, 0);
        assertNotNull(token);
    }

    @Test
    public void testEmuTokenConstructorWithTypeAndText() {
        LexicalAnalyzerImpl.EmuToken token = new LexicalAnalyzerImpl.EmuToken(Token.RESERVED, "test");
        assertEquals(Token.RESERVED, token.getType());
        assertEquals("test", token.getText());
    }

    @Test
    public void testEmuTokenGetOffsetForSecondConstructor() {
        LexicalAnalyzerImpl.EmuToken token = new LexicalAnalyzerImpl.EmuToken(Token.RESERVED, "test");
        assertEquals(0, token.getOffset());
    }
}
