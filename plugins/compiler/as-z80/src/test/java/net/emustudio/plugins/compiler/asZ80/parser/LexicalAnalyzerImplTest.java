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
package net.emustudio.plugins.compiler.asZ80.parser;

import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Lexer.*;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTokenTypesIgnoreCase;

public class LexicalAnalyzerImplTest {

    @Test
    public void testParseEols() {
        assertTokenTypes("\n\n\n\n\n", EOL, EOL, EOL, EOL, EOL, EOF);
    }

    @Test
    public void testParseError1() {
        assertTokenTypes("B :", REG_B, WS, ERROR, EOF);
    }

    @Test
    public void testParseError2() {
        assertTokenTypes("0x 9o 22b", ERROR, WS, ERROR, WS, ERROR, EOF);
    }

    @Test
    public void testParseHex1() {
        assertTokenTypes(
                "0x1 0x0 -0x5f -0xFffF 0x1BC",
                LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, WS, OP_SUBTRACT, LIT_HEXNUMBER_1, WS, OP_SUBTRACT, LIT_HEXNUMBER_1,
                WS, LIT_HEXNUMBER_1, EOF
        );
    }

    @Test
    public void testParseHex2() {
        assertTokenTypes(
                "1h 0h -5Fh -FFFFh 1BCh 5h -5h",
                LIT_HEXNUMBER_2, WS, LIT_HEXNUMBER_2, WS, OP_SUBTRACT, LIT_HEXNUMBER_2, WS, OP_SUBTRACT, LIT_HEXNUMBER_2,
                WS, LIT_HEXNUMBER_2, WS, LIT_HEXNUMBER_2, WS, OP_SUBTRACT, LIT_HEXNUMBER_2, EOF
        );
    }

    @Test
    public void testParseDecimal() {
        assertTokenTypes(
                "0 1 -2 3 -4 5 66 999",
                LIT_NUMBER, WS, LIT_NUMBER, WS, OP_SUBTRACT, LIT_NUMBER, WS, LIT_NUMBER, WS, OP_SUBTRACT, LIT_NUMBER,
                WS, LIT_NUMBER, WS, LIT_NUMBER, WS, LIT_NUMBER, EOF
        );
    }

    @Test
    public void testParseOctal() {
        assertTokenTypes("-6o 7q 11q -345O",
                OP_SUBTRACT, LIT_OCTNUMBER, WS, LIT_OCTNUMBER, WS, LIT_OCTNUMBER, WS, OP_SUBTRACT, LIT_OCTNUMBER, EOF
        );
    }

    @Test
    public void testParseBinary() {
        assertTokenTypes("10101010101010101010110b", LIT_BINNUMBER, EOF);
    }

    @Test
    public void testParseString1() {
        assertTokenTypes("'' 'sss'", LIT_STRING_1, WS, LIT_STRING_1, EOF);
        assertTokenTypes("'\nsss'", LIT_STRING_1, EOF);
    }

    @Test
    public void testParseString2() {
        assertTokenTypes("\"\" \"sss\"", LIT_STRING_2, WS, LIT_STRING_2, EOF);
        assertTokenTypes("\"\nsss\"", LIT_STRING_2, EOF);
    }

    @Test
    public void testParseString1Error() {
        assertTokenTypes("'open", ERROR, EOF);
    }

    @Test
    public void testParseString2Error() {
        assertTokenTypes("\"open", ERROR, EOF);
    }

    @Test
    public void testParseComment1() {
        assertTokenTypes("// comment fun1", COMMENT, EOF);
        assertTokenTypes("# comment fun1", COMMENT, EOF);
        assertTokenTypes("; comment fun1", COMMENT, EOF);
    }

    @Test
    public void testParseComment2() {
        assertTokenTypes("/*\n*\n* comment fun1\n\n*/", COMMENT2, EOF);
    }

