package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static net.emustudio.plugins.compiler.ram.RAMLexer.*;
import static org.antlr.v4.runtime.Recognizer.EOF;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    private final RAMLexer lexer;
    private static final int[] tokenMap = new int[ERROR + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[COMMENT2] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[OPCODE_READ] = Token.RESERVED;
        tokenMap[OPCODE_WRITE] = Token.RESERVED;
        tokenMap[OPCODE_LOAD] = Token.RESERVED;
        tokenMap[OPCODE_STORE] = Token.RESERVED;
        tokenMap[OPCODE_ADD] = Token.RESERVED;
        tokenMap[OPCODE_HALT] = Token.RESERVED;
        tokenMap[OPCODE_DIV] = Token.RESERVED;
        tokenMap[OPCODE_MUL] = Token.RESERVED;
        tokenMap[OPCODE_SUB] = Token.RESERVED;
        tokenMap[OPCODE_JMP] = Token.RESERVED;
        tokenMap[OPCODE_JGTZ] = Token.RESERVED;
        tokenMap[OPCODE_JZ] = Token.RESERVED;

        tokenMap[PREP_INPUT] = Token.PREPROCESSOR;

        tokenMap[OP_CONSTANT] = Token.OPERATOR;
        tokenMap[OP_INDIRECT] = Token.OPERATOR;

        tokenMap[LIT_NUMBER] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_1] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_2] = Token.LITERAL;
        tokenMap[LIT_OCTNUMBER] = Token.LITERAL;
        tokenMap[LIT_BINNUMBER] = Token.LITERAL;
        tokenMap[LIT_STRING_1] = Token.LITERAL;
        tokenMap[LIT_STRING_2] = Token.LITERAL;

        tokenMap[ID_IDENTIFIER] = Token.IDENTIFIER;
        tokenMap[ID_LABEL] = Token.IDENTIFIER;

        tokenMap[ERROR] = Token.ERROR;
    }


    public LexicalAnalyzerImpl(RAMLexer lexer) {
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
