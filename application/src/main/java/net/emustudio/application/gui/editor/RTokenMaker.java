/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.jcip.annotations.NotThreadSafe;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Segment;
import java.util.Objects;

@NotThreadSafe
public class RTokenMaker extends AbstractTokenMaker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTokenMaker.class);

    private final LexicalAnalyzer lexer;

    public RTokenMaker(Compiler compiler) {
        this.lexer = compiler.createLexer();
    }

    static int getTokenMakerType(int emuStudioTokenType) {
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
        try {
            lexer.reset(Objects.requireNonNull(text).toString());
        } catch (Exception ex) {
            LOGGER.error("Could not reset lexer", ex);
            addNullToken();
            return firstToken;
        }

        int previousEnd = -1;
        int previousStartOffset = -1;

        while (lexer.hasNext()) {
            net.emustudio.emulib.plugins.compiler.Token token;
            try {
                token = lexer.next();
            } catch (Exception ex) {
                // If the lexer itself throws, we cannot trust its state — stop iterating
                // to avoid a potential infinite loop that would freeze the EDT.
                LOGGER.error("Lexer threw during tokenization", ex);
                break;
            }

            try {
                // Skip EOF — we terminate the list with addNullToken() after the loop
                if (token.getType() == net.emustudio.emulib.plugins.compiler.Token.EOF) {
                    break;
                }

                int tokenMakerType = getTokenMakerType(token.getType());

                int tokenStartIndex = token.getOffset();
                String tokenText = token.getText();
                if (tokenText == null || tokenText.isEmpty()) {
                    continue; // skip zero-length tokens to prevent end < start
                }
                int tokenLength = tokenText.length() - 1;

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
            } catch (Exception ex) {
                LOGGER.error("Could not process token", ex);
            }
        }
        addNullToken();
        return firstToken;
    }

    @Override
    public TokenMap getWordsToHighlight() {
        return new TokenMap();
    }
}
