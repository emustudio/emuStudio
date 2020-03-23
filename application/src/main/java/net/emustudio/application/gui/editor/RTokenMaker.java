package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;
import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

import static net.emustudio.emulib.plugins.compiler.Token.*;

public class RTokenMaker extends AbstractTokenMaker {
    private final LexicalAnalyzer lexicalAnalyzer;

    public RTokenMaker(LexicalAnalyzer lexicalAnalyzer) {
        this.lexicalAnalyzer = Objects.requireNonNull(lexicalAnalyzer);
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        // Token starting offsets are always of the form:
        // 'startOffset + (currentTokenStart-offset)', but since startOffset and
        // offset are constant, tokens' starting positions become:
        // 'newStartOffset+currentTokenStart'.
        int newStartOffset = startOffset - offset;

        lexicalAnalyzer.reset(new StringReader(text.toString()), 0, offset, 0);
        int lastOffset = offset;
        for (int i = offset; i < end; ) {
            try {
                int expectedTokenStart = i;

                net.emustudio.emulib.plugins.compiler.Token token = lexicalAnalyzer.getToken();
                int tokenStart = token.getOffset();
                int tokenEnd = tokenStart + token.getLength() - 1;
                int skipChars = token.getLength();

                if (token.getType() != TEOF && token.getLength() > 0) {
                    int tokenMakerType = getTokenMakerType(token.getType());
                    if (tokenStart > expectedTokenStart) {
                        skipChars += tokenStart - expectedTokenStart;
                        // fill the gap
                        addToken(
                            text, expectedTokenStart, tokenStart - 1, Token.WHITESPACE,
                            newStartOffset + expectedTokenStart
                        );
                        lastOffset = tokenStart - 1;
                    }

                    addToken(
                        text, tokenStart, tokenEnd, tokenMakerType, newStartOffset + tokenStart
                    );
                    lastOffset = tokenEnd;
                } else {
                    // fill the gap
                    addToken(
                        text, expectedTokenStart, end - 1, Token.WHITESPACE, newStartOffset + expectedTokenStart
                    );
                    lastOffset = end - 1;
                    break;
                }

                i += skipChars;
            } catch (IOException donotlogit) {
                donotlogit.printStackTrace();
            }
        }
        if (offset == end || lastOffset < end - 1) {
            addNullToken();
        }
        return firstToken;
    }

    @Override
    public TokenMap getWordsToHighlight() {
        return new TokenMap();
    }

    private static int getTokenMakerType(int emuStudioTokenType) {
        switch (emuStudioTokenType) {
            case TEOF:
                return Token.NULL;
            case RESERVED:
                return Token.RESERVED_WORD;
            case PREPROCESSOR:
                return Token.PREPROCESSOR;
            case REGISTER:
                return Token.RESERVED_WORD_2;
            case SEPARATOR:
                return Token.SEPARATOR;
            case OPERATOR:
                return Token.OPERATOR;
            case COMMENT:
                return Token.COMMENT_MARKUP;
            case LITERAL:
                return Token.LITERAL_NUMBER_DECIMAL_INT;
            case IDENTIFIER:
                return Token.IDENTIFIER;
            case LABEL:
                return Token.ANNOTATION;
            case ERROR:
                return Token.ERROR_IDENTIFIER;
        }
        return Token.WHITESPACE;
    }
}