    @Test
    public void testParseOpcodes() {
        assertTokenTypesIgnoreCase("ADC", OPCODE_ADC, EOF);
        assertTokenTypesIgnoreCase("ADD", OPCODE_ADD, EOF);
        assertTokenTypesIgnoreCase("AND", OPCODE_AND, EOF);
        assertTokenTypesIgnoreCase("BIT", OPCODE_BIT, EOF);
        assertTokenTypesIgnoreCase("CALL", OPCODE_CALL, EOF);
        assertTokenTypesIgnoreCase("CCF", OPCODE_CCF, EOF);
        assertTokenTypesIgnoreCase("CP", OPCODE_CP, EOF);
        assertTokenTypesIgnoreCase("CPD", OPCODE_CPD, EOF);
        assertTokenTypesIgnoreCase("CPDR", OPCODE_CPDR, EOF);
        assertTokenTypesIgnoreCase("CPI", OPCODE_CPI, EOF);
        assertTokenTypesIgnoreCase("CPIR", OPCODE_CPIR, EOF);
        assertTokenTypesIgnoreCase("CPL", OPCODE_CPL, EOF);
        assertTokenTypesIgnoreCase("DAA", OPCODE_DAA, EOF);
        assertTokenTypesIgnoreCase("DEC", OPCODE_DEC, EOF);
        assertTokenTypesIgnoreCase("DI", OPCODE_DI, EOF);
        assertTokenTypesIgnoreCase("DJNZ", OPCODE_DJNZ, EOF);
        assertTokenTypesIgnoreCase("EI", OPCODE_EI, EOF);
        assertTokenTypesIgnoreCase("EX", OPCODE_EX, EOF);
        assertTokenTypesIgnoreCase("EXX", OPCODE_EXX, EOF);
        assertTokenTypesIgnoreCase("HALT", OPCODE_HALT, EOF);
        assertTokenTypesIgnoreCase("IM", OPCODE_IM, EOF);
        assertTokenTypesIgnoreCase("IN", OPCODE_IN, EOF);
        assertTokenTypesIgnoreCase("INC", OPCODE_INC, EOF);
        assertTokenTypesIgnoreCase("IND", OPCODE_IND, EOF);
        assertTokenTypesIgnoreCase("INDR", OPCODE_INDR, EOF);
        assertTokenTypesIgnoreCase("INI", OPCODE_INI, EOF);
        assertTokenTypesIgnoreCase("INIR", OPCODE_INIR, EOF);
        assertTokenTypesIgnoreCase("JP", OPCODE_JP, EOF);  // condition
        assertTokenTypesIgnoreCase("JR", OPCODE_JR, EOF);  // condition
        assertTokenTypesIgnoreCase("LD", OPCODE_LD, EOF);
        assertTokenTypesIgnoreCase("LDD", OPCODE_LDD, EOF);
        assertTokenTypesIgnoreCase("LDDR", OPCODE_LDDR, EOF);
        assertTokenTypesIgnoreCase("LDI", OPCODE_LDI, EOF);
        assertTokenTypesIgnoreCase("LDIR", OPCODE_LDIR, EOF);
        assertTokenTypesIgnoreCase("NEG", OPCODE_NEG, EOF);
        assertTokenTypesIgnoreCase("NOP", OPCODE_NOP, EOF);
        assertTokenTypesIgnoreCase("OR", OPCODE_OR, EOF);
        assertTokenTypesIgnoreCase("OTDR", OPCODE_OTDR, EOF);
        assertTokenTypesIgnoreCase("OTIR", OPCODE_OTIR, EOF);
        assertTokenTypesIgnoreCase("OUT", OPCODE_OUT, EOF);
        assertTokenTypesIgnoreCase("OUTD", OPCODE_OUTD, EOF);
        assertTokenTypesIgnoreCase("OUTI", OPCODE_OUTI, EOF);
        assertTokenTypesIgnoreCase("POP", OPCODE_POP, EOF);
        assertTokenTypesIgnoreCase("PUSH", OPCODE_PUSH, EOF);
        assertTokenTypesIgnoreCase("RES", OPCODE_RES, EOF);
        assertTokenTypesIgnoreCase("RET", OPCODE_RET, EOF); // condition
        assertTokenTypesIgnoreCase("RETI", OPCODE_RETI, EOF);
        assertTokenTypesIgnoreCase("RETN", OPCODE_RETN, EOF);
        assertTokenTypesIgnoreCase("RL", OPCODE_RL, EOF);
        assertTokenTypesIgnoreCase("RLA", OPCODE_RLA, EOF);
        assertTokenTypesIgnoreCase("RLC", OPCODE_RLC, EOF);
        assertTokenTypesIgnoreCase("RLCA", OPCODE_RLCA, EOF);
        assertTokenTypesIgnoreCase("RLD", OPCODE_RLD, EOF);
        assertTokenTypesIgnoreCase("RR", OPCODE_RR, EOF);
        assertTokenTypesIgnoreCase("RRA", OPCODE_RRA, EOF);
        assertTokenTypesIgnoreCase("RRC", OPCODE_RRC, EOF);
        assertTokenTypesIgnoreCase("RRCA", OPCODE_RRCA, EOF);
        assertTokenTypesIgnoreCase("RRD", OPCODE_RRD, EOF);
        assertTokenTypesIgnoreCase("RST", OPCODE_RST, EOF);
        assertTokenTypesIgnoreCase("SBC", OPCODE_SBC, EOF);
        assertTokenTypesIgnoreCase("SCF", OPCODE_SCF, EOF);
        assertTokenTypesIgnoreCase("SET", OPCODE_SET, EOF);
        assertTokenTypesIgnoreCase("SLA", OPCODE_SLA, EOF);
        assertTokenTypesIgnoreCase("SRA", OPCODE_SRA, EOF);
        assertTokenTypesIgnoreCase("SLL", OPCODE_SLL, EOF);
        assertTokenTypesIgnoreCase("SRL", OPCODE_SRL, EOF);
        assertTokenTypesIgnoreCase("SUB", OPCODE_SUB, EOF);
        assertTokenTypesIgnoreCase("XOR", OPCODE_XOR, EOF);
    }

