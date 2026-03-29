/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.Segment;
import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

public class RTokenMakerTest {

    // --- getTokenList: lexer consumption ---

    @Test
    public void testConsumesLexerViaNextHasNextNotIterator() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "LABEL"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 5, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        maker.getTokenList(segment("LABEL"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        assertFalse("Should use next()/hasNext(), not iterator()", lexer.iteratorUsed);
        assertEquals(2, lexer.nextCalls);
    }

    @Test
    public void testResetsLexerWithSegmentText() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 0, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        maker.getTokenList(segment("MOV A,B"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        assertEquals("MOV A,B", lexer.lastResetInput);
    }

    // --- getTokenList: null termination ---

    @Test
    public void testTokenListAlwaysEndsWithNullToken() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "X"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 1, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(segment("X"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        org.fife.ui.rsyntaxtextarea.Token last = lastToken(head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, last.getType());
    }

    @Test
    public void testEmptyInputProducesOnlyNullToken() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 0, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(segment(""), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        assertNotNull(head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, head.getType());
    }

    @Test
    public void testNoTokensFromLexerStillProducesNullToken() {
        // Lexer returns nothing (no EOF even) — e.g. empty line with a broken lexer
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(/* no tokens */);
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(segment(""), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        assertNotNull("Must return a token list even with no lexer output", head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, head.getType());
    }

    // --- getTokenList: single token + type mapping ---

    @Test
    public void testReservedMapsToReservedWord() {
        assertSingleTokenType("MOV", net.emustudio.emulib.plugins.compiler.Token.RESERVED,
                org.fife.ui.rsyntaxtextarea.Token.RESERVED_WORD);
    }

    @Test
    public void testPreprocessorMapsToPreprocessor() {
        assertSingleTokenType("ORG", net.emustudio.emulib.plugins.compiler.Token.PREPROCESSOR,
                org.fife.ui.rsyntaxtextarea.Token.PREPROCESSOR);
    }

    @Test
    public void testRegisterMapsToReservedWord2() {
        assertSingleTokenType("A", net.emustudio.emulib.plugins.compiler.Token.REGISTER,
                org.fife.ui.rsyntaxtextarea.Token.RESERVED_WORD_2);
    }

    @Test
    public void testSeparatorMapsToSeparator() {
        assertSingleTokenType(",", net.emustudio.emulib.plugins.compiler.Token.SEPARATOR,
                org.fife.ui.rsyntaxtextarea.Token.SEPARATOR);
    }

    @Test
    public void testOperatorMapsToOperator() {
        assertSingleTokenType("+", net.emustudio.emulib.plugins.compiler.Token.OPERATOR,
                org.fife.ui.rsyntaxtextarea.Token.OPERATOR);
    }

    @Test
    public void testCommentMapsToCommentMarkup() {
        assertSingleTokenType("; comment", net.emustudio.emulib.plugins.compiler.Token.COMMENT,
                org.fife.ui.rsyntaxtextarea.Token.COMMENT_MARKUP);
    }

    @Test
    public void testLiteralMapsToLiteralNumberDecimalInt() {
        assertSingleTokenType("42", net.emustudio.emulib.plugins.compiler.Token.LITERAL,
                org.fife.ui.rsyntaxtextarea.Token.LITERAL_NUMBER_DECIMAL_INT);
    }

    @Test
    public void testIdentifierMapsToIdentifier() {
        assertSingleTokenType("myVar", net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER,
                org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER);
    }

    @Test
    public void testLabelMapsToAnnotation() {
        assertSingleTokenType("loop:", net.emustudio.emulib.plugins.compiler.Token.LABEL,
                org.fife.ui.rsyntaxtextarea.Token.ANNOTATION);
    }

    @Test
    public void testErrorMapsToErrorIdentifier() {
        assertSingleTokenType("@", net.emustudio.emulib.plugins.compiler.Token.ERROR,
                org.fife.ui.rsyntaxtextarea.Token.ERROR_IDENTIFIER);
    }

    @Test
    public void testWhitespaceMapsToWhitespace() {
        assertSingleTokenType(" ", net.emustudio.emulib.plugins.compiler.Token.WHITESPACE,
                org.fife.ui.rsyntaxtextarea.Token.WHITESPACE);
    }

    @Test
    public void testUnknownTokenTypeFallsBackToWhitespace() {
        // Use a type that doesn't match any case in the switch (e.g. 0xFFFF)
        assertSingleTokenType("?", 0xFFFF, org.fife.ui.rsyntaxtextarea.Token.WHITESPACE);
    }

    // --- getTokenList: multiple tokens ---

    @Test
    public void testMultipleTokensFormLinkedList() {
        // "MOV A"
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.RESERVED, 0, "MOV"),
                token(net.emustudio.emulib.plugins.compiler.Token.WHITESPACE, 3, " "),
                token(net.emustudio.emulib.plugins.compiler.Token.REGISTER, 4, "A"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 5, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(segment("MOV A"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // 3 real tokens + 1 null terminator
        assertEquals(4, tokens.size());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.RESERVED_WORD, tokens.get(0).getType());
        assertEquals("MOV", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.WHITESPACE, tokens.get(1).getType());
        assertEquals(" ", tokens.get(1).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.RESERVED_WORD_2, tokens.get(2).getType());
        assertEquals("A", tokens.get(2).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, tokens.get(3).getType());
    }

    @Test
    public void testTokenOffsetsAreCorrect() {
        // "LD BC" — offsets: LD=0, space=2, BC=3
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.RESERVED, 0, "LD"),
                token(net.emustudio.emulib.plugins.compiler.Token.WHITESPACE, 2, " "),
                token(net.emustudio.emulib.plugins.compiler.Token.REGISTER, 3, "BC"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 5, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(segment("LD BC"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0);

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        assertEquals(0, tokens.get(0).getOffset());
        assertEquals(2, tokens.get(0).length());
        assertEquals(2, tokens.get(1).getOffset());
        assertEquals(1, tokens.get(1).length());
        assertEquals(3, tokens.get(2).getOffset());
        assertEquals(2, tokens.get(2).length());
    }

    @Test
    public void testStartOffsetIsAddedToTokenOffsets() {
        // Line starts at document offset 100
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "ABC"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 3, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("ABC"), org.fife.ui.rsyntaxtextarea.Token.NULL, 100
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        assertEquals(100, tokens.get(0).getOffset());
    }

    @Test
    public void testNonZeroSegmentOffsetShiftsArrayIndices() {
        // Simulate Segment with offset > 0 (e.g. from a document with shared char[])
        char[] array = "xxABC".toCharArray();
        Segment seg = new Segment(array, 2, 3); // text = "ABC" starting at index 2

        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "ABC"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 3, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(seg, org.fife.ui.rsyntaxtextarea.Token.NULL, 50);

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // First real token
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(0).getType());
        assertEquals("ABC", tokens.get(0).getLexeme());
        assertEquals(50, tokens.get(0).getOffset());
    }

    // --- getTokenList: gap detection ---

    @Test
    public void testGapAtBeginningProducesErrorToken() {
        // Lexer reports first token starting at offset 2, but text starts at 0 → gap [0,1]
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 2, "ABC"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 5, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("??ABC"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // Should be: ERROR_CHAR for gap, IDENTIFIER for "ABC", NULL
        assertTrue("Expected at least 3 tokens (error, identifier, null)", tokens.size() >= 3);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.ERROR_CHAR, tokens.get(0).getType());
        assertEquals("??", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(1).getType());
        assertEquals("ABC", tokens.get(1).getLexeme());
    }

    @Test
    public void testGapInMiddleProducesErrorToken() {
        // "AB__CD" — two tokens with a gap of 2 characters between them
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "AB"),
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 4, "CD"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 6, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("AB__CD"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // Should be: IDENTIFIER("AB"), ERROR_CHAR("__"), IDENTIFIER("CD"), NULL
        assertTrue("Expected at least 4 tokens", tokens.size() >= 4);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(0).getType());
        assertEquals("AB", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.ERROR_CHAR, tokens.get(1).getType());
        assertEquals("__", tokens.get(1).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(2).getType());
        assertEquals("CD", tokens.get(2).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, tokens.get(3).getType());
    }

    @Test
    public void testContiguousTokensProduceNoErrorGaps() {
        // "AB" at 0, "CD" at 2 — no gap
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "AB"),
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 2, "CD"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 4, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("ABCD"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // Should be: IDENTIFIER("AB"), IDENTIFIER("CD"), NULL — no ERROR_CHAR
        assertEquals(3, tokens.size());
        for (org.fife.ui.rsyntaxtextarea.Token t : tokens) {
            assertNotEquals("No error gap expected", org.fife.ui.rsyntaxtextarea.Token.ERROR_CHAR, t.getType());
        }
    }

    // --- getTokenList: exception handling ---

    @Test
    public void testLexerNextExceptionBreaksLoopAndProducesNullTerminator() {
        // A lexer where next() always throws — must NOT cause an infinite loop
        int[] nextCalls = {0};
        LexicalAnalyzer throwingLexer = new LexicalAnalyzer() {
            @Override
            public net.emustudio.emulib.plugins.compiler.Token next() {
                nextCalls[0]++;
                throw new RuntimeException("Lexer error");
            }

            @Override
            public boolean hasNext() {
                return true; // would cause infinite loop if next() exception didn't break
            }

            @Override
            public void reset(InputStream input) {
            }

            @Override
            public void reset(String input) {
                nextCalls[0] = 0;
            }

            @Override
            public void reset(char[] array, int offset, int length) {
                nextCalls[0] = 0;
            }

            @Override
            public Iterator<net.emustudio.emulib.plugins.compiler.Token> iterator() {
                throw new AssertionError();
            }
        };

        RTokenMaker maker = new RTokenMaker(new CompilerStub(throwingLexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("X"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        assertNotNull("Token list must not be null even after exception", head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, lastToken(head).getType());
        assertEquals("Loop must break after first exception in next()", 1, nextCalls[0]);
    }

    @Test
    public void testLexerResetExceptionReturnsNullTokenOnly() {
        // A lexer where reset() throws — simulates corrupted lexer state after compilation
        LexicalAnalyzer resetThrowingLexer = new LexicalAnalyzer() {
            @Override
            public net.emustudio.emulib.plugins.compiler.Token next() {
                throw new AssertionError("next() should not be called if reset() throws");
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public void reset(InputStream input) {
            }

            @Override
            public void reset(String input) {
                throw new RuntimeException("Reset failed");
            }

            @Override
            public void reset(char[] array, int offset, int length) {
                throw new RuntimeException("Reset failed");
            }

            @Override
            public Iterator<net.emustudio.emulib.plugins.compiler.Token> iterator() {
                throw new AssertionError();
            }
        };

        RTokenMaker maker = new RTokenMaker(new CompilerStub(resetThrowingLexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("X"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        assertNotNull("Must return valid token list even if reset() throws", head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, head.getType());
    }

    @Test
    public void testTokenProcessingExceptionStillProducesNullTerminator() {
        // A token whose getType() throws during processing — should be skipped
        net.emustudio.emulib.plugins.compiler.Token badToken = new net.emustudio.emulib.plugins.compiler.Token() {
            @Override
            public int getType() {
                throw new RuntimeException("bad token");
            }

            @Override
            public int getOffset() {
                return 0;
            }

            @Override
            public String getText() {
                return "X";
            }
        };
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                badToken,
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 1, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("X"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        assertNotNull(head);
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, lastToken(head).getType());
    }

    // --- getTokenList: edge cases ---

    @Test
    public void testEmptyTokenTextIsSkipped() {
        // A token with empty text should be skipped to prevent end < start corruption
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, ""),
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "OK"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 2, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("OK"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        // Empty token skipped, "OK" present, null terminator
        assertEquals(2, tokens.size());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(0).getType());
        assertEquals("OK", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, tokens.get(1).getType());
    }

    @Test
    public void testNullTokenTextIsSkipped() {
        // A token with null text should be skipped
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, null),
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "OK"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 2, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("OK"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        assertEquals(2, tokens.size());
        assertEquals("OK", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, tokens.get(1).getType());
    }

    // --- getTokenList: repeated calls ---

    @Test
    public void testRepeatedCallsResetState() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "A"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 1, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        // First call
        org.fife.ui.rsyntaxtextarea.Token first = maker.getTokenList(
                segment("A"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );
        List<org.fife.ui.rsyntaxtextarea.Token> firstTokens = collectTokens(first);

        // Second call — should produce same fresh result
        org.fife.ui.rsyntaxtextarea.Token second = maker.getTokenList(
                segment("A"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );
        List<org.fife.ui.rsyntaxtextarea.Token> secondTokens = collectTokens(second);

        assertEquals(firstTokens.size(), secondTokens.size());
        for (int i = 0; i < firstTokens.size(); i++) {
            assertEquals(firstTokens.get(i).getType(), secondTokens.get(i).getType());
        }
    }

    // --- getTokenList: EOF does not appear as a real token ---

    @Test
    public void testEofTokenIsNotIncludedAsRealToken() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "X"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 1, "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment("X"), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        // Should have: IDENTIFIER("X"), NULL — exactly 2 tokens
        List<org.fife.ui.rsyntaxtextarea.Token> tokens = collectTokens(head);
        assertEquals(2, tokens.size());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, tokens.get(0).getType());
        assertEquals("X", tokens.get(0).getLexeme());
        assertEquals(org.fife.ui.rsyntaxtextarea.Token.NULL, tokens.get(1).getType());
    }

    // --- getWordsToHighlight ---

    @Test
    public void testGetWordsToHighlightReturnsEmptyMap() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer();
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        // TokenMap doesn't expose a size method, but get() for any word should return -1 (no mapping)
        assertNotNull(maker.getWordsToHighlight());
        assertEquals(-1, maker.getWordsToHighlight().get("MOV".toCharArray(), 0, 2));
    }

    // ---- Helpers ----

    private static Segment segment(String text) {
        return new Segment(text.toCharArray(), 0, text.length());
    }

    private static net.emustudio.emulib.plugins.compiler.Token token(int type, int offset, String text) {
        return new net.emustudio.emulib.plugins.compiler.Token() {
            @Override
            public int getType() {
                return type;
            }

            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }

    /**
     * Asserts that a single-token input produces the expected RSyntaxTextArea token type.
     */
    private void assertSingleTokenType(String text, int emuStudioType, int expectedRstaType) {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(emuStudioType, 0, text),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, text.length(), "")
        );
        RTokenMaker maker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token head = maker.getTokenList(
                segment(text), org.fife.ui.rsyntaxtextarea.Token.NULL, 0
        );

        assertNotNull(head);
        assertEquals("Token type mismatch for '" + text + "'", expectedRstaType, head.getType());
        assertEquals(text, head.getLexeme());
    }

    /**
     * Collects all tokens (including the null terminator) into a list.
     */
    private static List<org.fife.ui.rsyntaxtextarea.Token> collectTokens(org.fife.ui.rsyntaxtextarea.Token head) {
        List<org.fife.ui.rsyntaxtextarea.Token> list = new ArrayList<>();
        org.fife.ui.rsyntaxtextarea.Token current = head;
        int safety = 1000;
        while (current != null && safety-- > 0) {
            list.add(current);
            if (current.getType() == org.fife.ui.rsyntaxtextarea.Token.NULL) {
                break;
            }
            current = current.getNextToken();
        }
        return list;
    }

    /**
     * Returns the last token in the linked list.
     */
    private static org.fife.ui.rsyntaxtextarea.Token lastToken(org.fife.ui.rsyntaxtextarea.Token head) {
        org.fife.ui.rsyntaxtextarea.Token current = head;
        int safety = 1000;
        while (current.getNextToken() != null && safety-- > 0) {
            if (current.getType() == org.fife.ui.rsyntaxtextarea.Token.NULL) {
                break;
            }
            current = current.getNextToken();
        }
        return current;
    }

}
