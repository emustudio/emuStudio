/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.jcip.annotations.NotThreadSafe;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Segment;
import java.util.Objects;

/**
 * RSyntaxTextArea TokenMaker that delegates tokenization to the compiler's
 * {@link LexicalAnalyzer}.
 * <p>
 * Extends {@link TokenMakerBase} directly (instead of {@code AbstractTokenMaker})
 * to avoid allocating an unused {@code TokenMap} on every construction.
 * Uses {@link LexicalAnalyzer#reset(char[], int, int)} to feed the lexer
 * the Segment's backing char array directly (zero-copy via {@code CharArrayCharStream}).
 */
@NotThreadSafe
public class RTokenMaker extends TokenMakerBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTokenMaker.class);

    private final LexicalAnalyzer lexer;

    public RTokenMaker(Compiler compiler) {
        this.lexer = Objects.requireNonNull(compiler).createLexer();
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
            default:
                return Token.WHITESPACE;
        }
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int textOffset = text.offset;
        int count = text.count;

        // offsetShift translates array indices into document offsets:
        //   documentOffset = arrayIndex + offsetShift
        // This is the standard RSyntaxTextArea pattern (see AbstractJFlexTokenMaker).
        int offsetShift = -textOffset + startOffset;

        try {
            // Feed the lexer the Segment's backing char[] directly.
            // The LexicalAnalyzer implementation wraps it in a zero-copy
            // CharArrayCharStream (no String or CharBuffer allocation).
            lexer.reset(array, textOffset, count);
        } catch (Exception ex) {
            LOGGER.error("Could not reset lexer", ex);
            addNullToken();
            return firstToken;
        }

        int previousEnd = -1;

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
                int emuType = token.getType();

                // Skip EOF — we terminate the list with addNullToken() after the loop
                if (emuType == net.emustudio.emulib.plugins.compiler.Token.EOF) {
                    break;
                }

                String tokenText = token.getText();
                if (tokenText == null || tokenText.isEmpty()) {
                    continue; // skip zero-length tokens to prevent end < start
                }

                int start = textOffset + token.getOffset();
                int end = start + tokenText.length() - 1;
                int tokenMakerType = getTokenMakerType(emuType);

                // Fill gaps with ERROR tokens (same idea as JFlex's catch-all rules)
                if (previousEnd == -1 && start != textOffset) {
                    addToken(array, textOffset, start - 1, Token.ERROR_CHAR, textOffset + offsetShift);
                } else if (previousEnd != -1 && start != previousEnd + 1) {
                    addToken(array, previousEnd + 1, start - 1, Token.ERROR_CHAR, previousEnd + 1 + offsetShift);
                }
                previousEnd = end;

                addToken(array, start, end, tokenMakerType, start + offsetShift);
            } catch (Exception ex) {
                LOGGER.error("Could not process token", ex);
            }
        }
        addNullToken();
        return firstToken;
    }
}