    @Test
    public void parseIm() {
        assertTokenTypesIgnoreCase("IM 0 0", OPCODE_IM, IM_WS, IM_0, WS, LIT_NUMBER, EOF);
        assertTokenTypesIgnoreCase("IM 0/1", OPCODE_IM, IM_WS, IM_01, EOF);
        assertTokenTypesIgnoreCase("IM 3 0", OPCODE_IM, IM_WS, LIT_NUMBER, WS, LIT_NUMBER, EOF);
    }

    @Test
    public void parseRet() {
        assertTokenTypesIgnoreCase("ret and", OPCODE_RET, COND_WS, OPCODE_AND, EOF);
        assertTokenTypesIgnoreCase("ret nz", OPCODE_RET, COND_WS, COND_NZ, EOF);
        assertTokenTypesIgnoreCase("ret c c", OPCODE_RET, COND_WS, COND_C, WS, REG_C, EOF);
        assertTokenTypesIgnoreCase("ret pe", OPCODE_RET, COND_WS, COND_PE, EOF);
        assertTokenTypesIgnoreCase("ret po", OPCODE_RET, COND_WS, COND_PO, EOF);
        assertTokenTypesIgnoreCase("ret p", OPCODE_RET, COND_WS, COND_P, EOF);
    }

    @Test
    public void parseJp() {
        assertTokenTypesIgnoreCase("jp c c", OPCODE_JP, COND_WS, COND_C, WS, REG_C, EOF);
        assertTokenTypesIgnoreCase("jp hl", OPCODE_JP, COND_WS, REG_HL, EOF);
    }

