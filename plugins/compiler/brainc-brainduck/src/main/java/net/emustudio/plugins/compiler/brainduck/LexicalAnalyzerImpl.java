package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    private final BraincLexer lexer;

    public LexicalAnalyzerImpl(BraincLexer lexer) {
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
            case BraincLexer.COMMENT:
                return Token.COMMENT;
            case BraincLexer.WS:
                return Token.WHITESPACE;
            case BraincLexer.DEC:
            case BraincLexer.DECV:
            case BraincLexer.INC:
            case BraincLexer.INCV:
            case BraincLexer.PRINT:
            case BraincLexer.LOAD:
            case BraincLexer.LOOP:
                return Token.RESERVED;
            case BraincLexer.EOF:
                return Token.EOF;
        }
        return Token.ERROR;
    }
}
