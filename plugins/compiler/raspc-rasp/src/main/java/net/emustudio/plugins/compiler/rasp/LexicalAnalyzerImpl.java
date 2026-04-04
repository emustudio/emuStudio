/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

import static net.emustudio.plugins.compiler.rasp.RASPParser.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    public static final int[] tokenMap = new int[ERROR + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[COMMENT2] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[OPCODE_READ] = Token.RESERVED;
        tokenMap[OPCODE_WRITE] = Token.RESERVED;
        tokenMap[OPCODE_LOAD] = Token.RESERVED;
        tokenMap[OPCODE_STORE] = Token.RESERVED;
        tokenMap[OPCODE_ADD] = Token.RESERVED;
        tokenMap[OPCODE_HALT] = Token.RESERVED;
        tokenMap[OPCODE_DIV] = Token.RESERVED;
        tokenMap[OPCODE_MUL] = Token.RESERVED;
        tokenMap[OPCODE_SUB] = Token.RESERVED;
        tokenMap[OPCODE_JMP] = Token.RESERVED;
        tokenMap[OPCODE_JGTZ] = Token.RESERVED;
        tokenMap[OPCODE_JZ] = Token.RESERVED;

        tokenMap[PREP_ORG] = Token.PREPROCESSOR;
        tokenMap[PREP_INPUT] = Token.PREPROCESSOR;

        tokenMap[OP_CONSTANT] = Token.OPERATOR;

        tokenMap[LIT_NUMBER] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_1] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_2] = Token.LITERAL;
        tokenMap[LIT_OCTNUMBER] = Token.LITERAL;
        tokenMap[LIT_BINNUMBER] = Token.LITERAL;

        tokenMap[ID_IDENTIFIER] = Token.IDENTIFIER;
        tokenMap[ID_LABEL] = Token.IDENTIFIER;

        tokenMap[ERROR] = Token.ERROR;
    }

    private final RASPLexer lexer;


    public LexicalAnalyzerImpl(RASPLexer lexer) {
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
