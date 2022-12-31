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

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
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
    }

    @Override
    public Token nextToken() {
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
    public boolean isAtEOF() {
        return lexer._hitEOF;
    }

    @Override
    public void reset(InputStream inputStream) throws IOException {
        lexer.setInputStream(CharStreams.fromStream(inputStream));
    }

    private int convertLexerTokenType(int tokenType) {
        if (tokenType == EOF) {
            return Token.EOF;
        }
        return tokenMap[tokenType];
    }
}
