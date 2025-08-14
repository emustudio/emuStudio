/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static net.emustudio.plugins.compiler.brainduck.BraincLexer.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    public static final int[] tokenMap = new int[COMMENT + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[DEC] = Token.RESERVED;
        tokenMap[DECV] = Token.RESERVED;
        tokenMap[INC] = Token.RESERVED;
        tokenMap[INCV] = Token.RESERVED;
        tokenMap[PRINT] = Token.RESERVED;
        tokenMap[LOAD] = Token.RESERVED;
        tokenMap[LOOP] = Token.RESERVED;
        tokenMap[HALT] = Token.RESERVED;
        tokenMap[ENDL] = Token.RESERVED;
    }

    private final BraincLexer lexer;

    public LexicalAnalyzerImpl(BraincLexer lexer) {
        this.lexer = Objects.requireNonNull(lexer);
    }

    @Override
    public Token next() {
        org.antlr.v4.runtime.Token token = lexer.nextToken();
        return new Token() {
            @Override
            public int getType() {
                return convertLexerTokenType(token.getType());
            }

            @Override
            public int getOffset() {
                return token.getStartIndex();
            }

            @Override
            public String getText() {
                return token.getText();
            }
        };
    }

    @Override
    public boolean hasNext() {
        return !lexer._hitEOF;
    }

    @Override
    public void reset(InputStream inputStream) throws IOException {
        lexer.setInputStream(CharStreams.fromStream(inputStream));
    }

    @Override
    public void reset(String source) {
        lexer.setInputStream(CharStreams.fromString(source));
    }

    private int convertLexerTokenType(int tokenType) {
        if (tokenType == EOF) {
            return Token.EOF;
        }
        return tokenMap[tokenType];
    }
}
