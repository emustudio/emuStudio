package net.emustudio.plugins.compiler.as8080;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    private final As8080Lexer lexer;
    private static final int[] tokenMap = new int[As8080Lexer.EOL + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[OPCODE_STC] = Token.RESERVED;
        tokenMap[OPCODE_CMC] = Token.RESERVED;
        tokenMap[OPCODE_INR] = Token.RESERVED;
        tokenMap[OPCODE_DCR] = Token.RESERVED;
        tokenMap[OPCODE_CMA] = Token.RESERVED;
        tokenMap[OPCODE_DAA] = Token.RESERVED;
        tokenMap[OPCODE_NOP] = Token.RESERVED;
        tokenMap[OPCODE_MOV] = Token.RESERVED;
        tokenMap[OPCODE_STAX] = Token.RESERVED;
        tokenMap[OPCODE_LDAX] = Token.RESERVED;
        tokenMap[OPCODE_ADD] = Token.RESERVED;
        tokenMap[OPCODE_ADC] = Token.RESERVED;
        tokenMap[OPCODE_SUB] = Token.RESERVED;
        tokenMap[OPCODE_SBB] = Token.RESERVED;
        tokenMap[OPCODE_ANA] = Token.RESERVED;
        tokenMap[OPCODE_XRA] = Token.RESERVED;
        tokenMap[OPCODE_ORA] = Token.RESERVED;
        tokenMap[OPCODE_CMP] = Token.RESERVED;
        tokenMap[OPCODE_RLC] = Token.RESERVED;
        tokenMap[OPCODE_RRC] = Token.RESERVED;
        tokenMap[OPCODE_RAL] = Token.RESERVED;
        tokenMap[OPCODE_RAR] = Token.RESERVED;
        tokenMap[OPCODE_PUSH] = Token.RESERVED;
        tokenMap[OPCODE_POP] = Token.RESERVED;
        tokenMap[OPCODE_DAD] = Token.RESERVED;
        tokenMap[OPCODE_INX] = Token.RESERVED;
        tokenMap[OPCODE_DCX] = Token.RESERVED;
        tokenMap[OPCODE_XCHG] = Token.RESERVED;
        tokenMap[OPCODE_XTHL] = Token.RESERVED;
        tokenMap[OPCODE_SPHL] = Token.RESERVED;
        tokenMap[OPCODE_LXI] = Token.RESERVED;
        tokenMap[OPCODE_MVI] = Token.RESERVED;
        tokenMap[OPCODE_ADI] = Token.RESERVED;
        tokenMap[OPCODE_ACI] = Token.RESERVED;
        tokenMap[OPCODE_SUI] = Token.RESERVED;
        tokenMap[OPCODE_SBI] = Token.RESERVED;
        tokenMap[OPCODE_ANI] = Token.RESERVED;
        tokenMap[OPCODE_XRI] = Token.RESERVED;
        tokenMap[OPCODE_ORI] = Token.RESERVED;
        tokenMap[OPCODE_CPI] = Token.RESERVED;
        tokenMap[OPCODE_STA] = Token.RESERVED;
        tokenMap[OPCODE_LDA] = Token.RESERVED;
        tokenMap[OPCODE_SHLD] = Token.RESERVED;
        tokenMap[OPCODE_LHLD] = Token.RESERVED;
        tokenMap[OPCODE_PCHL] = Token.RESERVED;
        tokenMap[OPCODE_JMP] = Token.RESERVED;
        tokenMap[OPCODE_JC] = Token.RESERVED;
        tokenMap[OPCODE_JNC] = Token.RESERVED;
        tokenMap[OPCODE_JZ] = Token.RESERVED;
        tokenMap[OPCODE_JNZ] = Token.RESERVED;
        tokenMap[OPCODE_JP] = Token.RESERVED;
        tokenMap[OPCODE_JM] = Token.RESERVED;
        tokenMap[OPCODE_JPE] = Token.RESERVED;
        tokenMap[OPCODE_JPO] = Token.RESERVED;
        tokenMap[OPCODE_CALL] = Token.RESERVED;
        tokenMap[OPCODE_CC] = Token.RESERVED;
        tokenMap[OPCODE_CNC] = Token.RESERVED;
        tokenMap[OPCODE_CZ] = Token.RESERVED;
        tokenMap[OPCODE_CNZ] = Token.RESERVED;
        tokenMap[OPCODE_CP] = Token.RESERVED;
        tokenMap[OPCODE_CM] = Token.RESERVED;
        tokenMap[OPCODE_CPE] = Token.RESERVED;
        tokenMap[OPCODE_CPO] = Token.RESERVED;
        tokenMap[OPCODE_RET] = Token.RESERVED;
        tokenMap[OPCODE_RC] = Token.RESERVED;
        tokenMap[OPCODE_RNC] = Token.RESERVED;
        tokenMap[OPCODE_RZ] = Token.RESERVED;
        tokenMap[OPCODE_RNZ] = Token.RESERVED;
        tokenMap[OPCODE_RM] = Token.RESERVED;
        tokenMap[OPCODE_RP] = Token.RESERVED;
        tokenMap[OPCODE_RPE] = Token.RESERVED;
        tokenMap[OPCODE_RPO] = Token.RESERVED;
        tokenMap[OPCODE_RST] = Token.RESERVED;
        tokenMap[OPCODE_EI] = Token.RESERVED;
        tokenMap[OPCODE_DI] = Token.RESERVED;
        tokenMap[OPCODE_IN] = Token.RESERVED;
        tokenMap[OPCODE_OUT] = Token.RESERVED;
        tokenMap[OPCODE_HLT] = Token.RESERVED;

        tokenMap[PREP_ORG] = Token.PREPROCESSOR;
        tokenMap[PREP_EQU] = Token.PREPROCESSOR;
        tokenMap[PREP_SET] = Token.PREPROCESSOR;
        tokenMap[PREP_INCLUDE] = Token.PREPROCESSOR;
        tokenMap[PREP_IF] = Token.PREPROCESSOR;
        tokenMap[PREP_ENDIF] = Token.PREPROCESSOR;
        tokenMap[PREP_MACRO] = Token.PREPROCESSOR;
        tokenMap[PREP_ENDM] = Token.PREPROCESSOR;
        tokenMap[PREP_DB] = Token.PREPROCESSOR;
        tokenMap[PREP_DW] = Token.PREPROCESSOR;
        tokenMap[PREP_DS] = Token.PREPROCESSOR;
        tokenMap[PREP_ADDR] = Token.PREPROCESSOR;

        tokenMap[REG_A] = Token.REGISTER;
        tokenMap[REG_B] = Token.REGISTER;
        tokenMap[REG_C] = Token.REGISTER;
        tokenMap[REG_D] = Token.REGISTER;
        tokenMap[REG_E] = Token.REGISTER;
        tokenMap[REG_H] = Token.REGISTER;
        tokenMap[REG_L] = Token.REGISTER;
        tokenMap[REG_M] = Token.REGISTER;
        tokenMap[REG_PSW] = Token.REGISTER;
        tokenMap[REG_SP] = Token.REGISTER;

        tokenMap[SEP_LPAR] = Token.SEPARATOR;
        tokenMap[SEP_RPAR] = Token.SEPARATOR;
        tokenMap[SEP_COMMA] = Token.SEPARATOR;

        tokenMap[OP_ADD] = Token.OPERATOR;
        tokenMap[OP_SUBTRACT] = Token.OPERATOR;
        tokenMap[OP_MULTIPLY] = Token.OPERATOR;
        tokenMap[OP_DIVIDE] = Token.OPERATOR;
        tokenMap[OP_EQUAL] = Token.OPERATOR;
        tokenMap[OP_MOD] = Token.OPERATOR;
        tokenMap[OP_SHR] = Token.OPERATOR;
        tokenMap[OP_SHL] = Token.OPERATOR;
        tokenMap[OP_NOT] = Token.OPERATOR;
        tokenMap[OP_AND] = Token.OPERATOR;
        tokenMap[OP_OR] = Token.OPERATOR;
        tokenMap[OP_XOR] = Token.OPERATOR;

        tokenMap[LIT_NUMBER] =Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_1] =Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_2] =Token.LITERAL;
        tokenMap[LIT_OCTNUMBER] =Token.LITERAL;
        tokenMap[LIT_BINNUMBER] =Token.LITERAL;
        tokenMap[LIT_STRING_1] =Token.LITERAL;
        tokenMap[LIT_STRING_2] =Token.LITERAL;

        tokenMap[ID_IDENTIFIER] = Token.IDENTIFIER;
        tokenMap[ID_LABEL] = Token.IDENTIFIER;

        tokenMap[ERROR] = Token.ERROR;
    }


    public LexicalAnalyzerImpl(As8080Lexer lexer) {
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
        return tokenMap[tokenType];
    }
}