    @Test
    public void parseCall() {
        assertTokenTypesIgnoreCase("call pe!", OPCODE_CALL, COND_WS, COND_PE, ERROR, EOF);
        assertTokenTypesIgnoreCase("call pet", OPCODE_CALL, COND_WS, ID_IDENTIFIER, EOF);
        assertTokenTypesIgnoreCase("call pe?", OPCODE_CALL, COND_WS, ID_IDENTIFIER, EOF);
        assertTokenTypesIgnoreCase("call pe>", OPCODE_CALL, COND_WS, COND_PE, OP_GT, EOF);
        assertTokenTypesIgnoreCase("call pe#", OPCODE_CALL, COND_WS, COND_PE, COMMENT, EOF);
        assertTokenTypesIgnoreCase("call pe\"", OPCODE_CALL, COND_WS, COND_PE, ERROR, EOF);
        assertTokenTypesIgnoreCase("call pe'", OPCODE_CALL, COND_WS, COND_PE, ERROR, EOF);
        assertTokenTypesIgnoreCase("call pe(", OPCODE_CALL, COND_WS, COND_PE, SEP_LPAR, EOF);
        assertTokenTypesIgnoreCase("call pe)", OPCODE_CALL, COND_WS, COND_PE, SEP_RPAR, EOF);
        assertTokenTypesIgnoreCase("call pe,", OPCODE_CALL, COND_WS, COND_PE, SEP_COMMA, EOF);
        assertTokenTypesIgnoreCase("call pe.", OPCODE_CALL, COND_WS, COND_PE, ERROR, EOF);
        assertTokenTypesIgnoreCase("call pe=", OPCODE_CALL, COND_WS, COND_PE, OP_EQUAL, EOF);
        assertTokenTypesIgnoreCase("call pe<", OPCODE_CALL, COND_WS, COND_PE, OP_LT, EOF);
        assertTokenTypesIgnoreCase("call pe&", OPCODE_CALL, COND_WS, COND_PE, OP_AND, EOF);
        assertTokenTypesIgnoreCase("call pe+", OPCODE_CALL, COND_WS, COND_PE, OP_ADD, EOF);
        assertTokenTypesIgnoreCase("call pe-", OPCODE_CALL, COND_WS, COND_PE, OP_SUBTRACT, EOF);
        assertTokenTypesIgnoreCase("call pe/", OPCODE_CALL, COND_WS, COND_PE, OP_DIVIDE, EOF);
        assertTokenTypesIgnoreCase("call pe;", OPCODE_CALL, COND_WS, COND_PE, COMMENT, EOF);
        assertTokenTypesIgnoreCase("call pe/**/", OPCODE_CALL, COND_WS, COND_PE, COMMENT2, EOF);
        assertTokenTypesIgnoreCase("call pe", OPCODE_CALL, COND_WS, COND_PE, EOF);
        assertTokenTypesIgnoreCase("call pe\n", OPCODE_CALL, COND_WS, COND_PE, EOL, EOF);
        assertTokenTypesIgnoreCase("call pe\f", OPCODE_CALL, COND_WS, COND_PE, WS, EOF);
        assertTokenTypesIgnoreCase("call pe\r", OPCODE_CALL, COND_WS, COND_PE, EOF);
        assertTokenTypesIgnoreCase("call pe0", OPCODE_CALL, COND_WS, ID_IDENTIFIER, EOF);
    }

    @Test
    public void testParsePreprocessor() {
        assertTokenTypesIgnoreCase("ORG", PREP_ORG, EOF);
        assertTokenTypesIgnoreCase("EQU", PREP_EQU, EOF);
        assertTokenTypesIgnoreCase("VAR", PREP_VAR, EOF);
        assertTokenTypesIgnoreCase("INCLUDE", PREP_INCLUDE, EOF);
        assertTokenTypesIgnoreCase("IF", PREP_IF, EOF);
        assertTokenTypesIgnoreCase("ENDIF", PREP_ENDIF, EOF);
        assertTokenTypesIgnoreCase("MACRO", PREP_MACRO, EOF);
        assertTokenTypesIgnoreCase("ENDM", PREP_ENDM, EOF);
        assertTokenTypesIgnoreCase("DB", PREP_DB, EOF);
        assertTokenTypesIgnoreCase("DW", PREP_DW, EOF);
        assertTokenTypesIgnoreCase("DS", PREP_DS, EOF);
        assertTokenTypesIgnoreCase("$", PREP_ADDR, EOF);
    }

