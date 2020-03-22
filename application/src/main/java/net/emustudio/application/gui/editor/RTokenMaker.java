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

        int currentTokenStart = offset;
        boolean currentTokenInitialLexicalState = initialTokenType == Token.NULL;

        System.out.println("'" + text + "'; startOffset=" + startOffset + ", initialTokenType=" + initialTokenType);

        lexicalAnalyzer.reset(new StringReader(text.toString()), 0, startOffset, 0);
        for (int i = offset; i < end; ) {

            try {
                if (currentTokenInitialLexicalState) {
                    currentTokenStart = i;
                }

                net.emustudio.emulib.plugins.compiler.Token token = lexicalAnalyzer.getSymbol();
                System.out.println("Token: " + token);
                System.out.println("  [" + token.getOffset() + "; " + token.getLength() + "] '" + token.getText() + "'");
                System.out.println("  " + tokenTypeString(token.getType()));

                currentTokenInitialLexicalState = token.isInitialLexicalState();

                int length = token.getLength();
                int tokenMakerType = getTokenMakerType(token.getType());

                if (token.getType() == TEOF) {
                    break;
                }

                if (token.getOffset() > currentTokenStart) {
                    currentTokenStart = token.getOffset();
                }

                addToken(
                    text, currentTokenStart, currentTokenStart + length, tokenMakerType,
                    newStartOffset + currentTokenStart
                );

                i += length;
            } catch (IOException dontknowyet) {
                dontknowyet.printStackTrace();
                // TODO
            }
        }

        if (currentTokenInitialLexicalState) {
            addNullToken();
        } else {
            addToken(text, currentTokenStart, end - 1, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
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
        return Token.NULL;
    }

    private String tokenTypeString(int type) {
        switch (type) {
            case TEOF:
                return "TEOF";
            case RESERVED:
                return "RESERVED";
            case PREPROCESSOR:
                return "PREPROCESSOR";
            case REGISTER:
                return "REGISTER";
            case SEPARATOR:
                return "SEPARATOR";
            case OPERATOR:
                return "OPERATOR";
            case COMMENT:
                return "COMMENT";
            case LITERAL:
                return "LITERAL";
            case IDENTIFIER:
                return "IDENTIFIER";
            case LABEL:
                return "LABEL";
            case ERROR:
                return "ERROR";
        }
        return "dont know";
    }
}
