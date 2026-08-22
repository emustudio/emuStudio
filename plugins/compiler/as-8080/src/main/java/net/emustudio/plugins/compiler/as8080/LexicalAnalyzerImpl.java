/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.shared.CharArrayCharStream;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {
    public static final int[] tokenMap = new int[As8080Lexer.EOL + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[COMMENT2] = Token.COMMENT;
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
        tokenMap[PREP_END] = Token.PREPROCESSOR;

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
        tokenMap[OP_MOD_2] = Token.OPERATOR;
        tokenMap[OP_SHR] = Token.OPERATOR;
        tokenMap[OP_SHR_2] = Token.OPERATOR;
        tokenMap[OP_SHL] = Token.OPERATOR;
        tokenMap[OP_SHL_2] = Token.OPERATOR;
        tokenMap[OP_NOT] = Token.OPERATOR;
        tokenMap[OP_NOT_2] = Token.OPERATOR;
        tokenMap[OP_AND] = Token.OPERATOR;
        tokenMap[OP_AND_2] = Token.OPERATOR;
        tokenMap[OP_OR] = Token.OPERATOR;
        tokenMap[OP_OR_2] = Token.OPERATOR;
        tokenMap[OP_XOR] = Token.OPERATOR;
        tokenMap[OP_XOR_2] = Token.OPERATOR;
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
    }

    private final As8080Lexer lexer;


    public LexicalAnalyzerImpl(As8080Lexer lexer) {
        this.lexer = Objects.requireNonNull(lexer);
        this.lexer.setTokenFactory(new EmuTokenFactory(tokenMap));
    }

    @Override
    public Token next() {
        return (Token) lexer.nextToken();
    }

    @Override
    public boolean hasNext() {
        return !lexer._hitEOF;
    }

    @Override
    public void reset(char[] array, int offset, int length) {
        lexer.setInputStream(new CharArrayCharStream(array, offset, length));
    }

    static class EmuToken extends CommonToken implements Token {
        EmuToken(Pair<TokenSource, CharStream> source, int type, int channel, int start, int stop) {
            super(source, type, channel, start, stop);
        }

        EmuToken(int type, String text) {
            super(type, text);
        }

        @Override
        public int getOffset() {
            return getStartIndex();
        }
    }

    static class EmuTokenFactory implements TokenFactory<EmuToken> {
        private final int[] tokenMap;

        EmuTokenFactory(int[] tokenMap) {
            this.tokenMap = tokenMap;
        }

        private int convertType(int antlrType) {
            if (antlrType == org.antlr.v4.runtime.Token.EOF) {
                return Token.EOF;
            }
            return tokenMap[antlrType];
        }

        @Override
        public EmuToken create(
                Pair<TokenSource, CharStream> source, int type, String text,
                int channel, int start, int stop, int line, int charPositionInLine) {
            EmuToken t = new EmuToken(source, convertType(type), channel, start, stop);
            t.setLine(line);
            t.setCharPositionInLine(charPositionInLine);
            if (text != null) {
                t.setText(text);
            } else if (source.b != null) {
                t.setText(source.b.getText(Interval.of(start, stop)));
            }
            return t;
        }

        @Override
        public EmuToken create(int type, String text) {
            return new EmuToken(convertType(type), text);
        }
    }
}
