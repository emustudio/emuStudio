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
