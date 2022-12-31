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
package net.emustudio.plugins.compiler.asZ80;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Lexer.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    public static final int[] tokenMap = new int[AsZ80Lexer.EOL + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[COMMENT2] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[IM_WS] = Token.WHITESPACE;
        tokenMap[COND_WS] = Token.WHITESPACE;
        tokenMap[OPCODE_ADC] = Token.RESERVED;
        tokenMap[OPCODE_AND] = Token.RESERVED;
        tokenMap[OPCODE_ADD] = Token.RESERVED;
        tokenMap[OPCODE_BIT] = Token.RESERVED;
        tokenMap[OPCODE_CALL] = Token.RESERVED;
        tokenMap[OPCODE_CCF] = Token.RESERVED;
        tokenMap[OPCODE_CP] = Token.RESERVED;
        tokenMap[OPCODE_CPD] = Token.RESERVED;
        tokenMap[OPCODE_CPDR] = Token.RESERVED;
        tokenMap[OPCODE_CPI] = Token.RESERVED;
        tokenMap[OPCODE_CPIR] = Token.RESERVED;
        tokenMap[OPCODE_CPL] = Token.RESERVED;
        tokenMap[OPCODE_DAA] = Token.RESERVED;
        tokenMap[OPCODE_DEC] = Token.RESERVED;
        tokenMap[OPCODE_DI] = Token.RESERVED;
        tokenMap[OPCODE_DJNZ] = Token.RESERVED;
        tokenMap[OPCODE_EI] = Token.RESERVED;
        tokenMap[OPCODE_EX] = Token.RESERVED;
        tokenMap[OPCODE_EXX] = Token.RESERVED;
        tokenMap[OPCODE_HALT] = Token.RESERVED;
        tokenMap[OPCODE_IM] = Token.RESERVED;
        tokenMap[OPCODE_IN] = Token.RESERVED;
        tokenMap[OPCODE_INC] = Token.RESERVED;
        tokenMap[OPCODE_IND] = Token.RESERVED;
        tokenMap[OPCODE_INDR] = Token.RESERVED;
        tokenMap[OPCODE_INI] = Token.RESERVED;
        tokenMap[OPCODE_INIR] = Token.RESERVED;
        tokenMap[OPCODE_JP] = Token.RESERVED;
        tokenMap[OPCODE_JR] = Token.RESERVED;
        tokenMap[OPCODE_LD] = Token.RESERVED;
        tokenMap[OPCODE_LDD] = Token.RESERVED;
        tokenMap[OPCODE_LDDR] = Token.RESERVED;
        tokenMap[OPCODE_LDI] = Token.RESERVED;
        tokenMap[OPCODE_LDIR] = Token.RESERVED;
        tokenMap[OPCODE_NEG] = Token.RESERVED;
        tokenMap[OPCODE_NOP] = Token.RESERVED;
        tokenMap[OPCODE_OR] = Token.RESERVED;
        tokenMap[OPCODE_OTDR] = Token.RESERVED;
        tokenMap[OPCODE_OTIR] = Token.RESERVED;
        tokenMap[OPCODE_OUT] = Token.RESERVED;
        tokenMap[OPCODE_OUTD] = Token.RESERVED;
        tokenMap[OPCODE_OUTI] = Token.RESERVED;
        tokenMap[OPCODE_POP] = Token.RESERVED;
        tokenMap[OPCODE_PUSH] = Token.RESERVED;
        tokenMap[OPCODE_RES] = Token.RESERVED;
        tokenMap[OPCODE_RET] = Token.RESERVED;
        tokenMap[OPCODE_RETI] = Token.RESERVED;
        tokenMap[OPCODE_RETN] = Token.RESERVED;
        tokenMap[OPCODE_RL] = Token.RESERVED;
        tokenMap[OPCODE_RLA] = Token.RESERVED;
        tokenMap[OPCODE_RLC] = Token.RESERVED;
        tokenMap[OPCODE_RLCA] = Token.RESERVED;
        tokenMap[OPCODE_RLD] = Token.RESERVED;
        tokenMap[OPCODE_RR] = Token.RESERVED;
        tokenMap[OPCODE_RRA] = Token.RESERVED;
        tokenMap[OPCODE_RRC] = Token.RESERVED;
        tokenMap[OPCODE_RRCA] = Token.RESERVED;
        tokenMap[OPCODE_RRD] = Token.RESERVED;
        tokenMap[OPCODE_RST] = Token.RESERVED;
        tokenMap[OPCODE_SBC] = Token.RESERVED;
        tokenMap[OPCODE_SCF] = Token.RESERVED;
        tokenMap[OPCODE_SET] = Token.RESERVED;
        tokenMap[OPCODE_SLA] = Token.RESERVED;
        tokenMap[OPCODE_SRA] = Token.RESERVED;
        tokenMap[OPCODE_SLL] = Token.RESERVED;
        tokenMap[OPCODE_SRL] = Token.RESERVED;
        tokenMap[OPCODE_SUB] = Token.RESERVED;
        tokenMap[OPCODE_XOR] = Token.RESERVED;
        tokenMap[COND_C] = Token.RESERVED;
        tokenMap[COND_NC] = Token.RESERVED;
        tokenMap[COND_Z] = Token.RESERVED;
        tokenMap[COND_NZ] = Token.RESERVED;
        tokenMap[COND_M] = Token.RESERVED;
        tokenMap[COND_P] = Token.RESERVED;
        tokenMap[COND_PE] = Token.RESERVED;
        tokenMap[COND_PO] = Token.RESERVED;
        tokenMap[IM_01] = Token.RESERVED;
        tokenMap[IM_0] = Token.RESERVED;
        tokenMap[IM_1] = Token.RESERVED;
        tokenMap[IM_2] = Token.RESERVED;

        tokenMap[PREP_ORG] = Token.PREPROCESSOR;
        tokenMap[PREP_EQU] = Token.PREPROCESSOR;
        tokenMap[PREP_VAR] = Token.PREPROCESSOR;
        tokenMap[PREP_INCLUDE] = Token.PREPROCESSOR;
        tokenMap[PREP_IF] = Token.PREPROCESSOR;
        tokenMap[PREP_ENDIF] = Token.PREPROCESSOR;
        tokenMap[PREP_MACRO] = Token.PREPROCESSOR;
        tokenMap[PREP_ENDM] = Token.PREPROCESSOR;
        tokenMap[PREP_DB] = Token.PREPROCESSOR;
        tokenMap[PREP_DW] = Token.PREPROCESSOR;
        tokenMap[PREP_DS] = Token.PREPROCESSOR;
        tokenMap[PREP_ADDR] = Token.PREPROCESSOR;
        tokenMap[PREP_END] = Token.PREPROCESSOR;

        tokenMap[REG_A] = Token.REGISTER;
        tokenMap[REG_B] = Token.REGISTER;
        tokenMap[REG_C] = Token.REGISTER;
        tokenMap[REG_D] = Token.REGISTER;
        tokenMap[REG_E] = Token.REGISTER;
        tokenMap[REG_H] = Token.REGISTER;
        tokenMap[REG_L] = Token.REGISTER;
        tokenMap[REG_IX] = Token.REGISTER;
        tokenMap[REG_IY] = Token.REGISTER;
        tokenMap[REG_SP] = Token.REGISTER;
        tokenMap[REG_HL] = Token.REGISTER;
        tokenMap[REG_DE] = Token.REGISTER;
        tokenMap[REG_BC] = Token.REGISTER;
        tokenMap[REG_IXH] = Token.REGISTER;
        tokenMap[REG_IXL] = Token.REGISTER;
        tokenMap[REG_IYH] = Token.REGISTER;
        tokenMap[REG_IYL] = Token.REGISTER;
        tokenMap[REG_AF] = Token.REGISTER;
        tokenMap[REG_AFF] = Token.REGISTER;
        tokenMap[REG_I] = Token.REGISTER;
        tokenMap[REG_R] = Token.REGISTER;

        tokenMap[SEP_LPAR] = Token.SEPARATOR;
        tokenMap[SEP_RPAR] = Token.SEPARATOR;
        tokenMap[SEP_COMMA] = Token.SEPARATOR;

        tokenMap[OP_ADD] = Token.OPERATOR;
        tokenMap[OP_SUBTRACT] = Token.OPERATOR;
        tokenMap[OP_MULTIPLY] = Token.OPERATOR;
        tokenMap[OP_DIVIDE] = Token.OPERATOR;
        tokenMap[OP_EQUAL] = Token.OPERATOR;
        tokenMap[OP_MOD] = Token.OPERATOR;
        tokenMap[OP_MOD_2] = Token.OPERATOR;
        tokenMap[OP_SHR] = Token.OPERATOR;
        tokenMap[OP_SHR_2] = Token.OPERATOR;
        tokenMap[OP_SHL] = Token.OPERATOR;
        tokenMap[OP_SHL_2] = Token.OPERATOR;
        tokenMap[OP_NOT] = Token.OPERATOR;
        tokenMap[OP_NOT_2] = Token.OPERATOR;
        tokenMap[OP_AND] = Token.OPERATOR;
        tokenMap[OP_OR] = Token.OPERATOR;
        tokenMap[OP_XOR] = Token.OPERATOR;
        tokenMap[OP_LT] = Token.OPERATOR;
        tokenMap[OP_LTE] = Token.OPERATOR;
        tokenMap[OP_GT] = Token.OPERATOR;
        tokenMap[OP_GTE] = Token.OPERATOR;

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
        tokenMap[ERROR_IM] = Token.ERROR;
        tokenMap[ERROR_COND] = Token.ERROR;
    }

    private final AsZ80Lexer lexer;


    public LexicalAnalyzerImpl(AsZ80Lexer lexer) {
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
