/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
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
        switch (tokenType) {
            case SSEMLexer.COMMENT:
                return Token.COMMENT;
            case SSEMLexer.EOL:
            case SSEMLexer.WS:
            case SSEMLexer.BWS:
                return Token.WHITESPACE;
            case SSEMLexer.JMP:
            case SSEMLexer.JPR:
            case SSEMLexer.LDN:
            case SSEMLexer.STO:
            case SSEMLexer.SUB:
            case SSEMLexer.CMP:
            case SSEMLexer.STP:
                return Token.RESERVED;
            case SSEMLexer.START:
                return Token.LABEL;
            case SSEMLexer.NUM:
            case SSEMLexer.BNUM:
                return Token.PREPROCESSOR;
            case SSEMLexer.NUMBER:
            case SSEMLexer.HEXNUMBER:
            case SSEMLexer.BinaryNumber:
                return Token.LITERAL;
            case SSEMLexer.EOF:
                return Token.EOF;
        }
        return Token.ERROR;
    }
}