    @Test
    public void testRegisters() {
        assertTokenTypesIgnoreCase("A", REG_A, EOF);
        assertTokenTypesIgnoreCase("B", REG_B, EOF);
        assertTokenTypesIgnoreCase("C", REG_C, EOF);
        assertTokenTypesIgnoreCase("D", REG_D, EOF);
        assertTokenTypesIgnoreCase("E", REG_E, EOF);
        assertTokenTypesIgnoreCase("H", REG_H, EOF);
        assertTokenTypesIgnoreCase("L", REG_L, EOF);
        assertTokenTypesIgnoreCase("AF", REG_AF, EOF);
        assertTokenTypesIgnoreCase("AF'", REG_AFF, EOF);
        assertTokenTypesIgnoreCase("SP", REG_SP, EOF);
        assertTokenTypesIgnoreCase("IX", REG_IX, EOF);
        assertTokenTypesIgnoreCase("IY", REG_IY, EOF);
        assertTokenTypesIgnoreCase("IXH", REG_IXH, EOF);
        assertTokenTypesIgnoreCase("IXL", REG_IXL, EOF);
        assertTokenTypesIgnoreCase("IYH", REG_IYH, EOF);
        assertTokenTypesIgnoreCase("IYL", REG_IYL, EOF);
        assertTokenTypesIgnoreCase("R", REG_R, EOF);
        assertTokenTypesIgnoreCase("I", REG_I, EOF);
    }

    @Test
    public void testSeparators() {
        assertTokenTypes("(),", SEP_LPAR, SEP_RPAR, SEP_COMMA, EOF);
    }

    @Test
    public void testOperators1() {
        assertTokenTypes("+-*/=<<>><><=>=^%|&",
                OP_ADD, OP_SUBTRACT, OP_MULTIPLY, OP_DIVIDE, OP_EQUAL, OP_SHL_2, OP_SHR_2, OP_LT, OP_GT, OP_LTE, OP_GTE,
                OP_XOR, OP_MOD_2, OP_OR, OP_AND, EOF);
    }

    @Test
    public void testOperators2() {
        assertTokenTypesIgnoreCase("MOD", OP_MOD, EOF);
        assertTokenTypesIgnoreCase("SHR", OP_SHR, EOF);
        assertTokenTypesIgnoreCase("SHL", OP_SHL, EOF);
        assertTokenTypesIgnoreCase("NOT", OP_NOT, EOF);
    }

    @Test
    public void testIdentifier() {
        assertTokenTypes("u @ ? _", ID_IDENTIFIER, WS, ID_IDENTIFIER, WS, ID_IDENTIFIER, WS, ID_IDENTIFIER, EOF);
        assertTokenTypes("a@ abc ZZ_ H005", ID_IDENTIFIER, WS, ID_IDENTIFIER, WS, ID_IDENTIFIER, WS, ID_IDENTIFIER, EOF);
    }

    @Test
    public void testLabel() {
        assertTokenTypes("u: @: ?: _:", ID_LABEL, WS, ID_LABEL, WS, ID_LABEL, WS, ID_LABEL, EOF);
        assertTokenTypes("a@: abc: ZZ_: H005:", ID_LABEL, WS, ID_LABEL, WS, ID_LABEL, WS, ID_LABEL, EOF);
        assertTokenTypes("a:", ID_LABEL, EOF);
    }

    @Test
    public void testCombineSpaceWithNoSpaceTokens() {
        assertTokenTypes("abc=(^,MOD)", ID_IDENTIFIER, OP_EQUAL, SEP_LPAR, OP_XOR, SEP_COMMA, OP_MOD, SEP_RPAR, EOF);
    }
}
