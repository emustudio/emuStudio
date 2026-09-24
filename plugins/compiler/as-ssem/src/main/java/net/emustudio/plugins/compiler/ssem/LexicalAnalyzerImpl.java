/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.antlr.CharArrayCharStream;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

import static net.emustudio.plugins.compiler.ssem.SSEMLexer.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    public static final int[] tokenMap = new int[SSEMLexer.BERROR + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[BWS] = Token.WHITESPACE;
        tokenMap[JMP] = Token.RESERVED;
        tokenMap[JPR] = Token.RESERVED;
        tokenMap[LDN] = Token.RESERVED;
        tokenMap[STO] = Token.RESERVED;
        tokenMap[SUB] = Token.RESERVED;
        tokenMap[CMP] = Token.RESERVED;
        tokenMap[STP] = Token.RESERVED;
        tokenMap[START] = Token.LABEL;
        tokenMap[NUM] = Token.PREPROCESSOR;
        tokenMap[BNUM] = Token.PREPROCESSOR;
        tokenMap[NUMBER] = Token.LITERAL;
        tokenMap[HEXNUMBER] = Token.LITERAL;
        tokenMap[BinaryNumber] = Token.LITERAL;
        tokenMap[ERROR] = Token.ERROR;
        tokenMap[BERROR] = Token.ERROR;
    }

    private final SSEMLexer lexer;

    public LexicalAnalyzerImpl(SSEMLexer lexer) {
        this.lexer = Objects.requireNonNull(lexer);
        this.lexer.setTokenFactory(new EmuTokenFactory(tokenMap));
    }

    @Override
    public Token next() {
        return (Token) lexer.nextToken();
    }

    @Override
    public boolean hasNext() {
        return !lexer._hitEOF;
    }

    @Override
    public void reset(char[] array, int offset, int length) {
        lexer.setInputStream(new CharArrayCharStream(array, offset, length));
    }

    static class EmuToken extends CommonToken implements Token {
        EmuToken(Pair<TokenSource, CharStream> source, int type, int channel, int start, int stop) {
            super(source, type, channel, start, stop);
        }

        EmuToken(int type, String text) {
            super(type, text);
        }

        @Override
        public int getOffset() {
            return getStartIndex();
        }
    }

    static class EmuTokenFactory implements TokenFactory<EmuToken> {
        private final int[] tokenMap;

        EmuTokenFactory(int[] tokenMap) {
            this.tokenMap = tokenMap;
        }

        private int convertType(int antlrType) {
            if (antlrType == org.antlr.v4.runtime.Token.EOF) {
                return Token.EOF;
            }
            return tokenMap[antlrType];
        }

        @Override
        public EmuToken create(
                Pair<TokenSource, CharStream> source, int type, String text,
                int channel, int start, int stop, int line, int charPositionInLine) {
            EmuToken t = new EmuToken(source, convertType(type), channel, start, stop);
            t.setLine(line);
            t.setCharPositionInLine(charPositionInLine);
            if (text != null) {
                t.setText(text);
            } else if (source.b != null) {
                t.setText(source.b.getText(Interval.of(start, stop)));
            }
            return t;
        }

        @Override
        public EmuToken create(int type, String text) {
            return new EmuToken(convertType(type), text);
        }
    }
}
