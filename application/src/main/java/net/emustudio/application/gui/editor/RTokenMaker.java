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
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;
import java.util.Objects;

public class RTokenMaker extends AbstractTokenMaker {
    private final Compiler compiler;

    public RTokenMaker(Compiler compiler) {
        this.compiler = Objects.requireNonNull(compiler);
    }

    private static int getTokenMakerType(int emuStudioTokenType) {
        switch (emuStudioTokenType) {
            case net.emustudio.emulib.plugins.compiler.Token.RESERVED:
                return Token.RESERVED_WORD;
            case net.emustudio.emulib.plugins.compiler.Token.PREPROCESSOR:
                return Token.PREPROCESSOR;
            case net.emustudio.emulib.plugins.compiler.Token.REGISTER:
                return Token.RESERVED_WORD_2;
            case net.emustudio.emulib.plugins.compiler.Token.SEPARATOR:
                return Token.SEPARATOR;
            case net.emustudio.emulib.plugins.compiler.Token.OPERATOR:
                return Token.OPERATOR;
            case net.emustudio.emulib.plugins.compiler.Token.COMMENT:
                return Token.COMMENT_MARKUP;
            case net.emustudio.emulib.plugins.compiler.Token.LITERAL:
                return Token.LITERAL_NUMBER_DECIMAL_INT;
            case net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER:
                return Token.IDENTIFIER;
            case net.emustudio.emulib.plugins.compiler.Token.LABEL:
                return Token.ANNOTATION;
            case net.emustudio.emulib.plugins.compiler.Token.ERROR:
                return Token.ERROR_IDENTIFIER;
            case net.emustudio.emulib.plugins.compiler.Token.EOF:
                return Token.NULL;
        }
        return Token.WHITESPACE;
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();
        LexicalAnalyzer lexer = compiler.createLexer((Objects.requireNonNull(text).toString()));
        int previousEnd = -1;
        int previousStartOffset = -1;

        for (net.emustudio.emulib.plugins.compiler.Token token : lexer) {
            try {
                int tokenMakerType = getTokenMakerType(token.getType());

                int tokenStartIndex = token.getOffset();
                int tokenLength = token.getText().length() - 1;
                if (token.getType() == net.emustudio.emulib.plugins.compiler.Token.EOF) {
                    tokenLength = 0;
                }

                int start = text.offset + tokenStartIndex;
                int end = text.offset + tokenStartIndex + tokenLength;
                int tokenStartOffset = startOffset + tokenStartIndex;

                if (previousEnd == -1 && tokenStartIndex != 0) {
                    // we have a gap in the beginning! Let's treat this gap as ERROR
                    addToken(text, text.offset, start - 1, Token.ERROR_CHAR, startOffset);
                } else if (previousEnd != -1 && start != (previousEnd + 1)) {
                    // we have a gap in the middle! Let's treat this gap as ERROR
                    addToken(
                            text, previousEnd + 1, start - 1, Token.ERROR_CHAR, previousStartOffset + 1
                    );
                }
                previousEnd = end;
                previousStartOffset = tokenStartOffset;

                addToken(text, start, end, tokenMakerType, tokenStartOffset);
            } catch (Exception ignored) {
                ignored.printStackTrace();

            }
        }
        return firstToken;
    }

    @Override
    public TokenMap getWordsToHighlight() {
        return new TokenMap();
    }
}
